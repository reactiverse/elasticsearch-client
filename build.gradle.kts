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
  `maven-publish`
  signing
}

allprojects {

  apply(plugin = "java")
  apply(plugin = "maven-publish")
  apply(plugin = "signing")

  version = "0.0-SNAPSHOT"
  group = "foo.bar"

  extra["vertxVersion"] = "3.6.2"
  extra["elasticClientVersion"] = "6.5.2"
  extra["isReleaseVersion"] = !version.toString().endsWith("SNAPSHOT")

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

  create<Jar>("sourcesJar") {
    from(sourceSets.main.get().allJava)
    classifier = "sources"
  }

  create<Jar>("javadocJar") {
    from(javadoc)
    classifier = "javadoc"
  }

  javadoc {
    if (JavaVersion.current().isJava9Compatible) {
      (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
  }

  withType<Sign> {
    onlyIf { project.extra["isReleaseVersion"] as Boolean }
  }
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])
      artifact(tasks["sourcesJar"])
      artifact(tasks["javadocJar"])
      pom {
        name.set(project.name)
        description.set("Vert.x Elasticsearch client")
        url.set("https://github.com/jponge/vertx-elasticsearch-client")
        licenses {
          license {
            name.set("The Apache License, Version 2.0")
            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
          }
        }
        developers {
          developer {
            id.set("jponge")
            name.set("Julien Ponge")
            email.set("julien.ponge@gmail.com")
          }
        }
        scm {
          connection.set("scm:git:git@github.com:jponge/vertx-elasticsearch-client.git")
          developerConnection.set("scm:git:git@github.com:jponge/vertx-elasticsearch-client.git")
          url.set("https://github.com/jponge/vertx-elasticsearch-client")
        }
      }
    }
  }
  repositories {
    // To locally check out the poms
    maven {
      val releasesRepoUrl = uri("$buildDir/repos/releases")
      val snapshotsRepoUrl = uri("$buildDir/repos/snapshots")
      name = "buildDir"
      url = if (project.extra["isReleaseVersion"] as Boolean) snapshotsRepoUrl else releasesRepoUrl
    }
  }
}

signing {
  sign(publishing.publications["mavenJava"])
}
