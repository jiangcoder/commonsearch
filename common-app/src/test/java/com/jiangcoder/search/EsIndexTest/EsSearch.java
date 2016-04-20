package com.jiangcoder.search.EsIndexTest;

import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;

import com.jiangcoder.search.es.ESClientUtils;
import com.jiangcoder.search.index.QueryBuilder;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

public class EsSearch {
	private final static String PARAM_REGION = "regionId";
	private final static String PARAM_CITY = "sCityId";
	private static Client client = ESClientUtils.getTransportClient();
	public static void main(String[] args) {
		QueryBuilder queryBuilder = new QueryBuilder();
		String cityId = "11010200";
	//	queryBuilder.setScriptParm(PARAM_REGION, cityId);
		//queryBuilder.regionId(cityId);
		queryBuilder.setScriptParm(PARAM_CITY, cityId);
		//queryBuilder.cityId(cityId);
		queryBuilder.setScriptParm("category","cat1000070");
		queryBuilder.setScriptParm("price",412.0d);
		queryBuilder.setIndices("pro").setTypes("productType");
		queryBuilder.setQuery("name:55高清");
		//queryBuilder.setExplain(true);
		BoolFilterBuilder categoriesFilter = FilterBuilders.boolFilter();
			String cateId = "cat1000070";
				categoriesFilter.should(FilterBuilders.termFilter(com.jiangcoder.search.es.ChildIndexStructure.FIELD_CATS,
						cateId).cache(true));
		queryBuilder.addFilter(categoriesFilter);
		queryBuilder.addFilter((FilterBuilders.termFilter("state",4 )));
		queryBuilder.setQuery("(name:(高清)^2)");
		//queryBuilder.setQuery("(title:(高清))");
		queryBuilder.from(0);
		queryBuilder.size(10);
		//queryBuilder.setQuery(QueryBuilders.boolQuery().should(QueryBuilders.termsQuery("evaluateCount", new int[]{0})));
		//queryBuilder.setScriptParm("gomeSortType", 1);
		//queryBuilder.setScriptParm("gomeSortAsc", false);
		SearchResponse reponse = queryBuilder.get();
		System.out.println(reponse);
		SearchHits hits = reponse.getHits();
		if (hits.getHits().length > 0) {
			BasicDBList dataList = new BasicDBList();
			for (SearchHit hit : hits.getHits()) {
					System.out.println(hit.getFields().size());
					Map<String, SearchHitField> map=hit.getFields();
					for(String s:map.keySet()){
						System.out.println(s+":"+map.get(s).getValue().toString());
					}
				BasicDBObject dbo = new BasicDBObject();
				dataList.add(dbo);
			}
		} else {
		}
	}
}
