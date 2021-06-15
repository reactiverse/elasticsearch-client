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

package integration;

import io.reactiverse.elasticsearch.client.reactivex.RestHighLevelClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import org.apache.http.HttpHost;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ExtendWith(VertxExtension.class)
public class RxJava2Tests {

  @Container
  private ElasticsearchContainer container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch-oss:7.10.1");

  private RestHighLevelClient client;

  @BeforeEach
  void prepare(Vertx vertx) {
    RestClientBuilder builder = RestClient.builder(
      new HttpHost(container.getContainerIpAddress(), container.getMappedPort(9200), "http"));
    client = RestHighLevelClient.create(vertx, builder);
  }

  @AfterEach
  void close() {
    client.close();
  }

  @Test
  void indexThenGet(Vertx vertx, VertxTestContext testContext) {
    String yo = "{\"foo\": \"bar\"}";
    IndexRequest req = new IndexRequest("posts", "_doc", "1").source(yo, XContentType.JSON);
    client
      .rxIndexAsync(req, RequestOptions.DEFAULT)
      .flatMap(resp -> client.rxGetAsync(new GetRequest("posts", "_all", "1"), RequestOptions.DEFAULT))
      .subscribe(resp -> testContext.verify(() -> {
        assertThat(Thread.currentThread().getName()).startsWith("vert.x-eventloop-thread-");
        assertThat(vertx.getOrCreateContext().isEventLoopContext()).isTrue();
        assertThat(resp.getType()).isEqualTo("_doc");
        assertThat(resp.getSourceAsMap()).hasEntrySatisfying("foo", "bar"::equals);
        testContext.completeNow();
      }), testContext::failNow);
  }
}
