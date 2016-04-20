//package com.jiangcoder.search.es;
//
//import static org.elasticsearch.search.aggregations.AggregationBuilders.nested;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import org.apache.commons.lang3.StringUtils;
//import org.elasticsearch.action.search.SearchRequestBuilder;
//import org.elasticsearch.action.search.SearchResponse;
//import org.elasticsearch.index.query.BoolFilterBuilder;
//import org.elasticsearch.index.query.FilterBuilder;
//import org.elasticsearch.index.query.FilterBuilders;
//import org.elasticsearch.index.query.NestedQueryBuilder;
//import org.elasticsearch.index.query.QueryBuilders;
//import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
//import org.elasticsearch.index.query.functionscore.script.ScriptScoreFunctionBuilder;
//import org.elasticsearch.index.query.support.QueryInnerHitBuilder;
//import org.elasticsearch.search.aggregations.Aggregation;
//import org.elasticsearch.search.aggregations.AggregationBuilder;
//import org.elasticsearch.search.aggregations.AggregationBuilders;
//import org.elasticsearch.search.aggregations.InternalAggregations;
//import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter;
//import org.elasticsearch.search.aggregations.bucket.nested.Nested;
//import org.elasticsearch.search.aggregations.bucket.terms.Terms;
//import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
//import org.elasticsearch.search.sort.SortBuilders;
//import org.elasticsearch.search.sort.SortOrder;
//import org.es.plugin.productqueryscore.functionquery.CustomProductScoreQueryBuilder;
//import org.es.plugin.searchX.SearchRequestBuilderX;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.google.common.collect.Lists;
//import com.mongodb.BasicDBObject;
//
//public class QueryBuilder {
//	public static String ES_PREFERENCE="_local";
//	private SearchRequestBuilderX searchBuilder;
//
//	private boolean hasProductIdCondition = false;
//
//	private List<String> returnFields = null;
//
//	private boolean debug = false;
//	
//	private Integer facetSize = 200;// 默认所有facet 最大200
//
//	private ScriptScoreFunctionBuilder functionBuilder;
//
//	private List<FilterBuilder> skuFilters = Lists.newArrayList();
//	
//	public List<FilterBuilder> getSkuFilters() {
//		return skuFilters;
//	}
//
//	public void setSkuFilters(List<FilterBuilder> skuFilters) {
//		this.skuFilters = skuFilters;
//	}
//
//	
//	private List<FilterBuilder> productFilters = Lists.newArrayList();
//
//	
//	public List<FilterBuilder> getProductFilters() {
//		return productFilters;
//	}
//
//	public void setProductFilters(List<FilterBuilder> productFilters) {
//		this.productFilters = productFilters;
//	}
//
//	protected  Logger logger = LoggerFactory
//			.getLogger(QueryBuilder.class);
//
//	protected List<TermsBuilder> termsBuilders;
//
//	// 去除title字段
//	private final String[] commFields = { "productId", "skuId", "productTag", "skuNo", "salesVolume", "promoScore", "shopId", "shopName", "shopType", "evaluateCount","promoFlag","compareTime"};
//
//
//	private String regionId = null;
//	private String cityId = null;
//	private String catIds = null;
//
//	public enum FilterType {
//		TERM, RANGE, NOT, OR, FILTER
//	}
//
//   private ESClientUtils clientUtils;
//
//	public SearchResponse get() {
//
//		return this.get(null);
//	}
//
//	/**
//	 * @Title: get
//	 * @Description: 获取检索结果
//	 * @param @param query
//	 * @param @param type
//	 * @param @return 设定文件
//	 * @return SearchResponse 返回类型
//	 * @throws
//	 */
//	public SearchResponse get(String query) {
//
//		makeUpQuery(query);
//
//		if (returnFields == null) {
//			searchBuilder.addFields("productId");
//
//		}
//
//		if (debug) {
//			logger.info("search DSL: [{}]",
//					new Object[] { searchBuilder.toString() });
//			debug = false;
//		} else {
//			// 本地测试，提交要注释
////			 logger.info("search DSL: [{}]", new Object[] {
////			 searchBuilder.toString() });
//		}
//
//		SearchResponse rep = searchBuilder.get();
//		if (rep != null && rep.getHits().getTotalHits() > 100000) {
//			logger.info("huge hit result DSL: [{}]",
//					new Object[] { searchBuilder.toString() });
//		}
//		return rep;
//	}
//
//	public QueryBuilder( ) {
//		String esPreference = ES_PREFERENCE;
//		searchBuilder = new SearchRequestBuilderX(ESClientUtils.getTransportClient()).setPreference(
//				esPreference);
//
//	}
//
//	public QueryBuilder preference(String preference) {
//
//		if (StringUtils.isNotEmpty(preference)) {
//			this.searchBuilder.setPreference(preference);
//		} else {
//			this.searchBuilder.setPreference("_local");
//		}
//		return this;
//	}
//
//	public QueryBuilder setIndices(String... indices) {
//		searchBuilder.setIndices(indices);
//		return this;
//	}
//
//	public QueryBuilder setTypes(String... types) {
//		searchBuilder.setTypes(types);
//		return this;
//	}
//
//	public QueryBuilder addSkuTermFilter(String field,
//			Object value) {
//		if (StringUtils.isNotEmpty(field)
//				&& field.equals("sku." + ChildIndexStructure.FIELD_PRODID)) {
//			this.hasProductIdCondition = true;
//		}
//		return addFilter(FilterType.TERM, field, value, null, null, null, null,
//				skuFilters);
//	}
//
//	public QueryBuilder addFilter(FilterType type,
//			String field, Object value, Object toValue, FilterBuilder filter,
//			List<?> orValues, BasicDBObject orFilters,
//			List<FilterBuilder> filters) {
//
//		switch (type) {
//		case TERM:
//			filters.add(FilterBuilders.termFilter(field, value));
//			break;
//		case RANGE:
//			filters.add(FilterBuilders.rangeFilter(field).from(value)
//					.to(toValue).cache(true));
//			break;
//		case NOT:
//			// filters.add(FilterBuilders.notFilter(FilterBuilders.termFilter(field,
//			// value)).cache(true));
//			filters.add(FilterBuilders.boolFilter().cache(true)
//					.mustNot(FilterBuilders.termFilter(field, value)));
//			break;
//		case OR:
//
//			if (orValues == null || orValues.size() < 1) {
//				if (orFilters != null && orFilters.size() > 0)
//					filters.add(buildOrFilter(orFilters));
//				else
//					return this;
//			} else {
//				filters.add(buildOrFilter(field, orValues));
//			}
//			break;
//		case FILTER:
//			filters.add(filter);
//			break;
//		}
//		return this;
//	}
//
//	public BoolFilterBuilder buildOrFilter(String field, List<?> orValues) {
//		BoolFilterBuilder orFilter = FilterBuilders.boolFilter().cache(true);
//		for (Object obj : orValues) {
//			orFilter.should(FilterBuilders.termFilter(field, obj));
//		}
//		return orFilter;
//	}
//
//	public BoolFilterBuilder buildOrFilter(BasicDBObject orFilters) {
//
//		BoolFilterBuilder orFilter = FilterBuilders.boolFilter().cache(true);
//		for (Map.Entry<?, ?> entry : orFilters.entrySet()) {
//			orFilter.should(FilterBuilders.termFilter((String) entry.getKey(),
//					entry.getValue()));
//		}
//
//		return orFilter;
//	}
//
//	private int gomeSortType = 0;
//	
//	public QueryBuilder setScriptParm(String key,
//			Object value) {
//		setScript(null);
//		if (!StringUtils.isEmpty(key)) {
//			if (key.equals("gomeSortType")) {
//				this.gomeSortType = (Integer) value;
//			} else if (key.equals("gomeSortAsc")) {
//			}
//		}
//		functionBuilder.param(key, value);
//		return this;
//	}
//
//	private static final String defaultScript = "esproduct";
//
//	private static final CharSequence SQL_ALL = "*:*";
//
//	public QueryBuilder setScript(String script) {
//		if (functionBuilder == null)
//			functionBuilder = ScoreFunctionBuilders.scriptFunction(StringUtils
//					.isEmpty(script) ? defaultScript : script);
//		return this;
//	}
//
//	private Boolean fake = null;
//
//	public QueryBuilder fake(boolean fake) {
//		this.fake = fake;
//		return this;
//	}
//
//	public QueryBuilder catIds(String catIds) {
//		this.catIds = catIds;
//		return this;
//	}
//
//	public QueryBuilder regionId(String regionId) {
//		this.regionId = regionId;
//		return this;
//	}
//	/**
//	 * @Title: makeUpQuery
//	 * @Description: 组成检索语句
//	 * @param @param sql
//	 * @param @param type
//	 * @param @param skuFilterBuilder
//	 * @param @param productFilterBuilder
//	 * @param @return 设定文件
//	 * @return QueryBuilder 返回类型
//	 * @throws
//	 */
//	public QueryBuilder makeUpQuery(String luceneQueryString) {
//
//		if (!StringUtils.isEmpty(luceneQueryString)) {
//
//			if (functionBuilder == null
//					&& !luceneQueryString
//							.contains(SQL_ALL)) {
//
//				searchBuilder.setQuery(QueryBuilders
//						.queryStringQuery(luceneQueryString));
//
//			} else {
//
//				BoolFilterBuilder skuFilters = getSkuFilter();
//				BoolFilterBuilder productFilters = getProductFilter();
//
//				@SuppressWarnings("rawtypes")
//				AggregationBuilder aggBuilder = null;
//
//				CustomProductScoreQueryBuilder functionQuery = getFunctionQuery(
//						luceneQueryString, skuFilters);
//
//				buildCustomParam(functionQuery);
//				this.makeUpNestedQuery(functionQuery, productFilters);
//				aggBuilder = nested("sku").path("sku");
//				
//				 if(termsBuilders!=null&&termsBuilders.size()>0){
//				 for(TermsBuilder term:termsBuilders){
//				 aggBuilder.subAggregation(term);
//				 }
//				 searchBuilder.addAggregation(aggBuilder);
//				 }
//			}
//		}
//		return this;
//	}
//
//	/**
//	 * @Title: makeUpNestedQuery
//	 * @Description: 组成nested的检索
//	 * @param @param functionQuery
//	 * @param @param productFilterBuilder 设定文件
//	 * @return void 返回类型
//	 * @throws
//	 */
//	private void makeUpNestedQuery(
//			CustomProductScoreQueryBuilder functionQuery,
//			BoolFilterBuilder productFilterBuilder) {
//		QueryInnerHitBuilder nestedInner = new QueryInnerHitBuilder()
//				.setSize(1).addSort("_score", SortOrder.DESC)
//				.setFetchSource(commFields, null);
//
//		if (debug) {
//			nestedInner.setExplain(true);
//			searchBuilder.setExplain(true);
//		}
//
//		for (String field : commFields) {
//			nestedInner.addFieldDataField(field);
//		}
//
//		int innerHitSize = (debug && hasProductIdCondition) ? 20 : 1;
//
//		NestedQueryBuilder nested = QueryBuilders
//				.nestedQuery("sku", functionQuery)
//				.scoreMode("max")
//				.innerHit(
//						nestedInner.setSize(innerHitSize).setFetchSource(
//								"skuId", null));
//
//		if (isProductFilterEmpty()) {
//			searchBuilder.setQuery(nested);
//		} else {
//			searchBuilder.setQuery(QueryBuilders.filteredQuery(nested,
//					productFilterBuilder));
//		}
//	}
//
//	/**
//	 * @Title: isProductFilterEmpty
//	 * @Description: 是否有product层的过滤
//	 * @param @return 设定文件
//	 * @return boolean 返回类型
//	 * @throws
//	 */
//	private boolean isProductFilterEmpty() {
//		if (productFilters == null || productFilters.isEmpty())
//			return true;
//		else
//			return false;
//	}
//
//	/**
//	 * @Title: buildCustomParam
//	 * @Description: 自定义检索参数传递
//	 * @param @param functionQuery 设定文件
//	 * @return void 返回类型
//	 * @throws
//	 */
//	private void buildCustomParam(CustomProductScoreQueryBuilder functionQuery) {
//
//		if (fake != null) {
//			functionQuery.fake(fake);
//		}
//		if (!StringUtils.isEmpty(regionId)) {
//			functionQuery.regionId(regionId);
//		}
//	}
//
//	/**
//	 * @Title: getFunctionQuery
//	 * @Description: 获取函数检索i
//	 * @param @param luceneQueryString
//	 * @param @param skuFilterBuilder
//	 * @param @return 设定文件
//	 * @return CustomFunctionScoreQueryBuilder 返回类型
//	 * @throws
//	 */
//	private CustomProductScoreQueryBuilder getFunctionQuery(
//			String luceneQueryString, BoolFilterBuilder skuFilterBuilder) {
//
//		CustomProductScoreQueryBuilder functionQuery = null;
//		if (luceneQueryString.contains(SQL_ALL)) {
//			functionQuery = new CustomProductScoreQueryBuilder(
//					QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
//							skuFilterBuilder)).add(
//					functionBuilder.lang("native")).boostMode("sum");
//		} else {
//
//			if (isScoreSort()) {
//				functionQuery = new CustomProductScoreQueryBuilder(
//						QueryBuilders.filteredQuery(QueryBuilders
//								.queryStringQuery(luceneQueryString),
//								skuFilterBuilder)).add(
//						functionBuilder.lang("native")).boostMode("sum");
//			} else {
//				functionQuery = new CustomProductScoreQueryBuilder(
//						QueryBuilders.filteredQuery(
//								QueryBuilders
//										.queryStringQuery(luceneQueryString),
//								skuFilterBuilder).boost(0)).add(
//						functionBuilder.lang("native")).boostMode("sum");
//			}
//
//		}
//		return functionQuery;
//	}
//
//	/**
//	 * @Title: isScoreSort
//	 * @Description: 是否是国美综合排序
//	 * @param @return 设定文件
//	 * @return boolean 返回类型
//	 * @throws
//	 */
//	private boolean isScoreSort() {
//		return (this.gomeSortType == 0) ? true : false;
//	}
//
//	/**
//	 * @Title: getSkuFilter
//	 * @Description: 获取sku层的filter
//	 * @param @return 设定文件
//	 * @return BoolFilterBuilder 返回类型
//	 * @throws
//	 */
//	private BoolFilterBuilder getSkuFilter() {
//		if (skuFilters != null && skuFilters.size() > 0) {
//			BoolFilterBuilder filterBuilder = FilterBuilders.boolFilter()
//					.cache(true);
//			for (FilterBuilder filter : skuFilters) {
//				filterBuilder.must(filter);
//			}
//			return filterBuilder;
//		} else {
//			return FilterBuilders.boolFilter().cache(true);
//		}
//	}
//
//	/**
//	 * @Title: getProductFilter
//	 * @Description: 获取product层的filter
//	 * @param @return 设定文件
//	 * @return BoolFilterBuilder 返回类型
//	 * @throws
//	 */
//	private BoolFilterBuilder getProductFilter() {
//		if (productFilters != null && productFilters.size() > 0) {
//			BoolFilterBuilder filterBuilder = FilterBuilders.boolFilter()
//					.cache(true);
//			for (FilterBuilder filter : productFilters) {
//				filterBuilder.must(filter);
//			}
//			return filterBuilder;
//		} else {
//			return FilterBuilders.boolFilter().cache(true);
//		}
//	}
//	
//	public QueryBuilder preference4Test(String ipPort) {
//		String nodeId = clientUtils.getNodeIdByIp(ipPort);
//		if (StringUtils.isNotEmpty(nodeId)) {
//			this.searchBuilder.setPreference("_only_node:" + nodeId);
//			logger.info("preference es server nodeId:{} ,identify:{}", new Object[] { nodeId, ipPort });
//		}
//		return this;
//	}
//
//	//cityId string
//	public QueryBuilder extraParam(String extraParam){
//	
//		this.searchBuilder.setExtraParam(extraParam);
//		return this;
//	}
//
//	public QueryBuilder cityId(String cityId) {
//		this.cityId = cityId;
//		return this;
//	}
//
//	public QueryBuilder addSkuFilter(FilterBuilder filter) {
//
//		return addFilter(FilterType.FILTER, null, null, null, filter, null,
//				null, skuFilters);
//	}
//	
//	public QueryBuilder addSkuRangeFilter(String field, String range) throws Exception{
//		range = range.replace("[", "").replace("]", "").replace("TO ", "").replace("*", String.valueOf(Integer.MAX_VALUE));
//		String[] rangeArr = range.split(" ");
//		if (rangeArr != null && rangeArr.length == 2 && !rangeArr[0].equals("undefined") && !rangeArr[1].equals("undefined")) {
//			return addFilter(FilterType.RANGE, field, Float.parseFloat(rangeArr[0]), Float.parseFloat(rangeArr[1]), null, null, null,skuFilters);
//		} else {
//			return this;
//		}
//	}
//	
//	public QueryBuilder addSkuRangeFilter(String field, Object from, Object to) {
//
//		return addFilter(FilterType.RANGE, field, from, to, null, null, null,skuFilters);
//	}
//	
//	public QueryBuilder addSkuNotFilter(String field, Object value) {
//
//		return addFilter(FilterType.NOT, field, value, null, null, null, null,skuFilters);
//	}
//	
//	public QueryBuilder addFacet(String... fields) {
//
//		if (fields == null || fields.length < 1)
//			return this;
//
//		for (String field : fields)
//			this.addFacet(field, true, null);
//
//		return this;
//	}
//	
//	public QueryBuilder addFacetForShoppingCartSearch(String... fields) {
//
//		if (fields == null || fields.length < 1)
//			return this;
//
//		for (String field : fields)
//			this.addFacet(field, true, null);
//
//		return this;
//	}
//	
//	/**
//	 * @Title: addFacet
//	 * @Description: 
//	 * @param @param field
//	 * @param @param filtered
//	 * @param @param size
//	 * @param @return    设定文件
//	 * @return QueryBuilder    返回类型
//	 * @throws
//	 */
//	public QueryBuilder addFacet(String field, boolean filtered, Integer size) {
//		
//		if(termsBuilders==null){
//			termsBuilders=new ArrayList<TermsBuilder>();
//		}
//		termsBuilders.add(AggregationBuilders.terms(field).field(field).size(size != null && size != 0 ? size : this.facetSize));
//		return this;
//	}
//	
//	public QueryBuilder addFacetForShoppingCartSearch(String field, boolean filtered, Integer size) {
//		
//		if(termsBuilders==null){
//			termsBuilders=new ArrayList<TermsBuilder>();
//		}
//		termsBuilders.add(AggregationBuilders.terms(field).field(field).include("cat*").size(size != null && size != 0 ? size : this.facetSize));
//		return this;
//	}
//	
//	public QueryBuilder from(int from) {
//		this.searchBuilder.setFrom(from);
//		return this;
//	}
//	
//	public QueryBuilder size(int size) {
//		this.searchBuilder.setSize(size);
//		return this;
//	}
//	
//	public QueryBuilder sort(String field, SortOrder order) {
//		this.searchBuilder.addSort(SortBuilders.fieldSort(field).order(order).missing("_last"));
//		return this;
//	}
//	
//	public QueryBuilder addSkuOrFilter(String field, List<?> orValues) {
//
//		return addFilter(FilterType.OR, field, null, null, null, orValues, null,skuFilters);
//	}
//	
//	public QueryBuilder setExplain(boolean enabled) {
//		searchBuilder.setExplain(enabled);
//		debug = true;
//		return this;
//	}
//
//	/**
//	 * @Title: getFacet
//	 * @param @param sql
//	 * @param @param alias
//	 * @param @param type
//	 * @param @return    设定文件
//	 * @return List<Facet>    返回类型
//	 * @throws
//	 */
//	public Map<String,Long> getFacet(String sql,String alias,String type){
//		
//		Map<String,Long> map = new HashMap<String,Long>();
//		
//		try{
//			BoolFilterBuilder skuFilters = getSkuFilter();
//			
//			//only nested,no parentChild	
//			NestedQueryBuilder nested = null;
//			
//			if(!this.isSkuFilterEmpty()){
//				nested=QueryBuilders.nestedQuery("sku", QueryBuilders.filteredQuery(QueryBuilders.queryStringQuery(sql), skuFilters)).scoreMode("max");
//
//			}else{
//				nested=QueryBuilders.nestedQuery("sku", QueryBuilders.queryStringQuery(sql)).scoreMode("max");
//			}
//			
//			@SuppressWarnings("rawtypes")
//			AggregationBuilder aggBuilder=null;
//			@SuppressWarnings("rawtypes")
//			AggregationBuilder subAgg=null;
//			
//			if(!isSkuFilterEmpty()){
//				subAgg=AggregationBuilders.filter("agg_filter").filter(this.getSkuFilter());
//			}
//					
//			aggBuilder=nested("sku").path("sku");
//			
//			if(subAgg!=null){
//				aggBuilder.subAggregation(subAgg);
//				TermsBuilder term = AggregationBuilders.terms(ChildIndexStructure.FIELD_CATS).field(ChildIndexStructure.FIELD_CATS).size(50);
//				subAgg.subAggregation(term);				
//			}
//
//			String esPreference = ES_PREFERENCE;
//
//			SearchRequestBuilder builder= clientUtils.getTransportClient()
//													   .prepareSearch()
//													   .setIndices(alias)
//													   .setTypes(type)
//													   .setPreference(esPreference.trim())
//													   .setQuery(nested)
//													   .setSize(0)
//													   .setFrom(0)
//													   .addAggregation(aggBuilder).setQueryCache(true)
//													   ;
//			
//			
//			SearchResponse rep=builder.get();
//			
//			Nested skuAgg = rep.getAggregations().get("sku");
//			InternalFilter aggFilter = skuAgg.getAggregations().get("agg_filter");
//			InternalAggregations aggs=  aggFilter.getAggregations();
//
//			
//			List<Aggregation> list =aggs.asList();
//			for( Aggregation agg:list ){
//				Terms termAgg = (Terms)agg;
//				String aggName = termAgg.getName();
//				
//				if(termAgg.getBuckets().size()==0)
//					continue;
//				
//				if (aggName.startsWith("categories")) {
//					
//					for (Terms.Bucket entry : termAgg.getBuckets()) {
//					    String catId = entry.getKey();     
//					    if(!catId.startsWith("cat"))
//					    	continue;
////					    BasicDBObject category = categoryDict.getCategory(catId);
////						if (EmptyUtil.isEmpty(catId) || EmptyUtil.isEmpty(category) || !"gome".equalsIgnoreCase(category.getString("catalog"))) {
////							continue;
////						}else{
////							map.put(entry.getKey(), entry.getDocCount());
////						}
//					}
//				}
//			}
//			
//		}catch( Exception e ){
//			logger.info(" get categories facet wrong,e is " + e);
//		}
//		return map;
//	}
//	
//	/**
//	 * @Title: isSkuFilterEmpty
//	 * @Description: 是否有sku层的过滤
//	 * @param @return    设定文件
//	 * @return boolean    返回类型
//	 * @throws
//	 */
//	private boolean isSkuFilterEmpty(){
//		if(skuFilters==null || skuFilters.isEmpty() )
//			return true;
//		else 
//			return false;
//	}
//
//	public QueryBuilder removeSkuFilter(FilterBuilder filter) {
//		skuFilters.remove(filter);
//		return this;
//	}
//	public QueryBuilder addTermFilter(String field, Object value) {
//
//		return addFilter(FilterType.TERM, field, value, null, null, null, null,null);
//
//	}
//}
