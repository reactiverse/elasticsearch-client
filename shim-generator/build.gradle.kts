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

val elastic by configurations.creating

val elasticSourcesDir = "$buildDir/elastic-sources/"
val elasticShimsDir = "${buildDir}/elastic-shims"

val elasticClientVersion = extra["elasticClientVersion"]

dependencies {
  implementation("ch.qos.logback:logback-classic:1.2.3")
  implementation("com.github.javaparser:javaparser-core:3.7.0")
  elastic("org.elasticsearch.client:elasticsearch-rest-high-level-client:${elasticClientVersion}:sources")
}

tasks {

  create<Copy>("elastic-unpack") {
    val sources = elastic.resolve().filter { it.name.endsWith("-sources.jar") }
    sources.forEach { from(zipTree(it)) }
    into(elasticSourcesDir)
  }

  create<JavaExec>("elastic-process") {
    main = "shimgen.Analyze"
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf(
      "$elasticSourcesDir/org/elasticsearch/client/RestHighLevelClient.java",
      elasticShimsDir,
      "io.vertx.elasticsearch.client"
    )
    description = "Generate the shims from the ElasticSearch source code"
    group = "build"
    dependsOn("elastic-unpack")
    inputs.dir(elasticSourcesDir)
    outputs.dir(elasticShimsDir)
  }
}

