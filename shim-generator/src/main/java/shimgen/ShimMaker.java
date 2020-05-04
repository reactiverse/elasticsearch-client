/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package shimgen;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.stream.Collectors;

class ShimMaker extends VoidVisitorAdapter<Void> {

  private final Logger logger = LoggerFactory.getLogger(ShimMaker.class);

  private final CompilationUnit shimInterfaceUnit;
  private final ClassOrInterfaceDeclaration shimInterface;
  private final CompilationUnit shimImplementationUnit;
  private final ClassOrInterfaceDeclaration shimImplementation;

  ShimMaker(CompilationUnit shimInterfaceUnit, ClassOrInterfaceDeclaration shimInterface, CompilationUnit shimImplementationUnit, ClassOrInterfaceDeclaration shimImplementation) {
    this.shimInterfaceUnit = shimInterfaceUnit;
    this.shimInterface = shimInterface;
    this.shimImplementationUnit = shimImplementationUnit;
    this.shimImplementation = shimImplementation;
  }

  @Override
  public void visit(ImportDeclaration n, Void arg) {
    shimInterfaceUnit.addImport(n);
    shimImplementationUnit.addImport(n);
    super.visit(n, arg);
  }

  @Override
  public void visit(ClassOrInterfaceDeclaration n, Void arg) {
    if (n.getNameAsString().equals("RestHighLevelClient")) {
      super.visit(n, arg);
      return;
    }
    logger.info("Client class {}, generating a constructor", n.getNameAsString());

    String delegateType = "org.elasticsearch.client." + n.getNameAsString();

    shimImplementation
      .addField("Vertx", "vertx")
      .setPrivate(true)
      .setFinal(true);

    shimImplementation
      .addField(delegateType, "delegate")
      .setPrivate(true)
      .setFinal(true);

    String code = "{\n" +
      "  this.vertx = vertx;\n" +
      "  this.delegate = delegate;" +
      "}\n";

    shimImplementation
      .addConstructor()
      .addParameter("Vertx", "vertx")
      .addParameter(delegateType, "delegate")
      .setBody(JavaParser.parseBlock(code));

    super.visit(n, arg);
  }

  @Override
  public void visit(ConstructorDeclaration n, Void arg) {
    if (n.isPublic()) {
      NodeList<Parameter> parameters = NodeList.nodeList(n.getParameters());
      parameters.add(0, new Parameter(new TypeParameter("Vertx"), "vertx"));
      logger.info("Interface factory method with parameters {}", parameters);

      BlockStmt createBlock = new BlockStmt();
      shimInterface
        .addMethod("create", Modifier.Keyword.STATIC)
        .addSingleMemberAnnotation("GenIgnore", "GenIgnore.PERMITTED_TYPE")
        .setType(shimInterface.getNameAsString())
        .setParameters(parameters)
        .setBody(createBlock);
      String args = parameters
        .stream()
        .map(Parameter::getNameAsString)
        .collect(Collectors.joining(", "));
      String implCreation = "return new " + shimImplementation.getNameAsString() + "(" + args + ");";
      createBlock.addStatement(JavaParser.parseStatement(implCreation));

      BlockStmt constructorBlock = new BlockStmt();
      ConstructorDeclaration constructor = shimImplementation
        .addConstructor()
        .setParameters(parameters)
        .setBody(constructorBlock);

      parameters.forEach(p -> {
        shimImplementation
          .addField(p.getType(), p.getNameAsString())
          .setPrivate(true)
          .setFinal(true);
        constructorBlock
          .addStatement(JavaParser.parseStatement("this." + p.getNameAsString() + " = " + p.getNameAsString() + ";"));
      });

      String constructorArgs = parameters
        .stream()
        .map(Parameter::getNameAsString)
        .filter(p -> !"vertx".equals(p))
        .collect(Collectors.joining(", "));
      constructorBlock
        .addStatement(JavaParser.parseStatement("this.delegate = new org.elasticsearch.client." + shimInterface.getNameAsString() + "(" + constructorArgs + ");"));

      shimImplementation
        .addField("org.elasticsearch.client." + shimInterface.getNameAsString(), "delegate")
        .setPrivate(true)
        .setFinal(true);
    }
    super.visit(n, arg);
  }

  @Override
  public void visit(MethodDeclaration n, Void arg) {
    if (isAsyncMethod(n)) {
      generateAsyncMethod(n);
    } else if (isNestedClientMethod(n)) {
      generateNestedClientMethod(n);
    } else if (isPassThroughMethod(n)) {
      generatePassThrough(n);
    }
    super.visit(n, arg);
  }

  private static final String ASYNC_DISPATCH_TEMPLATE = "{\n" +
    "      Context context = vertx.getOrCreateContext();\n" +
    "      delegate.{{METHOD}}({{ARGS}} new ActionListener<{{TYPE}}>() {\n" +
    "        @Override\n" +
    "        public void onResponse({{TYPE}} value) {\n" +
    "          context.runOnContext(v -> handler.handle(Future.succeededFuture(value)));\n" +
    "        }\n" +
    "\n" +
    "        @Override\n" +
    "        public void onFailure(Exception e) {\n" +
    "          context.runOnContext(v -> handler.handle(Future.failedFuture(e)));\n" +
    "        }\n" +
    "      });\n" +
    "    }";

  private void generateAsyncMethod(MethodDeclaration n) {
    logger.info("Async method shim for {}", n.getNameAsString());
    NodeList<Parameter> parameters = NodeList.nodeList(n.getParameters());
    Parameter lastParameter = parameters.get(parameters.size() - 1);
    Optional<NodeList<Type>> typeArguments = lastParameter.getType().asClassOrInterfaceType().getTypeArguments();
    if (!typeArguments.isPresent() || typeArguments.get().size() != 1) {
      logger.warn("Not processing {} as the last argument is of actual type {}", n.getNameAsString(), lastParameter.getType());
      return;
    }
    Type parametricType = typeArguments.get().get(0);
    lastParameter.setType("Handler<AsyncResult<" + parametricType + ">>");
    lastParameter.setName("handler");

    shimInterface
      .addMethod(n.getNameAsString())
      .setPublic(true)
      .setType("void")
      .setParameters(parameters)
      .removeBody()
      .addSingleMemberAnnotation("GenIgnore", "GenIgnore.PERMITTED_TYPE");

    NodeList<Parameter> callParameters = NodeList.nodeList(parameters);
    callParameters.removeLast();
    String callArgs = callParameters
      .stream()
      .map(NodeWithSimpleName::getNameAsString)
      .collect(Collectors.joining(", "));
    if (!callArgs.isEmpty()) {
      callArgs = callArgs + ", ";
    }

    String shimCode = ASYNC_DISPATCH_TEMPLATE
      .replace("{{METHOD}}", n.getNameAsString())
      .replace("{{ARGS}}", callArgs)
      .replace("{{TYPE}}", parametricType.toString());

    logger.debug("Code -> \n{}", shimCode);

    shimImplementation
      .addMethod(n.getNameAsString())
      .setPublic(true)
      .addAnnotation("Override")
      .setType("void")
      .setParameters(parameters)
      .setBody(JavaParser.parseBlock(shimCode));
  }

  private void generateNestedClientMethod(MethodDeclaration n) {
    String methodName = n.getNameAsString();
    String methodType = n.getTypeAsString();
    if (Analyze.FILTERED_TYPES.contains(methodType)) {
      logger.debug("Skipping {} as the return type is being filtered", methodName);
      return;
    }
    logger.info("Nested client method shim for {}", methodName);

    shimInterface
      .addMethod(methodName)
      .setType(n.getType())
      .setParameters(n.getParameters())
      .removeBody()
      .addSingleMemberAnnotation("GenIgnore", "GenIgnore.PERMITTED_TYPE");

    String callArgs = n.getParameters()
      .stream()
      .map(NodeWithSimpleName::getNameAsString)
      .collect(Collectors.joining(", "));

    String shimCode = "{ return new " + methodType + "Impl(vertx, delegate." + methodName + "(" + callArgs + ")); }";
    MethodDeclaration impl = shimImplementation
      .addMethod(methodName)
      .setPublic(true)
      .addAnnotation("Override")
      .setType(n.getType())
      .setParameters(n.getParameters())
      .setBody(JavaParser.parseBlock(shimCode));
  }

  private void generatePassThrough(MethodDeclaration n) {
    logger.info("Passthrough method {}", n.getNameAsString());
    MethodDeclaration method = shimInterface
      .addMethod(n.getNameAsString())
      .setType(n.getType())
      .setParameters(n.getParameters())
      .removeBody()
      .addSingleMemberAnnotation("GenIgnore", "GenIgnore.PERMITTED_TYPE");

    String callArgs = n.getParameters()
      .stream()
      .map(NodeWithSimpleName::getNameAsString)
      .collect(Collectors.joining(", "));

    String shimCode;
    if ("void".equals(n.getTypeAsString())) {
      shimCode = "delegate." + n.getName() + "(" + callArgs + ");";
    } else {
      shimCode = "return delegate." + n.getName() + "(" + callArgs + ");";
    }
    shimCode = "{try {\n" +
      shimCode + "\n" +
      "} catch (Throwable t) {\n" +
      "  throw new RuntimeException(t);\n" +
      "}}";

    MethodDeclaration impl = shimImplementation
      .addMethod(n.getNameAsString())
      .setPublic(true)
      .addAnnotation("Override")
      .setType(n.getType())
      .setParameters(n.getParameters())
      .setBody(JavaParser.parseBlock(shimCode));
  }

  private boolean isAsyncMethod(MethodDeclaration n) {
    return n.isPublic() && n.getNameAsString().endsWith("Async") && !n.isAnnotationPresent("Deprecated");
  }

  private boolean isNestedClientMethod(MethodDeclaration n) {
    return n.getTypeAsString().endsWith("Client");
  }

  private boolean isPassThroughMethod(MethodDeclaration n) {
    return n.isPublic() && !n.isAnnotationPresent("Deprecated") && n.getNameAsString().equals("close");
  }
}
