package com.jiangcoder.search.index;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;
import org.junit.Test;

import com.jiangcoder.search.es.ESClientUtils;
import com.jiangcoder.search.es.ParentChildOrNestedQueryBuilder;
import com.mongodb.BasicDBObject;

public class ESSearch {
	public static void main(String[] args) {
		
		
	}
	@Test
	public void Search() throws Exception{
		Client client=ESClientUtils.getTransportClient();
		ParentChildOrNestedQueryBuilder queryBuilder = new ParentChildOrNestedQueryBuilder().setIndices("product2").setTypes("productType");
		BasicDBObject oReq=new BasicDBObject();
		SearchResponse response = getQueryResponse(oReq,queryBuilder);
	}
	@Test
	public  void SearchIndex(){
		Client client=ESClientUtils.getTransportClient();
		SearchResponse response=client.prepareSearch("product2").execute().actionGet();
		for(SearchHit hit:response.getHits()){
			System.out.println(hit.getId());
			System.out.println(response.getHeader("title"));
			if(hit.getFields().containsKey("title")){
				System.out.println("field.title: "+hit.getFields().get("title").getValue());
			}
			System.out.println("source.title:"+hit.getSource().get("title"));
		}
	}
	public SearchResponse getQueryResponse(BasicDBObject oReq,ParentChildOrNestedQueryBuilder queryBuilder)
			throws Exception {
		return null;
	}
}