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

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.GenericListVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

class NestedClientFinder extends GenericListVisitorAdapter<String, Void> {

  private final Logger logger = LoggerFactory.getLogger(NestedClientFinder.class);

  private ClassOrInterfaceDeclaration currentClass;

  @Override
  public List<String> visit(ClassOrInterfaceDeclaration n, Void arg) {
    if (n.isPublic()) {
      currentClass = n;
      logger.info("Visiting class {}", n.getName());
    }
    return super.visit(n, arg);
  }

  @Override
  public List<String> visit(MethodDeclaration n, Void arg) {
    if (n.isPublic()) {
      logger.info("Looking at {} in {}", n.getName(), currentClass.getName());
      String type = n.getTypeAsString();
      if (type.endsWith("Client") && !Analyze.FILTERED_TYPES.contains(type)) {
        return Collections.singletonList(type);
      }
    }
    return super.visit(n, arg);
  }
}
