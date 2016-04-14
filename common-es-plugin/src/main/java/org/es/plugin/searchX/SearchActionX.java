/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.es.plugin.searchX;

import org.elasticsearch.action.ClientAction;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;

public class SearchActionX extends ClientAction<SearchRequestX, SearchResponse, SearchRequestBuilderX> {

    
    public static final SearchActionX INSTANCE = new SearchActionX();
    public static final String NAME = "indices:data/read/searchx";

    private SearchActionX() {
        super(NAME);
    }

    @Override
    public SearchResponse newResponse() {
        return new SearchResponse();
    }

    @Override
    public SearchRequestBuilderX newRequestBuilder(Client client) {
        return new SearchRequestBuilderX(client);
    }
}
