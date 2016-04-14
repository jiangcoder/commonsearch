///*
// * Licensed to Elasticsearch under one or more contributor
// * license agreements. See the NOTICE file distributed with
// * this work for additional information regarding copyright
// * ownership. Elasticsearch licenses this file to you under
// * the Apache License, Version 2.0 (the "License"); you may
// * not use this file except in compliance with the License.
// * You may obtain a copy of the License at
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
package org.es.plugin.searchX;

import static org.elasticsearch.action.search.SearchType.COUNT;
import static org.elasticsearch.action.search.SearchType.DFS_QUERY_THEN_FETCH;
import static org.elasticsearch.action.search.SearchType.QUERY_AND_FETCH;
import static org.elasticsearch.action.search.SearchType.SCAN;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.search.type.TransportSearchCountAction;
import org.elasticsearch.action.search.type.TransportSearchDfsQueryAndFetchAction;
import org.elasticsearch.action.search.type.TransportSearchDfsQueryThenFetchAction;
import org.elasticsearch.action.search.type.TransportSearchQueryAndFetchAction;
import org.elasticsearch.action.search.type.TransportSearchQueryThenFetchAction;
import org.elasticsearch.action.search.type.TransportSearchScanAction;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.TransportAction;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.internal.InternalSearchHitField;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.BaseTransportRequestHandler;
import org.elasticsearch.transport.TransportChannel;
import org.elasticsearch.transport.TransportService;

public class TransportSearchActionX extends TransportAction<SearchRequestX, SearchResponse>{
    private final ClusterService clusterService;
    private final TransportSearchDfsQueryThenFetchAction dfsQueryThenFetchAction;
    private final TransportSearchQueryThenFetchAction queryThenFetchAction;
    private final TransportSearchDfsQueryAndFetchAction dfsQueryAndFetchAction;
    private final TransportSearchQueryAndFetchAction queryAndFetchAction;
    private final TransportSearchScanAction scanAction;
    private final TransportSearchCountAction countAction;
    private final boolean optimizeSingleShard;

    @Inject
    public TransportSearchActionX(Settings settings, ThreadPool threadPool,
                                 TransportService transportService, ClusterService clusterService,
                                 TransportSearchDfsQueryThenFetchAction dfsQueryThenFetchAction,
                                 TransportSearchQueryThenFetchAction queryThenFetchAction,
                                 TransportSearchDfsQueryAndFetchAction dfsQueryAndFetchAction,
                                 TransportSearchQueryAndFetchAction queryAndFetchAction,
                                 TransportSearchScanAction scanAction,
                                 TransportSearchCountAction countAction, ActionFilters actionFilters) {
        super(settings, SearchActionX.NAME, threadPool,actionFilters);
        this.clusterService = clusterService;
        this.dfsQueryThenFetchAction = dfsQueryThenFetchAction;
        this.queryThenFetchAction = queryThenFetchAction;
        this.dfsQueryAndFetchAction = dfsQueryAndFetchAction;
        this.queryAndFetchAction = queryAndFetchAction;
        this.scanAction = scanAction;
        this.countAction = countAction;

        this.optimizeSingleShard = componentSettings.getAsBoolean("optimize_single_shard", true);

        transportService.registerHandler(SearchActionX.NAME, new TransportHandler());
    }

    @Override
    protected void doExecute(SearchRequestX searchRequest, ActionListener<SearchResponse> listener) {
        // optimize search type for cases where there is only one shard group to search on
        if (optimizeSingleShard && searchRequest.searchType() != SCAN && searchRequest.searchType() != COUNT) {
            try {
                ClusterState clusterState = clusterService.state();
                String[] concreteIndices = clusterState.metaData().concreteIndices(searchRequest.indicesOptions(), searchRequest.indices());
                Map<String, Set<String>> routingMap = clusterState.metaData().resolveSearchRouting(searchRequest.routing(), searchRequest.indices());
                int shardCount = clusterService.operationRouting().searchShardsCount(clusterState, searchRequest.indices(), concreteIndices, routingMap, searchRequest.preference());
                if (shardCount == 1) {
                    // if we only have one group, then we always want Q_A_F, no need for DFS, and no need to do THEN since we hit one shard
                    searchRequest.searchType(QUERY_AND_FETCH);
                }
            } catch (IndexMissingException e) {
                // ignore this, we will notify the search response if its really the case
                // from the actual action
            } catch (Exception e) {
                logger.debug("failed to optimize search type, continue as normal", e);
            }
        }

        if (searchRequest.searchType() == DFS_QUERY_THEN_FETCH) {
            dfsQueryThenFetchAction.execute(searchRequest, listener);
        } else if (searchRequest.searchType() == SearchType.QUERY_THEN_FETCH) {
            queryThenFetchAction.execute(searchRequest, listener);
        } else if (searchRequest.searchType() == SearchType.DFS_QUERY_AND_FETCH) {
            dfsQueryAndFetchAction.execute(searchRequest, listener);
        } else if (searchRequest.searchType() == SearchType.QUERY_AND_FETCH) {
            queryAndFetchAction.execute(searchRequest, listener);
        } else if (searchRequest.searchType() == SearchType.SCAN) {
            scanAction.execute(searchRequest, listener);
        } else if (searchRequest.searchType() == SearchType.COUNT) {
            countAction.execute(searchRequest, listener);
        }
    }

    private class TransportHandler extends BaseTransportRequestHandler<SearchRequestX> {

        @Override
        public SearchRequestX newInstance() {
            return new SearchRequestX();
        }

        @Override
        public void messageReceived(final SearchRequestX request, final TransportChannel channel) throws Exception {
            // no need for a threaded listener
            request.listenerThreaded(false);
            execute(request, new ActionListener<SearchResponse>() {
                
                @Override
                public void onResponse(SearchResponse result) {
                    try {
                    	if(result.getHits().hits().length!=0&&request.extraParam()!=null&&!request.extraParam().isEmpty()){
                    		SearchHit[] hits=result.getHits().hits();
                            for(SearchHit hit:hits ){
                            	
                            			if(hit.getFields().get("skuNo")==null) continue;
                            			
                                    String skuNo=hit.getFields().get("skuNo").getValue();
                                    Integer instock=1;//BlackDragonList.getStock(skuNo, Integer.valueOf(request.extraParam()));
                                    List<Object> values=new ArrayList<Object>();
                                    values.add(instock);
                                    InternalSearchHitField field=new InternalSearchHitField("_instock",values);
                                    hit.getFields().put("_instock", field);
                                }
                    		}
                        channel.sendResponse(result);
                    } catch (Throwable e) {
                        onFailure(e);
                    }
                }

                @Override
                public void onFailure(Throwable e) {
                    try {
                        channel.sendResponse(e);
                    } catch (Exception e1) {
                        logger.warn("Failed to send response for search", e1);
                    }
                }
            });
        }

        @Override
        public String executor() {
            return ThreadPool.Names.SAME;
        }
    }
}
