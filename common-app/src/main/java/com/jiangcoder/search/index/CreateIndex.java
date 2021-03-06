package com.jiangcoder.search.index;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.AndFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.MatchQueryBuilder.ZeroTermsQuery;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.facet.filter.FilterFacetBuilder;
import org.elasticsearch.search.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest.Suggestion.Entry.Option;
import org.elasticsearch.search.suggest.term.TermSuggestionBuilder;

import com.jiangcoder.search.es.ESClientUtils;

public class CreateIndex {
	public static void main(String[] args) {
	}
	//@Test
	public  void CreateIndex() {
		Client client=ESClientUtils.getTransportClient();
		IndexResponse response =client.prepareIndex("product","productType","9100003735").setSource("{\"id\":\"9100003735\",\"productType\":0,\"name\":\"55英寸4K超高清 智能 八核 网络 内置WiFi 窄边 LED 液晶电视\",\"state\":4,\"startDate\":1459353600000,\"endDate\":1893427199000,\"productTag\":1,\"evaluateCount\":0,\"brand\":\"chuangwei\",\"iismerchant\":2,\"price\":318.0,\"category\":100030}").execute().actionGet();
	}
	//@Test
	public  void DeleteIndex(){
		Client client=ESClientUtils.getTransportClient();
		DeleteResponse response=client.prepareDelete("product","sku","AVPsEeeMV7koqLHyM2_n").execute().actionGet();
	}
	//@Test
	public  void SearchIndex(){
		Client client=ESClientUtils.getTransportClient();
		SearchResponse response=client.prepareSearch("product2").addFields("title","_source").execute().actionGet();
		for(SearchHit hit:response.getHits()){
			System.out.println(hit.getId());
			if(hit.getFields().containsKey("title")){
				System.out.println("field.title: "+hit.getFields().get("title").getValue());
			}
			System.out.println("source.title:"+hit.getSource().get("title"));
		}
	}
	//@Test
	public void SearchQueryBuilder(){
		Client client=ESClientUtils.getTransportClient();
		QueryBuilder queryBuilder=QueryBuilders.disMaxQuery().add(QueryBuilders.termQuery("title", "master22 elasticSearch")).add(QueryBuilders.prefixQuery("title","master"));
		System.out.println(queryBuilder.toString());
		SearchResponse response=client.prepareSearch("library").setQuery(queryBuilder).execute().actionGet();
		for(SearchHit hit:response.getHits()){
			System.out.println(hit.getId());
			if(hit.getFields().containsKey("title")){
				System.out.println("field.title: "+hit.getFields().get("title").getValue());
			}
			System.out.println("source.title:"+hit.getSource().get("title"));
		}
	}
	//@Test
	public void SearchAll(){
		Client client=ESClientUtils.getTransportClient();
		QueryBuilder queryBuilder=QueryBuilders.matchAllQuery().boost(11f).normsField("title");
		SearchResponse response=client.prepareSearch("library").setQuery(queryBuilder).execute().actionGet();
		for(SearchHit hit:response.getHits()){
			System.out.println(hit.getId());
			if(hit.getFields().containsKey("title")){
				System.out.println("field.title: "+hit.getFields().get("title").getValue());
			}
			System.out.println("source.title:"+hit.getSource().get("title"));
		}
	}
	//@Test 
	//match search
	public void MatchSearch(){
		Client client=ESClientUtils.getTransportClient();
		QueryBuilder queryBuilder=QueryBuilders.matchQuery("title","elasticsearch").operator(org.elasticsearch.index.query.MatchQueryBuilder.Operator.AND).zeroTermsQuery(ZeroTermsQuery.ALL);
		SearchResponse response=client.prepareSearch("library").setQuery(queryBuilder).execute().actionGet();
		for(SearchHit hit:response.getHits()){
			System.out.println(hit.getId());
			if(hit.getFields().containsKey("title")){
				System.out.println("field.title: "+hit.getFields().get("title").getValue());
			}
			System.out.println("source.title:"+hit.getSource().get("title"));
		}
	}
	//@Test
	public void PagingSearch(){
		Client client=ESClientUtils.getTransportClient();
		SearchResponse response=client.prepareSearch("library").setQuery(QueryBuilders.matchAllQuery()).setFrom(0).setSize(20).execute().actionGet();
		for(SearchHit hit:response.getHits()){
			System.out.println(hit.getId());
			if(hit.getFields().containsKey("title")){
				System.out.println("field.title: "+hit.getFields().get("title").getValue());
			}
			System.out.println("source.title:"+hit.getSource().get("title"));
		}
	}
	//@Test
	public void SortSearch(){
		Client client=ESClientUtils.getTransportClient();
		SearchResponse response=client.prepareSearch("library").setQuery(QueryBuilders.matchAllQuery()).addSort(SortBuilders.fieldSort("title"))
								.addSort("_score",SortOrder.DESC).execute().actionGet();
		for(SearchHit hit:response.getHits()){
			System.out.println(hit.getId());
			if(hit.getFields().containsKey("title")){
				System.out.println("field.title: "+hit.getFields().get("title").getValue());
			}
			System.out.println("source.title:"+hit.getSource().get("title"));
		}
	}
	//@Test
	public void FilterSearch(){
		Client client=ESClientUtils.getTransportClient();
		AndFilterBuilder filterBuilder=FilterBuilders.andFilter(FilterBuilders.existsFilter("title").filterName("love"),FilterBuilders.termFilter("title", "jiangtao"));//
		SearchResponse response=client.prepareSearch("library").setPostFilter(filterBuilder).execute().actionGet();
		for(SearchHit hit:response.getHits()){
			System.out.println(hit.getId());
			if(hit.getFields().containsKey("title")){
				System.out.println("field.title: "+hit.getFields().get("title").getValue());
			}
			System.out.println("source.title:"+hit.getSource().get("title"));
		}
	}
	//@Test
//	public void FacetBuilderSearch(){
//		Client client=ESClientUtils.getTransportClient();
//		@SuppressWarnings("deprecation")
//		FilterFacetBuilder facetBuilder=FacetBuilders.filterFacet("test").filter(FilterBuilders.termFilter("title", "elastic"));
//		SearchResponse response=client.prepareSearch("library").addFacet(facetBuilder).execute().actionGet();
//		for(SearchHit hit:response.getHits()){
//			System.out.println(hit.getId());
//			if(hit.getFields().containsKey("title")){
//				System.out.println("field.title: "+hit.getFields().get("title").getValue());
//			}
//			System.out.println("source.title:"+hit.getSource().get("title"));
//		}
//	}
	//@Test
	public void HighLightSearch(){
		Client client=ESClientUtils.getTransportClient();
		SearchResponse response=client.prepareSearch("library").addHighlightedField("title")
				.setQuery(QueryBuilders.termQuery("title","love"))
				.setHighlighterPreTags("<1>","<2>")
				.setHighlighterPostTags("</1>","</2>").execute().actionGet();
		for(SearchHit hit:response.getHits().getHits()){
			HighlightField hField=hit.getHighlightFields().get("title");
			for(org.elasticsearch.common.text.Text t:hField.fragments()){
				System.out.println(t.toString());
			}
		}
	}
	//@Test
	public void SuggestSearch(){  //todo
		Client client=ESClientUtils.getTransportClient();
		SearchResponse response=client.prepareSearch("library").setQuery(QueryBuilders.matchAllQuery())
				.addSuggestion(new TermSuggestionBuilder("title").text("love").field("_all")).execute().actionGet();
		for(org.elasticsearch.search.suggest.Suggest.Suggestion.Entry<? extends Option> entry:response.getSuggest().getSuggestion("title").getEntries()){
			System.out.println("Check for: "+entry.getText()+".Options");
			for(Option option:entry.getOptions()){
				System.out.println("\t"+option.getText());
			}
		}
	}
	
}
