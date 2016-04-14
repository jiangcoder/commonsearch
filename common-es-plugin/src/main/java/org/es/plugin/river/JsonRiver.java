//package org.es.plugin.river;
//
//import static org.elasticsearch.client.Requests.*;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.LinkedTransferQueue;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.TransferQueue;
//
//import org.elasticsearch.ElasticsearchException;
//import org.elasticsearch.action.WriteConsistencyLevel;
//import org.elasticsearch.action.bulk.BulkRequestBuilder;
//import org.elasticsearch.action.bulk.BulkResponse;
//import org.elasticsearch.action.get.GetResponse;
//import org.elasticsearch.action.index.IndexRequest;
//import org.elasticsearch.action.index.IndexResponse;
//import org.elasticsearch.action.support.replication.ReplicationType;
//import org.elasticsearch.client.Client;
//import org.elasticsearch.common.StopWatch;
//import org.elasticsearch.common.inject.Inject;
//import org.elasticsearch.common.logging.ESLogger;
//import org.elasticsearch.common.logging.ESLoggerFactory;
//import org.elasticsearch.common.unit.TimeValue;
//import org.elasticsearch.common.util.concurrent.EsExecutors;
//import org.elasticsearch.river.*;
//
//import com.jiangcoder.search.es.ESClientUtils;
//
//public class JsonRiver extends AbstractRiverComponent implements River {
//	private BulkRequestBuilder bulkRequest = null;
//    private Client client=ESClientUtils.getTransportClient();
//    private final String riverIndexName;
//
//    public static String RIVER_URL = "http://search.atguat.com.cn/search?question=iphone&pageType=json";
//    public static String RIVER_INDEX = "products";
//    public static String RIVER_TYPE = "product";
//    public static TimeValue RIVER_REFRESH_INTERVAL = TimeValue.timeValueSeconds(30);
//    public static int RIVER_MAX_BULK_SIZE = 5000;
//
//    private volatile Thread slurperThread;
//    private volatile Thread indexerThread;
//    private volatile boolean closed;
//
//    private final TransferQueue<RiverProduct> stream = new LinkedTransferQueue<RiverProduct>();
//
//    @Inject public JsonRiver(RiverName riverName, RiverSettings settings, @RiverIndexName String riverIndexName, Client client) {
//        super(riverName, settings);
//        this.riverIndexName = riverIndexName;
//        this.client = client;
//    }
//
//    @Override
//    public void start() {
//        logger.info("Starting JSON stream river: url [{}], query interval [{}]", RIVER_URL, RIVER_REFRESH_INTERVAL);
//
//        closed = false;
//        try {
//            slurperThread = EsExecutors.daemonThreadFactory("json_river_slurper").newThread(new Slurper());
//            slurperThread.start();
//            indexerThread = EsExecutors.daemonThreadFactory("json_river_indexer").newThread(new Indexer());
//            indexerThread.start();
//        } catch (ElasticsearchException e) {
//            logger.error("Error starting indexer and slurper. River is not running", e);
//            closed = true;
//        }
//    }
//
//    @Override
//    public void close() {
//        if (closed) {
//            return;
//        }
//        logger.info("closing json stream river");
//        slurperThread.interrupt();
//        indexerThread.interrupt();
//        closed = true;
//    }
//
//    private class Slurper implements Runnable {
//
//        private final ESLogger logger = ESLoggerFactory.getLogger(this.getClass().getName());
//
//        @Override
//        public void run() {
//            RiverImporter importer = new RiverImporter(RIVER_URL, stream);
//
//            while (!closed) {
//                logger.debug("Slurper run() started");
//                String lastIndexUpdate = getLastUpdatedTimestamp();
//
//                try {
//                    RiverProductImport result = importer.executeImport(lastIndexUpdate);
//
//                    if (result.exportTimestamp != null) {
//                        storeLastUpdatedTimestamp(result.exportTimestamp);
//                    }
//
//                    logger.info("Slurping [{}] documents with timestamp [{}]", result.exportedProductCount, result.exportTimestamp);
//                } catch (ElasticsearchException e) {
//                    logger.error("Failed to import data from json stream", e);
//                }
//
//                try {
//                    Thread.sleep(RIVER_REFRESH_INTERVAL.getMillis());
//                } catch (InterruptedException e1) {}
//            }
//        }
//
//        private void storeLastUpdatedTimestamp(String exportTimestamp) {
//            String json = "{ \"lastUpdatedTimestamp\" : \"" + exportTimestamp + "\" }";
//            IndexRequest updateTimestampRequest = indexRequest(riverIndexName).type(riverName.name()).id("lastUpdatedTimestamp").source(json);
//            client.index(updateTimestampRequest).actionGet();
//        }
//
//        private String getLastUpdatedTimestamp() {
//            GetResponse lastUpdatedTimestampResponse = client.prepareGet().setIndex(riverIndexName).setType(riverName.name()).setId("lastUpdatedTimestamp").execute().actionGet();
//            if (lastUpdatedTimestampResponse.isExists() && lastUpdatedTimestampResponse.getSource().containsKey("lastUpdatedTimestamp")) {
//                return lastUpdatedTimestampResponse.getSource().get("lastUpdatedTimestamp").toString();
//            }
//
//            return null;
//        }
//    }
//
//
//    private class Indexer implements Runnable {
//        private final ESLogger logger = ESLoggerFactory.getLogger(this.getClass().getName());
//        private int deletedDocuments = 0;
//        private int insertedDocuments = 0;
//        private BulkRequestBuilder bulk;
//        private StopWatch sw;
//
//        @Override
//        public void run() {
//            while (!closed) {
//                logger.debug("Indexer run() started");
//                sw = new StopWatch().start();
//                deletedDocuments = 0;
//                insertedDocuments = 0;
//                    	IndexResponse response =client.prepareIndex("library","book","5").setSource("{\"title\":\"b\"}").execute().actionGet();
//        }
//            }
//        private void addProductToBulkRequest(RiverProduct riverProduct) {
//            if (riverProduct.action == RiverProduct.Action.DELETE) {
//                //bulk.add(deleteRequest(RIVER_INDEX).type(RIVER_TYPE).id(riverProduct.id));
//                logger.error("DELETING {}/{}/{}", RIVER_INDEX, RIVER_TYPE, riverProduct.id);
//                client.prepareDelete(RIVER_INDEX, RIVER_TYPE, riverProduct.id).execute().actionGet();
//                deletedDocuments++;
//            } else {
//                logger.error("INDEXING {}/{}/{}", RIVER_INDEX, RIVER_TYPE, riverProduct.id);
//                bulk.add(indexRequest(RIVER_INDEX).type(RIVER_TYPE).id(riverProduct.id).source(riverProduct.product));
//                insertedDocuments++;
//            }
//        }
//
//    }
//}