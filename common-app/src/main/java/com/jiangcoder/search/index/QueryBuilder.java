package com.jiangcoder.search.index;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.index.query.functionscore.script.ScriptScoreFunctionBuilder;
import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacetBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.es.plugin.product.jiangcoder.JiangcoderScoreQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.jiangcoder.search.es.ESClientUtils;
import com.mongodb.BasicDBObject;

/**
 * 查询实体，填充查询信息
 *
 * @author dinghb
 *
 */
public class QueryBuilder {
	protected static Logger logger = LoggerFactory.getLogger(QueryBuilder.class);
	private List<FilterBuilder> skuFilters = Lists.newArrayList();
	public enum FilterType {
		TERM, RANGE, NOT, OR, FILTER
	}

	private final String[] commFields = { "productId", "skuId", "productTag", "skuNo", "salesVolume", "promoScore", "title", "shopId", "shopName", "shopType", "evaluateCount","promoFlag","name","state","id"};
	private List<FilterBuilder> filters = Lists.newArrayList();
	// private AndFilterBuilder andFilterBuilder;
	BoolFilterBuilder andFilterBuilder;
	private StringBuilder querySql;
	private SearchRequestBuilder searchBuilder;
	private Integer facetSize = 200;// 默认所有facet 最大200
	private ScriptScoreFunctionBuilder functionBuilder;
	private static final String defaultScript = "native-script";
	private boolean debug = false;
	private List<String> returnFields = null;
	private Boolean fake = null;
	private String regionId = null;
	private String cityId = null;
	private String catIds=null;
	private double price=0;
    private int gomeSortType=0;
    private boolean gomeSortAsc=false;
    private String category=null;
	public QueryBuilder() {
		searchBuilder = ESClientUtils.getTransportClient().prepareSearch().setPreference(Global.ES_PREFERENCE).setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public QueryBuilder setScript(String script) {
		if (functionBuilder == null)
			functionBuilder = ScoreFunctionBuilders.scriptFunction(StringUtils.isEmpty(script) ? defaultScript : script);

		return this;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public QueryBuilder setScriptParm(String key, Object value) {

		setScript(null);
		if( !StringUtils.isEmpty(key) ){
			if( key.equals("gomeSortType") ){
				this.gomeSortType = (Integer)value;
			}else if( key.equals("gomeSortAsc") ){
				this.gomeSortAsc = (Boolean)value;
			}
		}
		functionBuilder.param(key, value);
		return this;
	}

	public QueryBuilder setFacetSize(Integer size) {
		this.facetSize = size;
		return this;
	}

	public QueryBuilder preference(String preference) {

		if (StringUtils.isNotEmpty(preference)) {
			this.searchBuilder.setPreference(preference);
		} else {
			this.searchBuilder.setPreference("_local");
		}
		return this;
	}

	public QueryBuilder preference4Test(String ipPort) {
		String nodeId = ESClientUtils.getNodeIdByIp(ipPort);
		if (StringUtils.isNotEmpty(nodeId)) {
			this.searchBuilder.setPreference("_only_node:" + nodeId);
			logger.info("preference es server nodeId:{} ,identify:{}", new Object[] { nodeId, ipPort });
		}
		return this;
	}

	public QueryBuilder from(int from) {
		this.searchBuilder.setFrom(from);
		return this;
	}

	public QueryBuilder size(int size) {
		this.searchBuilder.setSize(size);
		return this;
	}

	public QueryBuilder sort(String field, SortOrder order) {
		this.searchBuilder.addSort(SortBuilders.fieldSort(field).order(order).missing("_last"));
		return this;
	}

	public QueryBuilder regionId(String regionId) {
		this.regionId = regionId;
		return this;
	}
	public QueryBuilder cityId(String cityId) {
		this.cityId = cityId;
		return this;
	}

	public QueryBuilder fake(boolean fake) {
		this.fake = fake;
		return this;
	}
	public QueryBuilder catIds(String catIds) {
		this.catIds = catIds;
		return this;
	}
	public QueryBuilder category(String category) {
		this.category=category;
		return this;
	}
	public QueryBuilder price(float price){
		this.price=price;
		return this;
	}
	public QueryBuilder gomeSortType(int gomeSortType) {
		this.gomeSortType = gomeSortType;
		return this;
	}
	
	public QueryBuilder gomeSortAsc(boolean gomeSortAsc) {
		this.gomeSortAsc = gomeSortAsc;
		return this;
	}

	public QueryBuilder addTermFilter(String field, Object value) {

		return addFilter(FilterType.TERM, field, value, null, null, null, null);

	}

	public QueryBuilder addNotFilter(String field, Object value) {

		return addFilter(FilterType.NOT, field, value, null, null, null, null);

	}

	/**
	 * 格式 [1 to 100]
	 *
	 * @param range
	 * @return
	 */
	public QueryBuilder addRangeFilter(String field, String range) {
		range = range.replace("[", "").replace("]", "").replace("TO ", "").replace("*", String.valueOf(Integer.MAX_VALUE));
		String[] rangeArr = range.split(" ");
		if (rangeArr != null && rangeArr.length == 2 && !rangeArr[0].equals("undefined") && !rangeArr[1].equals("undefined")) {
			try {
				return addFilter(FilterType.RANGE, field, Float.parseFloat(rangeArr[0]), Float.parseFloat(rangeArr[1]), null, null, null);
			} catch (Exception e) {
				logger.error("addRangeFilter", e);
				return this;
			}

		} else {
			return this;
		}
	}

	public QueryBuilder addRangeFilter(String field, Object from, Object to) {

		return addFilter(FilterType.RANGE, field, from, to, null, null, null);

	}

	public QueryBuilder addOrFilter(String field, List orValues) {

		return addFilter(FilterType.OR, field, null, null, null, orValues, null);

	}
	public List<FilterBuilder> getSkuFilters() {
		return skuFilters;
	}

	public void setSkuFilters(List<FilterBuilder> skuFilters) {
		this.skuFilters = skuFilters;
	}
	public QueryBuilder addOrFilter(BasicDBObject orfilters) {

		return addFilter(FilterType.OR, null, null, null, null, null, orfilters);

	}
	public QueryBuilder addFilter(FilterType type,
			String field, Object value, Object toValue, FilterBuilder filter,
			List<?> orValues, BasicDBObject orFilters,
			List<FilterBuilder> filters) {

		switch (type) {
		case TERM:
			filters.add(FilterBuilders.termFilter(field, value));
			break;
		case RANGE:
			filters.add(FilterBuilders.rangeFilter(field).from(value)
					.to(toValue).cache(true));
			break;
		case NOT:
			// filters.add(FilterBuilders.notFilter(FilterBuilders.termFilter(field,
			// value)).cache(true));
			filters.add(FilterBuilders.boolFilter().cache(true)
					.mustNot(FilterBuilders.termFilter(field, value)));
			break;
		case OR:

			if (orValues == null || orValues.size() < 1) {
				if (orFilters != null && orFilters.size() > 0)
					filters.add(buildOrFilter(orFilters));
				else
					return this;
			} else {
				filters.add(buildOrFilter(field, orValues));
			}
			break;
		case FILTER:
			filters.add(filter);
			break;
		}
		return this;
	}
	public QueryBuilder addSkuFilter(FilterBuilder filter) {

		return addFilter(FilterType.FILTER, null, null, null, filter, null,
				null, skuFilters);
	}
	public QueryBuilder addFilter(FilterBuilder filter) {

		return addFilter(FilterType.FILTER, null, null, null, filter, null, null);
	}

	public QueryBuilder addFilter(FilterType type, String field, Object value, Object toValue, FilterBuilder filter, List orValues, BasicDBObject orFilters) {

		if (andFilterBuilder == null) {
			// andFilterBuilder= FilterBuilders.andFilter().cache(true);
			andFilterBuilder = FilterBuilders.boolFilter().cache(true);
			// searchBuilder.setPostFilter(andFilterBuilder);
		}
		switch (type) {
		case TERM:
			filters.add(FilterBuilders.termFilter(field, value));
			break;
		case RANGE:
			filters.add(FilterBuilders.rangeFilter(field).from(value).to(toValue));
			break;
		case NOT:
			// filters.add(FilterBuilders.notFilter(FilterBuilders.termFilter(field,
			// value)).cache(true));
			filters.add(FilterBuilders.boolFilter().cache(true).mustNot(FilterBuilders.termFilter(field, value)).cache(true));
			break;
		case OR:

			if (orValues == null || orValues.size() < 1) {
				if (orFilters != null && orFilters.size() > 0)
					filters.add(buildOrFilter(orFilters));
				else
					return this;
			} else {
				filters.add(buildOrFilter(field, orValues));
			}
			break;
		case FILTER:
			filters.add(filter);
			break;
		}
		return this;
	}

	public BoolFilterBuilder buildOrFilter(String field, List orValues) {
		BoolFilterBuilder orFilter = FilterBuilders.boolFilter().cache(true);
		for (Object obj : orValues) {
			orFilter.should(FilterBuilders.termFilter(field, obj));
		}
		return orFilter;
	}

	public BoolFilterBuilder buildOrFilter(BasicDBObject orFilters) {

		BoolFilterBuilder orFilter = FilterBuilders.boolFilter().cache(true);
		for (Map.Entry entry : orFilters.entrySet()) {
			orFilter.should(FilterBuilders.termFilter((String) entry.getKey(), entry.getValue()));
		}

		return orFilter;
	}

	public QueryBuilder addFacet(String... fields) {

		if (fields == null || fields.length < 1)
			return this;

		for (String field : fields)
			this.addFacet(field, true, null);

		return this;
	}

	public QueryBuilder addFacet(String field, boolean filtered, Integer size) {
		TermsFacetBuilder termfacet = FacetBuilders.termsFacet(field).field(field);
		termfacet.size(size != null && size != 0 ? size : this.facetSize);
		searchBuilder.addFacet(termfacet);
		return this;
	}

	public QueryBuilder setQuery(org.elasticsearch.index.query.QueryBuilder query) {
		searchBuilder.setQuery(query);
		return this;
	}

	public QueryBuilder addField(String field) {
		searchBuilder.addField(field);
		return this;
	}

	public QueryBuilder setIndices(String... indices) {
		searchBuilder.setIndices(indices);
		return this;
	}

	public QueryBuilder setTypes(String... types) {
		searchBuilder.setTypes(types);
		return this;
	}

	public QueryBuilder removeFilter(FilterBuilder filter) {
		filters.remove(filter);
		return this;
	}

	public QueryBuilder setExplain(boolean enabled) {
		searchBuilder.setExplain(enabled);
		debug = true;
		return this;
	}

	public SearchResponse get() {

		return this.get(null);
	}

	public QueryBuilder setQuery(String sql) {

		if (!StringUtils.isEmpty(sql)) {
			if (functionBuilder == null && !sql.contains(Global.SQL_ALL)) {
				searchBuilder.setQuery(QueryBuilders.queryString(sql));
			} else {
				JiangcoderScoreQueryBuilder functionQuery = null;
				if (sql.contains(Global.SQL_ALL)) {
					
					if(isScoreSort()){
						functionQuery = new JiangcoderScoreQueryBuilder(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), andFilterBuilder)).add(functionBuilder.lang("native")).boostMode(
								"sum");
					}else{
						functionQuery = new JiangcoderScoreQueryBuilder(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery().boost(0), andFilterBuilder)).add(functionBuilder.lang("native")).boostMode(
								"sum");
					}
				} else {
					
					if(isScoreSort()){
						functionQuery = new JiangcoderScoreQueryBuilder(QueryBuilders.filteredQuery(QueryBuilders.queryString(sql), andFilterBuilder)).add(functionBuilder.lang("native")).boostMode(
								"sum");
					}else{
						functionQuery = new JiangcoderScoreQueryBuilder(QueryBuilders.filteredQuery(QueryBuilders.queryString(sql).boost(0), andFilterBuilder)).add(functionBuilder.lang("native")).boostMode(
								"sum");
					}
					
					
				}
				if (fake != null) {
					functionQuery.fake(fake);
				}
				if (!StringUtils.isEmpty(regionId)) {
					functionQuery.regionId(regionId);
				}
				if (!StringUtils.isEmpty(cityId)) {
					functionQuery.cityId(cityId);
				}
				if (!StringUtils.isEmpty(catIds)) {
					functionQuery.catIds(catIds);
				}
				
//				functionQuery.gomeSortAsc(gomeSortAsc);
//				functionQuery.gomeSortType(gomeSortType);
				
				
				searchBuilder.setQuery(functionQuery);
			}
		}
		return this;
	}
	public List<Facet> getFacet(String sql,String alias,String type){
		BoolFilterBuilder filterBuilder= FilterBuilders.boolFilter().cache(true);
		if (filters != null && filters.size() > 0) {
			for (FilterBuilder filter : filters) {
				filterBuilder.must(filter);
			}
	   }
		TermsFacetBuilder termfacet = FacetBuilders.termsFacet("categories").field("categories");
		termfacet.size(200);
		
		
		SearchRequestBuilder  builder=ESClientUtils.getTransportClient().prepareSearch().setIndices(alias).setTypes(type).setPreference(Global.ES_PREFERENCE.trim())
				.setQuery(QueryBuilders.filteredQuery(QueryBuilders.queryString(sql), filterBuilder)).setSize(0).addFacet(termfacet);
		
//		logger.info("getfacet dsl [{}]",new Object[]{builder.toString()});
		
		SearchResponse rep=builder.get();
		
		return rep.getFacets().facets();
	}
	
	
	/**
	 * @Title: isScoreSort
	 * @Description: 是否是国美综合排序
	 * @param @return    设定文件
	 * @return boolean    返回类型
	 * @throws
	 */
	private boolean isScoreSort(){
		return (this.gomeSortType==0)?true:false;
	}
	
	public SearchResponse get(String query) {

		if (filters != null && filters.size() > 0) {
				for (FilterBuilder filter : filters) {
					andFilterBuilder.must(filter);
				}
		}
		
		setQuery(query);

		if (returnFields == null) {
			searchBuilder.addFields(commFields);
		}

		if (debug) {
			logger.info("search DSL: [{}]", new Object[] { searchBuilder.toString() });
			debug = false;
		}
		else{
			//本地测试，提交要注释
			System.out.println(searchBuilder.toString());
			logger.info("search DSL: [{}]", new Object[] { searchBuilder.toString() });
		}
		
		SearchResponse rep=searchBuilder.get();
		if(rep!=null&&rep.getHits().getTotalHits()>100000){
			logger.info("huge hit result DSL: [{}]", new Object[] { searchBuilder.toString() });
		}
		return rep;
	}
	public String  getSearchBuilder(){
		
		return searchBuilder.toString();
	}

}
