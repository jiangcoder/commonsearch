package com.jiangcoder.search.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.replication.ReplicationType;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.jiangcoder.search.es.ChildIndexStructure;
import com.jiangcoder.search.es.ESClientUtils;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

public class ESIndex {
	private static Logger logger = Logger.getLogger(ESIndex.class.getName());
	private BulkRequestBuilder bulkRequest = null;
	private BulkRequestBuilder bulkRequestForDelete = null;
	private List<String> toDeleteProductIds = new ArrayList<String>();
	private BulkRequestBuilder bulkRequestForFieldUpdate = null;
	public static String indexType = "productType";
	public static String indexName = "product";
	private Client client = ESClientUtils.getTransportClient();
	public Object lock;
	public Object lockForBulkDelete;
	public Object lockForBulkFieldUpdate;
	
	/**
	 * 批量建索引
	 * @param oDocs
	 */
	public void createIndex(BasicDBList oDocs) {
		try {
			BasicDBList bList=new BasicDBList();
			for(Object obj:oDocs){
				BasicDBObject product =(BasicDBObject)obj;
				BasicDBObject ProductDoc=new BasicDBObject();
				ProductDoc.put("id", product.get("id"));
				ProductDoc.put("productTag", product.get("productTag"));
				ProductDoc.put("salesVolume",product.get("salesVolume"));
				BasicDBList skus=(BasicDBList)product.get("skus");
				BasicDBList skusDoc=new BasicDBList();
				for(Object skuObj:skus){
					BasicDBObject sku=(BasicDBObject)skuObj;
					BasicDBObject s=new BasicDBObject();
					s.put("skuId", sku.get("id"));
					s.put("skuNo", sku.get("skuNo"));
					s.put("skuState",sku.get("state"));
					String productId = product.getString("id");
					String skuId = sku.getString("id");
					String skuName = sku.getString("name");
					s.put(ChildIndexStructure.FIELD_SKUID, skuId);
					s.put(ChildIndexStructure.FIELD_PRODID, productId);
					StringBuilder titleBuilder = new StringBuilder();
					titleBuilder.append(skuName);
					titleBuilder.append(" ");
					String promoTag = s.getString("promoTag", " ");
					titleBuilder.append(promoTag).append(" ");
					//活动属性增加
					// brand 及其拼音加入 title catagorybrand
					String brand = product.getString("brand");
					 // 店铺添加sortNo
					// 添加店铺子段 店铺名称加入title
					BasicDBObject oShop = (BasicDBObject) product.get("shop");
					if (oShop != null) {
						s.put(ChildIndexStructure.FIELD_SHOPID, oShop.getString("id"));
						s.put(ChildIndexStructure.FIELD_SHOPNAM, oShop.getString("name"));
						s.put(ChildIndexStructure.FIELD_SHOPTYPE, Integer.parseInt(oShop.getString("type")));
						titleBuilder.append(oShop.getString("name"));
						titleBuilder.append(" ");
					}
					String skuNo = sku.getString("skuNo");
					s.put(ChildIndexStructure.FIELD_SKUNO, skuNo);
					s.put(ChildIndexStructure.FIELD_SKUSTATE, sku.getInt("state", 0));
					//这里要特别注意，这个计算价格的过程中，隐藏
					s.put(ChildIndexStructure.FIELD_PRICE, 999);
					s.put(ChildIndexStructure.FIELD_PRODSTATE, product.getInt("state", 0));
					ArrayList<String> tokens = new TokenUtil().parseTokensGome(titleBuilder.toString(), true); 
					s.put(ChildIndexStructure.FIELD_TITLE, tokens.get(0));
					s.put(ChildIndexStructure.FIELD_NGRAM, tokens.size() == 2 ? tokens.get(1) : "");
					skusDoc.add(s);
				}
				ProductDoc.put("sku", skusDoc);
				bList.add(ProductDoc);
			}
			List<IndexRequestBuilder> indexRequestBuilders = buildNestedBulkIndexParam(bList, Global.NESTED_INDEXNAME, Global.nestedIndexType, client);
			if (bulkRequest == null) {
				bulkRequest = client.prepareBulk();
			}
			for (IndexRequestBuilder indexBuilder : indexRequestBuilders) {
				bulkRequest.add(indexBuilder);
			}
			if (bulkRequest.numberOfActions() >= 1) {
				executeIndex();
				logger.info("bulkRequest bigger than 1 ,thencommit!");
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("add Bulk fail", e);
		}
	}
	
	 /**
	 * @Title: createParentChildDocOrNestedDocAndMaybeAddScheduleJob
	 * @Description: 创建parent或者nested的doc对象
	 * @param @param productId
	 * @param @param product
	 * @param @param parent
	 * @param @param allLegalChildSkus
	 * @param @param clearData
	 * @param @param productFacets
	 * @param @param jedis
	 * @param @param isParentChildNotNested
	 * @param @return    设定文件
	 * @return BasicDBObject    返回类型
	 * @throws
	 */
	
	
	public List<IndexRequestBuilder> buildNestedBulkIndexParam(BasicDBList docs,String nestedIndexName,String nestedType,Client client) {
		List<IndexRequestBuilder> indexBuiders = new ArrayList<IndexRequestBuilder>();
		for (Object docData : docs) {
			XContentBuilder xcb = buildIndexParam((BasicDBObject)docData);
			if (xcb != null) {
				String composedProductId =  ((BasicDBObject)docData).getString("id");
				indexBuiders.add(
						client.prepareIndex(nestedIndexName, nestedType).setId(composedProductId).setSource(xcb)
						.setConsistencyLevel(WriteConsistencyLevel.ALL));
				logger.info("add bulk composedProductId="+composedProductId);
			}}
		return indexBuiders;
	}
	/**
	 * @Title: buildIndexParam
	 */
	public XContentBuilder buildIndexParam(BasicDBObject docData) {
		XContentBuilder xcb = null;
		try {
			xcb = XContentFactory.jsonBuilder().prettyPrint().startObject();
			for (Entry<String, Object> entry : docData.entrySet()) {
				xcb.field(entry.getKey(), entry.getValue());
			}
			xcb.endObject();
		} catch (Exception e) {
			logger.error("build BulkIndex skuid :" + docData.getString(ChildIndexStructure.FIELD_SKUID) + " Fail.", e);
		}
		return xcb;
	}
	
	/**
	 * 单个进行索引处理
	 */
	public void solveDoc(List<BasicDBObject> docs) {
		for (BasicDBObject docData : docs) {
			XContentBuilder xcb = buildIndexParam(docData);
			if (xcb != null) {
				String skuId = docData.getString(ChildIndexStructure.FIELD_SKUID);
				client.prepareIndex(indexName, indexType, skuId).setSource(xcb).execute().actionGet();
			}
		}
	}
	public void executeDeleteBulk() {
		if (bulkRequestForDelete != null && bulkRequestForDelete.numberOfActions() > 0) {
			BulkResponse bulkReponse = bulkRequestForDelete.setReplicationType(ReplicationType.SYNC)
					.setConsistencyLevel(WriteConsistencyLevel.ALL).execute().actionGet();
			if (!bulkReponse.hasFailures()) {
				logger.info("==========bulkRequestForDelete successfully,ids {}"+
						StringUtils.join(toDeleteProductIds, "|"));
			} else {
				logger.info("==========bulkRequestForDelete fail,one delete fail will not make others fail,just log"
						+ bulkReponse.buildFailureMessage());
			}
		}
	}

	/**
	 * 批量索引，被定时线程周期性调度
	 */
	public void executeIndex() {
		logger.info(bulkRequest.numberOfActions()+"=========");
		if (bulkRequest != null && bulkRequest.numberOfActions() > 0) {
			logger.info("-------------------------");
			BulkResponse bulkReponse = bulkRequest.setReplicationType(ReplicationType.SYNC)
					.setConsistencyLevel(WriteConsistencyLevel.ALL).execute().actionGet();
			if (!bulkReponse.hasFailures()) {
				
			}else {
				logger.info(bulkReponse.buildFailureMessage());
			}
			bulkRequest = null;
		}
	}
	/**
	 * 
	 * @Title: deleteDocByProductId @Description: 根据产品id删除数据，注意索引和缓存都需要处理。
	 * 注意：es批量删除数据时，不会因为一条数据的删除失败，而导致其他数据的删除失败 @param @param
	 * productId @param @param jedis 设定文件 @return void 返回类型 @throws
	 */
	protected void deleteDocByProductId(String productId) {
		logger.info("prepare to delete productId={}"+ productId);
		try {
				logger.info("prodcutId not in ESDict,productId is {}"+ new Object[] { productId });
				// 这种先不删除redis,避免es和缓存库不一致;
				List<String> list = getAllComposedProductIdsByProductId(productId);
				if (!EmptyUtil.isEmpty(list)) {
					for (String composedProductId : list) {
						if (StringUtils.isNotEmpty(composedProductId)) {
							DeleteRequestBuilder drb = client.prepareDelete(Global.NESTED_INDEXNAME, Global.nestedIndexType,
									composedProductId);
							addDeleteRequestToBulk(drb);
						}}
					}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("prepare to delete productId {} fail,e is {}"+new Object[] { productId, e.toString() });
		}
	}

	/**
	 * 
	 */
	protected void addDeleteRequestToBulk(DeleteRequestBuilder drb) {
		synchronized (lockForBulkDelete) {
			if (bulkRequestForDelete == null) {
				bulkRequestForDelete = client.prepareBulk();
			}
			bulkRequestForDelete.add(drb);
		}
	}
	public void executeFieldUpdateBulk() {

		if (bulkRequestForFieldUpdate != null && bulkRequestForFieldUpdate.numberOfActions() > 0) {
			BulkResponse bulkReponse = bulkRequestForFieldUpdate.setReplicationType(ReplicationType.SYNC)
					.setConsistencyLevel(WriteConsistencyLevel.ALL).execute().actionGet();
			if (!bulkReponse.hasFailures()) {
				logger.info("==========executeFieldUpdateBulk successful");
			} else {
				logger.info("==========executeFieldUpdateBulk fail" + bulkReponse.buildFailureMessage());
			}
			bulkRequestForFieldUpdate = null;
		}
	}

	public List<String> getAllComposedProductIdsByProductId(String productId) {
		List<String> resultList = new ArrayList<String>();
		try {
			NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("sku",
					QueryBuilders.filteredQuery(null, FilterBuilders.termFilter("sku.productId", productId)));
			SearchRequestBuilder builder = client.prepareSearch(Global.NESTED_INDEXNAME).setTypes(Global.nestedIndexType)
					.setQuery(nestedQuery).setFrom(0).setSize(48);
			builder.execute();
			SearchResponse searchResponse = builder.get();

			if (searchResponse != null) {
				SearchHit[] hits = searchResponse.getHits().hits();
				if (hits != null) {
					for (SearchHit hit : hits) {
						resultList.add(hit.getId());
					}
				}
			}

		} catch (Exception e) {
		}
		return resultList;
	}

	/**
	 * 使用同步锁lockForBulkFieldUpdate将操作添加到
	 */
	protected void addUpdateRequestToBulk(UpdateRequestBuilder urb) {
		if (bulkRequestForFieldUpdate == null) {
			bulkRequestForFieldUpdate = client.prepareBulk();
		}
		bulkRequestForFieldUpdate.add(urb);
		if (bulkRequestForFieldUpdate.numberOfActions() > 1000) {
			this.executeFieldUpdateBulk();
			logger.info("executeFieldUpdateBulk bigger than 1000 ,thencommit!");
		}
	}

}
