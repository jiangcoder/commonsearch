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

import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.rescore.RescoreBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.SuggestBuilder;

import java.util.Map;

public class SearchRequestBuilderX extends ActionRequestBuilder<SearchRequestX, SearchResponse, SearchRequestBuilderX, Client>{

    private SearchSourceBuilder sourceBuilder;

    public SearchRequestBuilderX(Client client) {
        super(client, new SearchRequestX());
    }

    
    public SearchRequestBuilderX setExtraParam(String extraParam){
        request.extraParam(extraParam);
        return this;
    }
    
    /**
     * Sets the indices the search will be executed on.
     */
    public SearchRequestBuilderX setIndices(String... indices) {
        request.indices(indices);
        return this;
    }

    /**
     * The document types to execute the search against. Defaults to be executed against
     * all types.
     */
    public SearchRequestBuilderX setTypes(String... types) {
        request.types(types);
        return this;
    }

    /**
     * The search type to execute, defaults to {@link org.elasticsearch.action.search.SearchType#DEFAULT}.
     */
    public SearchRequestBuilderX setSearchType(SearchType searchType) {
        request.searchType(searchType);
        return this;
    }

    /**
     * The a string representation search type to execute, defaults to {@link SearchType#DEFAULT}. Can be
     * one of "dfs_query_then_fetch"/"dfsQueryThenFetch", "dfs_query_and_fetch"/"dfsQueryAndFetch",
     * "query_then_fetch"/"queryThenFetch", and "query_and_fetch"/"queryAndFetch".
     */
    public SearchRequestBuilderX setSearchType(String searchType) throws ElasticsearchIllegalArgumentException {
        request.searchType(searchType);
        return this;
    }

    /**
     * If set, will enable scrolling of the search request.
     */
    public SearchRequestBuilderX setScroll(Scroll scroll) {
        request.scroll(scroll);
        return this;
    }

    /**
     * If set, will enable scrolling of the search request for the specified timeout.
     */
    public SearchRequestBuilderX setScroll(TimeValue keepAlive) {
        request.scroll(keepAlive);
        return this;
    }

    /**
     * If set, will enable scrolling of the search request for the specified timeout.
     */
    public SearchRequestBuilderX setScroll(String keepAlive) {
        request.scroll(keepAlive);
        return this;
    }

    /**
     * An optional timeout to control how long search is allowed to take.
     */
    public SearchRequestBuilderX setTimeout(TimeValue timeout) {
        sourceBuilder().timeout(timeout);
        return this;
    }

    /**
     * An optional timeout to control how long search is allowed to take.
     */
    public SearchRequestBuilderX setTimeout(String timeout) {
        sourceBuilder().timeout(timeout);
        return this;
    }

    /**
     * A comma separated list of routing values to control the shards the search will be executed on.
     */
    public SearchRequestBuilderX setRouting(String routing) {
        request.routing(routing);
        return this;
    }

    /**
     * The routing values to control the shards that the search will be executed on.
     */
    public SearchRequestBuilderX setRouting(String... routing) {
        request.routing(routing);
        return this;
    }

    /**
     * Sets the preference to execute the search. Defaults to randomize across shards. Can be set to
     * <tt>_local</tt> to prefer local shards, <tt>_primary</tt> to execute only on primary shards, or
     * a custom value, which guarantees that the same order will be used across different requests.
     */
    public SearchRequestBuilderX setPreference(String preference) {
        request.preference(preference);
        return this;
    }

    /**
     * Specifies what type of requested indices to ignore and wildcard indices expressions.
     *
     * For example indices that don't exist.
     */
    public SearchRequestBuilderX setIndicesOptions(IndicesOptions indicesOptions) {
        request().indicesOptions(indicesOptions);
        return this;
    }

    /**
     * Constructs a new search source builder with a search query.
     *
     * @see org.elasticsearch.index.query.QueryBuilders
     */
    public SearchRequestBuilderX setQuery(QueryBuilder queryBuilder) {
        sourceBuilder().query(queryBuilder);
        return this;
    }

    /**
     * Constructs a new search source builder with a raw search query.
     */
    public SearchRequestBuilderX setQuery(String query) {
        sourceBuilder().query(query);
        return this;
    }

    /**
     * Constructs a new search source builder with a raw search query.
     */
    public SearchRequestBuilderX setQuery(BytesReference queryBinary) {
        sourceBuilder().query(queryBinary);
        return this;
    }

    /**
     * Constructs a new search source builder with a raw search query.
     */
    public SearchRequestBuilderX setQuery(byte[] queryBinary) {
        sourceBuilder().query(queryBinary);
        return this;
    }

    /**
     * Constructs a new search source builder with a raw search query.
     */
    public SearchRequestBuilderX setQuery(byte[] queryBinary, int queryBinaryOffset, int queryBinaryLength) {
        sourceBuilder().query(queryBinary, queryBinaryOffset, queryBinaryLength);
        return this;
    }

    /**
     * Constructs a new search source builder with a raw search query.
     */
    public SearchRequestBuilderX setQuery(XContentBuilder query) {
        sourceBuilder().query(query);
        return this;
    }

    /**
     * Constructs a new search source builder with a raw search query.
     */
    public SearchRequestBuilderX setQuery(Map query) {
        sourceBuilder().query(query);
        return this;
    }

    /**
     * Sets a filter that will be executed after the query has been executed and only has affect on the search hits
     * (not aggregations or facets). This filter is always executed as last filtering mechanism.
     */
    public SearchRequestBuilderX setPostFilter(FilterBuilder postFilter) {
        sourceBuilder().postFilter(postFilter);
        return this;
    }

    /**
     * Sets a filter on the query executed that only applies to the search query
     * (and not facets for example).
     */
    public SearchRequestBuilderX setPostFilter(String postFilter) {
        sourceBuilder().postFilter(postFilter);
        return this;
    }

    /**
     * Sets a filter on the query executed that only applies to the search query
     * (and not facets for example).
     */
    public SearchRequestBuilderX setPostFilter(BytesReference postFilter) {
        sourceBuilder().postFilter(postFilter);
        return this;
    }

    /**
     * Sets a filter on the query executed that only applies to the search query
     * (and not facets for example).
     */
    public SearchRequestBuilderX setPostFilter(byte[] postFilter) {
        sourceBuilder().postFilter(postFilter);
        return this;
    }

    /**
     * Sets a filter on the query executed that only applies to the search query
     * (and not facets for example).
     */
    public SearchRequestBuilderX setPostFilter(byte[] postFilter, int postFilterOffset, int postFilterLength) {
        sourceBuilder().postFilter(postFilter, postFilterOffset, postFilterLength);
        return this;
    }

    /**
     * Sets a filter on the query executed that only applies to the search query
     * (and not facets for example).
     */
    public SearchRequestBuilderX setPostFilter(XContentBuilder postFilter) {
        sourceBuilder().postFilter(postFilter);
        return this;
    }

    /**
     * Sets a filter on the query executed that only applies to the search query
     * (and not facets for example).
     */
    public SearchRequestBuilderX setPostFilter(Map postFilter) {
        sourceBuilder().postFilter(postFilter);
        return this;
    }

    /**
     * Sets the minimum score below which docs will be filtered out.
     */
    public SearchRequestBuilderX setMinScore(float minScore) {
        sourceBuilder().minScore(minScore);
        return this;
    }

    /**
     * From index to start the search from. Defaults to <tt>0</tt>.
     */
    public SearchRequestBuilderX setFrom(int from) {
        sourceBuilder().from(from);
        return this;
    }

    /**
     * The number of search hits to return. Defaults to <tt>10</tt>.
     */
    public SearchRequestBuilderX setSize(int size) {
        sourceBuilder().size(size);
        return this;
    }

    /**
     * Should each {@link org.elasticsearch.search.SearchHit} be returned with an
     * explanation of the hit (ranking).
     */
    public SearchRequestBuilderX setExplain(boolean explain) {
        sourceBuilder().explain(explain);
        return this;
    }

    /**
     * Should each {@link org.elasticsearch.search.SearchHit} be returned with its
     * version.
     */
    public SearchRequestBuilderX setVersion(boolean version) {
        sourceBuilder().version(version);
        return this;
    }

    /**
     * Sets the boost a specific index will receive when the query is executeed against it.
     *
     * @param index      The index to apply the boost against
     * @param indexBoost The boost to apply to the index
     */
    public SearchRequestBuilderX addIndexBoost(String index, float indexBoost) {
        sourceBuilder().indexBoost(index, indexBoost);
        return this;
    }

    /**
     * The stats groups this request will be aggregated under.
     */
    public SearchRequestBuilderX setStats(String... statsGroups) {
        sourceBuilder().stats(statsGroups);
        return this;
    }

    /**
     * Sets no fields to be loaded, resulting in only id and type to be returned per field.
     */
    public SearchRequestBuilderX setNoFields() {
        sourceBuilder().noFields();
        return this;
    }

    /**
     * Indicates whether the response should contain the stored _source for every hit
     *
     * @param fetch
     * @return
     */
    public SearchRequestBuilderX setFetchSource(boolean fetch) {
        sourceBuilder().fetchSource(fetch);
        return this;
    }

    /**
     * Indicate that _source should be returned with every hit, with an "include" and/or "exclude" set which can include simple wildcard
     * elements.
     *
     * @param include An optional include (optionally wildcarded) pattern to filter the returned _source
     * @param exclude An optional exclude (optionally wildcarded) pattern to filter the returned _source
     */
    public SearchRequestBuilderX setFetchSource(@Nullable String include, @Nullable String exclude) {
        sourceBuilder().fetchSource(include, exclude);
        return this;
    }

    /**
     * Indicate that _source should be returned with every hit, with an "include" and/or "exclude" set which can include simple wildcard
     * elements.
     *
     * @param includes An optional list of include (optionally wildcarded) pattern to filter the returned _source
     * @param excludes An optional list of exclude (optionally wildcarded) pattern to filter the returned _source
     */
    public SearchRequestBuilderX setFetchSource(@Nullable String[] includes, @Nullable String[] excludes) {
        sourceBuilder().fetchSource(includes, excludes);
        return this;
    }


    /**
     * Adds a field to load and return (note, it must be stored) as part of the search request.
     * If none are specified, the source of the document will be return.
     */
    public SearchRequestBuilderX addField(String field) {
        sourceBuilder().field(field);
        return this;
    }

    /**
     * Adds a field data based field to load and return. The field does not have to be stored,
     * but its recommended to use non analyzed or numeric fields.
     *
     * @param name The field to get from the field data cache
     */
    public SearchRequestBuilderX addFieldDataField(String name) {
        sourceBuilder().fieldDataField(name);
        return this;
    }

    /**
     * Adds a script based field to load and return. The field does not have to be stored,
     * but its recommended to use non analyzed or numeric fields.
     *
     * @param name   The name that will represent this value in the return hit
     * @param script The script to use
     */
    public SearchRequestBuilderX addScriptField(String name, String script) {
        sourceBuilder().scriptField(name, script);
        return this;
    }

    /**
     * Adds a script based field to load and return. The field does not have to be stored,
     * but its recommended to use non analyzed or numeric fields.
     *
     * @param name   The name that will represent this value in the return hit
     * @param script The script to use
     * @param params Parameters that the script can use.
     */
    public SearchRequestBuilderX addScriptField(String name, String script, Map<String, Object> params) {
        sourceBuilder().scriptField(name, script, params);
        return this;
    }

    /**
     * Adds a partial field based on _source, with an "include" and/or "exclude" set which can include simple wildcard
     * elements.
     *
     * @deprecated since 1.0.0
     * use {@link org.elasticsearch.action.search.SearchRequestBuilderX#setFetchSource(String, String)} instead
     *
     * @param name    The name of the field
     * @param include An optional include (optionally wildcarded) pattern from _source
     * @param exclude An optional exclude (optionally wildcarded) pattern from _source
     */
    @Deprecated
    public SearchRequestBuilderX addPartialField(String name, @Nullable String include, @Nullable String exclude) {
        sourceBuilder().partialField(name, include, exclude);
        return this;
    }

    /**
     * Adds a partial field based on _source, with an "includes" and/or "excludes set which can include simple wildcard
     * elements.
     *
     * @deprecated since 1.0.0
     * use {@link org.elasticsearch.action.search.SearchRequestBuilderX#setFetchSource(String[], String[])} instead
     *
     * @param name     The name of the field
     * @param includes An optional list of includes (optionally wildcarded) patterns from _source
     * @param excludes An optional list of excludes (optionally wildcarded) patterns from _source
     */
    @Deprecated
    public SearchRequestBuilderX addPartialField(String name, @Nullable String[] includes, @Nullable String[] excludes) {
        sourceBuilder().partialField(name, includes, excludes);
        return this;
    }

    /**
     * Adds a script based field to load and return. The field does not have to be stored,
     * but its recommended to use non analyzed or numeric fields.
     *
     * @param name   The name that will represent this value in the return hit
     * @param lang   The language of the script
     * @param script The script to use
     * @param params Parameters that the script can use (can be <tt>null</tt>).
     */
    public SearchRequestBuilderX addScriptField(String name, String lang, String script, Map<String, Object> params) {
        sourceBuilder().scriptField(name, lang, script, params);
        return this;
    }

    /**
     * Adds a sort against the given field name and the sort ordering.
     *
     * @param field The name of the field
     * @param order The sort ordering
     */
    public SearchRequestBuilderX addSort(String field, SortOrder order) {
        sourceBuilder().sort(field, order);
        return this;
    }

    /**
     * Adds a generic sort builder.
     *
     * @see org.elasticsearch.search.sort.SortBuilders
     */
    public SearchRequestBuilderX addSort(SortBuilder sort) {
        sourceBuilder().sort(sort);
        return this;
    }

    /**
     * Applies when sorting, and controls if scores will be tracked as well. Defaults to
     * <tt>false</tt>.
     */
    public SearchRequestBuilderX setTrackScores(boolean trackScores) {
        sourceBuilder().trackScores(trackScores);
        return this;
    }

    /**
     * Adds the fields to load and return as part of the search request. If none are specified,
     * the source of the document will be returned.
     */
    public SearchRequestBuilderX addFields(String... fields) {
        sourceBuilder().fields(fields);
        return this;
    }

    /**
     * Adds a facet to the search operation.
     * @deprecated Facets are deprecated and will be removed in a future release. Please use aggregations instead.
     */
    @Deprecated
    public SearchRequestBuilderX addFacet(FacetBuilder facet) {
        sourceBuilder().facet(facet);
        return this;
    }

    /**
     * Sets a raw (xcontent) binary representation of facets to use.
     * @deprecated Facets are deprecated and will be removed in a future release. Please use aggregations instead.
     */
    @Deprecated
    public SearchRequestBuilderX setFacets(BytesReference facets) {
        sourceBuilder().facets(facets);
        return this;
    }

    /**
     * Sets a raw (xcontent) binary representation of facets to use.
     * @deprecated Facets are deprecated and will be removed in a future release. Please use aggregations instead.
     */
    @Deprecated
    public SearchRequestBuilderX setFacets(byte[] facets) {
        sourceBuilder().facets(facets);
        return this;
    }

    /**
     * Sets a raw (xcontent) binary representation of facets to use.
     * @deprecated Facets are deprecated and will be removed in a future release. Please use aggregations instead.
     */
    @Deprecated
    public SearchRequestBuilderX setFacets(byte[] facets, int facetsOffset, int facetsLength) {
        sourceBuilder().facets(facets, facetsOffset, facetsLength);
        return this;
    }

    /**
     * Sets a raw (xcontent) binary representation of facets to use.
     * @deprecated Facets are deprecated and will be removed in a future release. Please use aggregations instead.
     */
    @Deprecated
    public SearchRequestBuilderX setFacets(XContentBuilder facets) {
        sourceBuilder().facets(facets);
        return this;
    }

    /**
     * Sets a raw (xcontent) binary representation of facets to use.
     * @deprecated Facets are deprecated and will be removed in a future release. Please use aggregations instead.
     */
    @Deprecated
    public SearchRequestBuilderX setFacets(Map facets) {
        sourceBuilder().facets(facets);
        return this;
    }

    /**
     * Adds an get to the search operation.
     */
    public SearchRequestBuilderX addAggregation(AbstractAggregationBuilder aggregation) {
        sourceBuilder().aggregation(aggregation);
        return this;
    }

    /**
     * Sets a raw (xcontent) binary representation of addAggregation to use.
     */
    public SearchRequestBuilderX setAggregations(BytesReference aggregations) {
        sourceBuilder().aggregations(aggregations);
        return this;
    }

    /**
     * Sets a raw (xcontent) binary representation of addAggregation to use.
     */
    public SearchRequestBuilderX setAggregations(byte[] aggregations) {
        sourceBuilder().aggregations(aggregations);
        return this;
    }

    /**
     * Sets a raw (xcontent) binary representation of addAggregation to use.
     */
    public SearchRequestBuilderX setAggregations(byte[] aggregations, int aggregationsOffset, int aggregationsLength) {
        sourceBuilder().facets(aggregations, aggregationsOffset, aggregationsLength);
        return this;
    }

    /**
     * Sets a raw (xcontent) binary representation of addAggregation to use.
     */
    public SearchRequestBuilderX setAggregations(XContentBuilder aggregations) {
        sourceBuilder().aggregations(aggregations);
        return this;
    }

    /**
     * Sets a raw (xcontent) binary representation of addAggregation to use.
     */
    public SearchRequestBuilderX setAggregations(Map aggregations) {
        sourceBuilder().aggregations(aggregations);
        return this;
    }

    /**
     * Adds a field to be highlighted with default fragment size of 100 characters, and
     * default number of fragments of 5.
     *
     * @param name The field to highlight
     */
    public SearchRequestBuilderX addHighlightedField(String name) {
        highlightBuilder().field(name);
        return this;
    }


    /**
     * Adds a field to be highlighted with a provided fragment size (in characters), and
     * default number of fragments of 5.
     *
     * @param name         The field to highlight
     * @param fragmentSize The size of a fragment in characters
     */
    public SearchRequestBuilderX addHighlightedField(String name, int fragmentSize) {
        highlightBuilder().field(name, fragmentSize);
        return this;
    }

    /**
     * Adds a field to be highlighted with a provided fragment size (in characters), and
     * a provided (maximum) number of fragments.
     *
     * @param name              The field to highlight
     * @param fragmentSize      The size of a fragment in characters
     * @param numberOfFragments The (maximum) number of fragments
     */
    public SearchRequestBuilderX addHighlightedField(String name, int fragmentSize, int numberOfFragments) {
        highlightBuilder().field(name, fragmentSize, numberOfFragments);
        return this;
    }

    /**
     * Adds a field to be highlighted with a provided fragment size (in characters),
     * a provided (maximum) number of fragments and an offset for the highlight.
     *
     * @param name              The field to highlight
     * @param fragmentSize      The size of a fragment in characters
     * @param numberOfFragments The (maximum) number of fragments
     */
    public SearchRequestBuilderX addHighlightedField(String name, int fragmentSize, int numberOfFragments,
                                                    int fragmentOffset) {
        highlightBuilder().field(name, fragmentSize, numberOfFragments, fragmentOffset);
        return this;
    }

    /**
     * Adds a highlighted field.
     */
    public SearchRequestBuilderX addHighlightedField(HighlightBuilder.Field field) {
        highlightBuilder().field(field);
        return this;
    }

    /**
     * Set a tag scheme that encapsulates a built in pre and post tags. The allows schemes
     * are <tt>styled</tt> and <tt>default</tt>.
     *
     * @param schemaName The tag scheme name
     */
    public SearchRequestBuilderX setHighlighterTagsSchema(String schemaName) {
        highlightBuilder().tagsSchema(schemaName);
        return this;
    }

    public SearchRequestBuilderX setHighlighterFragmentSize(Integer fragmentSize) {
        highlightBuilder().fragmentSize(fragmentSize);
        return this;
    }

    public SearchRequestBuilderX setHighlighterNumOfFragments(Integer numOfFragments) {
        highlightBuilder().numOfFragments(numOfFragments);
        return this;
    }

    public SearchRequestBuilderX setHighlighterFilter(Boolean highlightFilter) {
        highlightBuilder().highlightFilter(highlightFilter);
        return this;
    }

    /**
     * The encoder to set for highlighting
     */
    public SearchRequestBuilderX setHighlighterEncoder(String encoder) {
        highlightBuilder().encoder(encoder);
        return this;
    }

    /**
     * Explicitly set the pre tags that will be used for highlighting.
     */
    public SearchRequestBuilderX setHighlighterPreTags(String... preTags) {
        highlightBuilder().preTags(preTags);
        return this;
    }

    /**
     * Explicitly set the post tags that will be used for highlighting.
     */
    public SearchRequestBuilderX setHighlighterPostTags(String... postTags) {
        highlightBuilder().postTags(postTags);
        return this;
    }

    /**
     * The order of fragments per field. By default, ordered by the order in the
     * highlighted text. Can be <tt>score</tt>, which then it will be ordered
     * by score of the fragments.
     */
    public SearchRequestBuilderX setHighlighterOrder(String order) {
        highlightBuilder().order(order);
        return this;
    }

    public SearchRequestBuilderX setHighlighterRequireFieldMatch(boolean requireFieldMatch) {
        highlightBuilder().requireFieldMatch(requireFieldMatch);
        return this;
    }

    public SearchRequestBuilderX setHighlighterBoundaryMaxScan(Integer boundaryMaxScan) {
        highlightBuilder().boundaryMaxScan(boundaryMaxScan);
        return this;
    }

    public SearchRequestBuilderX setHighlighterBoundaryChars(char[] boundaryChars) {
        highlightBuilder().boundaryChars(boundaryChars);
        return this;
    }

    /**
     * The highlighter type to use.
     */
    public SearchRequestBuilderX setHighlighterType(String type) {
        highlightBuilder().highlighterType(type);
        return this;
    }

    public SearchRequestBuilderX setHighlighterFragmenter(String fragmenter) {
        highlightBuilder().fragmenter(fragmenter);
        return this;
    }

    /**
     * Sets a query to be used for highlighting all fields instead of the search query.
     */
    public SearchRequestBuilderX setHighlighterQuery(QueryBuilder highlightQuery) {
        highlightBuilder().highlightQuery(highlightQuery);
        return this;
    }

    /**
     * Sets the size of the fragment to return from the beginning of the field if there are no matches to
     * highlight and the field doesn't also define noMatchSize.
     * @param noMatchSize integer to set or null to leave out of request.  default is null.
     * @return this builder for chaining
     */
    public SearchRequestBuilderX setHighlighterNoMatchSize(Integer noMatchSize) {
        highlightBuilder().noMatchSize(noMatchSize);
        return this;
    }

    /**
     * Sets the maximum number of phrases the fvh will consider if the field doesn't also define phraseLimit.
     */
    public SearchRequestBuilderX setHighlighterPhraseLimit(Integer phraseLimit) {
        highlightBuilder().phraseLimit(phraseLimit);
        return this;
    }

    public SearchRequestBuilderX setHighlighterOptions(Map<String, Object> options) {
        highlightBuilder().options(options);
        return this;
    }

    /**
     * Forces to highlight fields based on the source even if fields are stored separately.
     */
    public SearchRequestBuilderX setHighlighterForceSource(Boolean forceSource) {
        highlightBuilder().forceSource(forceSource);
        return this;
    }

    /**
     * Send the fields to be highlighted using a syntax that is specific about the order in which they should be highlighted.
     * @return this for chaining
     */
    public SearchRequestBuilderX setHighlighterExplicitFieldOrder(boolean explicitFieldOrder) {
        highlightBuilder().useExplicitFieldOrder(explicitFieldOrder);
        return this;
    }

    /**
     * Delegates to {@link org.elasticsearch.search.suggest.SuggestBuilder#setText(String)}.
     */
    public SearchRequestBuilderX setSuggestText(String globalText) {
        suggestBuilder().setText(globalText);
        return this;
    }

    /**
     * Delegates to {@link org.elasticsearch.search.suggest.SuggestBuilder#addSuggestion(org.elasticsearch.search.suggest.SuggestBuilder.SuggestionBuilder)}.
     */
    public SearchRequestBuilderX addSuggestion(SuggestBuilder.SuggestionBuilder<?> suggestion) {
        suggestBuilder().addSuggestion(suggestion);
        return this;
    }

    /**
     * Clears all rescorers on the builder and sets the first one.  To use multiple rescore windows use
     * {@link #addRescorer(org.elasticsearch.search.rescore.RescoreBuilder.Rescorer, int)}.
     * @param rescorer rescorer configuration
     * @return this for chaining
     */
    public SearchRequestBuilderX setRescorer(RescoreBuilder.Rescorer rescorer) {
        sourceBuilder().clearRescorers();
        return addRescorer(rescorer);
    }

    /**
     * Clears all rescorers on the builder and sets the first one.  To use multiple rescore windows use
     * {@link #addRescorer(org.elasticsearch.search.rescore.RescoreBuilder.Rescorer, int)}.
     * @param rescorer rescorer configuration
     * @param window rescore window
     * @return this for chaining
     */
    public SearchRequestBuilderX setRescorer(RescoreBuilder.Rescorer rescorer, int window) {
        sourceBuilder().clearRescorers();
        return addRescorer(rescorer, window);
    }

    /**
     * Adds a new rescorer.
     * @param rescorer rescorer configuration
     * @return this for chaining
     */
    public SearchRequestBuilderX addRescorer(RescoreBuilder.Rescorer rescorer) {
        sourceBuilder().addRescorer(new RescoreBuilder().rescorer(rescorer));
        return this;
    }

    /**
     * Adds a new rescorer.
     * @param rescorer rescorer configuration
     * @param window rescore window
     * @return this for chaining
     */
    public SearchRequestBuilderX addRescorer(RescoreBuilder.Rescorer rescorer, int window) {
        sourceBuilder().addRescorer(new RescoreBuilder().rescorer(rescorer).windowSize(window));
        return this;
    }

    /**
     * Clears all rescorers from the builder.
     * @return this for chaining
     */
    public SearchRequestBuilderX clearRescorers() {
        sourceBuilder().clearRescorers();
        return this;
    }

    /**
     * Sets the rescore window for all rescorers that don't specify a window when added.
     * @param window rescore window
     * @return this for chaining
     */
    public SearchRequestBuilderX setRescoreWindow(int window) {
        sourceBuilder().defaultRescoreWindowSize(window);
        return this;
    }

    /**
     * Sets the source of the request as a json string. Note, settings anything other
     * than the search type will cause this source to be overridden, consider using
     * {@link #setExtraSource(String)}.
     */
    public SearchRequestBuilderX setSource(String source) {
        request.source(source);
        return this;
    }

    /**
     * Sets the source of the request as a json string. Allows to set other parameters.
     */
    public SearchRequestBuilderX setExtraSource(String source) {
        request.extraSource(source);
        return this;
    }

    /**
     * Sets the source of the request as a json string. Note, settings anything other
     * than the search type will cause this source to be overridden, consider using
     * {@link #setExtraSource(BytesReference)}.
     */
    public SearchRequestBuilderX setSource(BytesReference source) {
        request.source(source, false);
        return this;
    }

    /**
     * Sets the source of the request as a json string. Note, settings anything other
     * than the search type will cause this source to be overridden, consider using
     * {@link #setExtraSource(BytesReference)}.
     */
    public SearchRequestBuilderX setSource(BytesReference source, boolean unsafe) {
        request.source(source, unsafe);
        return this;
    }


    /**
     * Sets the source of the request as a json string. Note, settings anything other
     * than the search type will cause this source to be overridden, consider using
     * {@link #setExtraSource(byte[])}.
     */
    public SearchRequestBuilderX setSource(byte[] source) {
        request.source(source);
        return this;
    }

    /**
     * Sets the source of the request as a json string. Allows to set other parameters.
     */
    public SearchRequestBuilderX setExtraSource(BytesReference source) {
        request.extraSource(source, false);
        return this;
    }

    /**
     * Sets the source of the request as a json string. Allows to set other parameters.
     */
    public SearchRequestBuilderX setExtraSource(BytesReference source, boolean unsafe) {
        request.extraSource(source, unsafe);
        return this;
    }

    /**
     * Sets the source of the request as a json string. Allows to set other parameters.
     */
    public SearchRequestBuilderX setExtraSource(byte[] source) {
        request.extraSource(source);
        return this;
    }

    /**
     * Sets the source of the request as a json string. Note, settings anything other
     * than the search type will cause this source to be overridden, consider using
     * {@link #setExtraSource(byte[])}.
     */
    public SearchRequestBuilderX setSource(byte[] source, int offset, int length) {
        request.source(source, offset, length);
        return this;
    }

    /**
     * Sets the source of the request as a json string. Allows to set other parameters.
     */
    public SearchRequestBuilderX setExtraSource(byte[] source, int offset, int length) {
        request.extraSource(source, offset, length);
        return this;
    }

    /**
     * Sets the source of the request as a json string. Note, settings anything other
     * than the search type will cause this source to be overridden, consider using
     * {@link #setExtraSource(byte[])}.
     */
    public SearchRequestBuilderX setSource(XContentBuilder builder) {
        request.source(builder);
        return this;
    }

    /**
     * Sets the source of the request as a json string. Allows to set other parameters.
     */
    public SearchRequestBuilderX setExtraSource(XContentBuilder builder) {
        request.extraSource(builder);
        return this;
    }

    /**
     * Sets the source of the request as a map. Note, setting anything other than the
     * search type will cause this source to be overridden, consider using
     * {@link #setExtraSource(java.util.Map)}.
     */
    public SearchRequestBuilderX setSource(Map source) {
        request.source(source);
        return this;
    }

    public SearchRequestBuilderX setExtraSource(Map source) {
        request.extraSource(source);
        return this;
    }

    /**
     * template stuff
     */

    public SearchRequestBuilderX setTemplateName(String templateName) {
        request.templateName(templateName);
        return this;
    }

    public SearchRequestBuilderX setTemplateType(ScriptService.ScriptType templateType) {
        request.templateType(templateType);
        return this;
    }


    public SearchRequestBuilderX setTemplateSource(String source) {
        request.templateSource(source);
        return this;
    }

    public SearchRequestBuilderX setTemplateSource(BytesReference source) {
        request.templateSource(source, true);
        return this;
    }

    /**
     * Sets the source builder to be used with this request. Note, any operations done
     * on this require builder before are discarded as this internal builder replaces
     * what has been built up until this point.
     */
    public SearchRequestBuilderX internalBuilder(SearchSourceBuilder sourceBuilder) {
        this.sourceBuilder = sourceBuilder;
        return this;
    }

    /**
     * Returns the internal search source builder used to construct the request.
     */
    public SearchSourceBuilder internalBuilder() {
        return sourceBuilder();
    }

    @Override
    public String toString() {
        return internalBuilder().toString();
    }

    @Override
    public SearchRequestX request() {
        if (sourceBuilder != null) {
            request.source(sourceBuilder());
        }
        return request;
    }

    @Override
    protected void doExecute(ActionListener<SearchResponse> listener) {
        if (sourceBuilder != null) {
            request.source(sourceBuilder());
        }
//        client.search(request, listener);
        client.execute(SearchActionX.INSTANCE, request,listener);
    }

    private SearchSourceBuilder sourceBuilder() {
        if (sourceBuilder == null) {
            sourceBuilder = new SearchSourceBuilder();
        }
        return sourceBuilder;
    }

    private HighlightBuilder highlightBuilder() {
        return sourceBuilder().highlighter();
    }

    private SuggestBuilder suggestBuilder() {
        return sourceBuilder().suggest();
    }
}
