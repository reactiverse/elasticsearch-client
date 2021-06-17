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
  java
}

val vertxVersion = extra["vertxVersion"]
val mutinyBindingsVersion = extra["mutinyBindingsVersion"]
val assertjVersion = extra["assertjVersion"]
val tcVersion = extra["tcVersion"]
val junitVersion = extra["junitVersion"]

dependencies {
  testImplementation(project(":elasticsearch-client-rxjava2"))
  testImplementation(project(":elasticsearch-client-mutiny"))

  testImplementation("io.vertx:vertx-junit5:${vertxVersion}")
  testImplementation("io.vertx:vertx-junit5-rx-java2:${vertxVersion}")
  testImplementation("io.smallrye.reactive:smallrye-mutiny-vertx-junit5:${mutinyBindingsVersion}")

  testImplementation("org.assertj:assertj-core:${assertjVersion}")
  testImplementation("org.testcontainers:elasticsearch:${tcVersion}")
  testImplementation("org.testcontainers:junit-jupiter:${tcVersion}")

  testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
}

tasks.test {
  useJUnitPlatform()
}
