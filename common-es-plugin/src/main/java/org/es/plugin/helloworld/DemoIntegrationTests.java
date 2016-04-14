///*
// * Licensed to David Pilato under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership. David Pilato licenses this
// * file to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// *
// *    http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing,
// * software distributed under the License is distributed on an
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// * KIND, either express or implied.  See the License for the
// * specific language governing permissions and limitations
// * under the License.
// */
//
//package org.es.plugin.helloworld;
//
//import org.elasticsearch.ElasticsearchException;
//import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
//import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
//import org.elasticsearch.action.count.CountResponse;
//import org.elasticsearch.common.logging.ESLogger;
//import org.elasticsearch.common.logging.Loggers;
//import org.elasticsearch.common.network.NetworkUtils;
//import org.elasticsearch.node.Node;
//import org.junit.After;
//import org.junit.AfterClass;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//import static org.elasticsearch.client.Requests.*;
//import static org.elasticsearch.common.io.Streams.copyToStringFromClasspath;
//import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
//import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
//import static org.elasticsearch.index.query.QueryBuilders.termQuery;
//import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.Matchers.equalTo;
//
//import org.apache.lucene.queryparser.xml.builders.TermQueryBuilder;
//
///**
// *
// */
//public class DemoIntegrationTests {
//
//    private final ESLogger logger = Loggers.getLogger(getClass());
//
//    private Node node;
//
//    @BeforeClass
//    public void setupServer() {
//        node = nodeBuilder()
//                //.local(true)
//                .settings(settingsBuilder()
//                        .put("path.data", "target/data")
//                        .put("cluster.name", "itest-cluster-" + NetworkUtils.getLocalAddress())
//                        //.put("gateway.type", "none")
//                ).node();
//    }
//
//    @AfterClass
//    public void closeServer() {
//        node.close();
//    }
//
//    @Before
//    public void createIndex() {
//        logger.info("deleting index [itest]");
//        try {
//            node.client().admin().indices().delete(deleteIndexRequest("itest")).actionGet();
//        } catch (ElasticsearchException e) {
//            logger.info("index [itest] does not exist... No pb!");
//        }
//        logger.info("creating index [itest]");
//        node.client().admin().indices().create(createIndexRequest("itest").settings(settingsBuilder().put("index.numberOfReplicas", 0))).actionGet();
//        logger.info("Running Cluster Health");
//        ClusterHealthResponse clusterHealth = node.client().admin().cluster().health(clusterHealthRequest().waitForGreenStatus()).actionGet();
//        logger.info("Done Cluster Health, status " + clusterHealth.getStatus().toString());
//    }
//
//    @After
//    public void deleteIndex() {
//        logger.info("deleting index [itest]");
//        node.client().admin().indices().delete(deleteIndexRequest("itest")).actionGet();
//    }
//
//    @Test
//    public void testSimpleAttachment() throws Exception {
//        String mapping = copyToStringFromClasspath("itest-mapping.json");
//        String text = "Hello World";
//
//        node.client().admin().indices().putMapping(putMappingRequest("itest").type("person").source(mapping)).actionGet();
//
//        node.client().index(indexRequest("itest").type("person")
//                .source(jsonBuilder().startObject().field("my_demo", text).endObject())).actionGet();
//        node.client().admin().indices().refresh(refreshRequest()).actionGet();
//
//    }
//}