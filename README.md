# An Elasticsearch client for Eclipse Vert.x

This client exposes the [Elasticsearch Java High Level REST Client](https://www.elastic.co/guide/en/elasticsearch/client/java-rest/master/java-rest-high.html) for [Eclipse Vert.x](https://vertx.io/) applications.

## Overview

This client is based on automatically generated shims using a source-to-source transformation from the client source code.

The generated shims ensure that asynchronous event processing respect the Vert.x threading model.

## Usage

The following modules can be used:

* `elasticsearch-client`: a classic Vert.x API based on callbacks
* `elasticsearch-client-rxjava2`: a RxJava 2 API of the client

The Maven `groupId` is `io.reactiverse`. 

## Sample usage

Here is a sample usage of the RxJava 2 API where an index request is followed by a get request:

```java
String yo = "{\"foo\": \"bar\"}";
IndexRequest req = new IndexRequest("posts", "_doc", "1").source(yo, XContentType.JSON);
client
  .rxIndexAsync(req, RequestOptions.DEFAULT)
  .flatMap(resp -> client.rxGetAsync(new GetRequest("posts", "_all", "1"), RequestOptions.DEFAULT))
  .subscribe(resp -> {
    // Handle the response here    
  });
```

## Legal

Originally developped by [Julien Ponge](https://julien.ponge.org/).

    Copyright 2018 Red Hat, Inc.
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
