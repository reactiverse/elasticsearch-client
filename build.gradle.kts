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

allprojects {

  apply(plugin = "java")

  version = "0.0-SNAPSHOT"
  group = "foo.bar"

  extra["vertxVersion"] = "3.6.2"
  extra["elasticClientVersion"] = "6.5.2"

  repositories {
    mavenCentral()
    maven {
      url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
  }

  java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
}

val vertxVersion = extra["vertxVersion"]
val elasticClientVersion = extra["elasticClientVersion"]

dependencies {
  api("io.vertx:vertx-core:${vertxVersion}")
  api("org.elasticsearch.client:elasticsearch-rest-high-level-client:${elasticClientVersion}")
  compileOnly("io.vertx:vertx-codegen:${vertxVersion}")
}

sourceSets {
  main {
    java {
      setSrcDirs(listOf("src/main/java", "src/main/generated"))
    }
  }
}

tasks {

  create<Copy>("copy-shims") {
    from(getByPath(":shim-generator:elastic-process"))
    into("src/main/generated")
  }

  getByName("compileJava").dependsOn("copy-shims")

  getByName<Delete>("clean") {
    delete.add("src/main/generated")
  }
}
