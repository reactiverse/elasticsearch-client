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

plugins {
  `java-library`
}

val vertxVersion = extra["vertxVersion"]

dependencies {
  api(rootProject)
  api("io.vertx:vertx-rx-java2:${vertxVersion}")
  compileOnly("io.vertx:vertx-codegen:${vertxVersion}")
  annotationProcessor("io.vertx:vertx-rx-java2:${vertxVersion}")
  annotationProcessor("io.vertx:vertx-rx-java2-gen:${vertxVersion}")
  annotationProcessor("io.vertx:vertx-codegen:${vertxVersion}:processor")
}

sourceSets {
  main {
    java {
      setSrcDirs(listOf("../src/main/generated", "src/main/generated"))
    }
  }
}

tasks {

  getByName<JavaCompile>("compileJava") {
    options.annotationProcessorGeneratedSourcesDirectory = File("$projectDir/src/main/generated")
  }

  getByName<Delete>("clean") {
    delete.add("src/main/generated")
  }
}
