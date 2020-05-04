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

dependencies {
  testImplementation(project(":elasticsearch-client-rxjava2"))

  testImplementation("io.vertx:vertx-junit5:${vertxVersion}")

  testImplementation("org.assertj:assertj-core:3.14.0")
  testImplementation("org.testcontainers:elasticsearch:1.14.1")
  testImplementation("org.testcontainers:junit-jupiter:1.14.1")

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")
}

tasks.test {
  useJUnitPlatform()
}
