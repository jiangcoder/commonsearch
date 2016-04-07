//package com.jiangcoder.search.es;
//
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//
//import org.apache.commons.lang3.StringUtils;
//import org.elasticsearch.action.search.SearchResponse;
//import org.elasticsearch.index.query.BoolFilterBuilder;
//import org.elasticsearch.index.query.FilterBuilders;
//import org.elasticsearch.search.aggregations.Aggregation;
//import org.elasticsearch.search.aggregations.bucket.nested.Nested;
//import org.elasticsearch.search.aggregations.bucket.terms.Terms;
//import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
//import org.elasticsearch.search.sort.SortOrder;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.jiangcoder.search.index.EmptyUtil;
//import com.mongodb.BasicDBList;
//import com.mongodb.BasicDBObject;
//
//import redis.clients.jedis.JedisCluster;
//
//public class CommonSearch {
//	private Logger logger = LoggerFactory.getLogger(CommonSearch.class);
//	public BasicDBObject search(BasicDBObject oRequire, JedisCluster jedis) {
//
//		BasicDBObject oResult = new BasicDBObject();
//		// 记录检索请求获取时间
//		long start = System.currentTimeMillis();
//		// oReq包含检索请求信息的对象
//		BasicDBObject oReq = (BasicDBObject) oRequire;
//
//		String ip = oReq.getString("ip", "").trim();
//
//		String indexName = Global.INDEX_NAME;
//		// 初始化QueryBuilder
//		QueryBuilder queryBuilder = new QueryBuilder(
//				esClientUtils, categoryDict);
//		queryBuilder.setIndices(indexName).setTypes(INDEXTYPE);
//		// 区域id
//		String cityId = oReq.getString("regionId", "11010200");
//		if (cityId.equalsIgnoreCase("0") || StringUtil.isEmpty(cityId)) {
//			cityId = "11010200";
//		}
//
//		queryBuilder.setScriptParm(PARAMREGION, cityId);
//		queryBuilder.regionId(cityId);
//
//		Map<String, String> cityIndex = blackDragonList.getCityIndex();
//		cityId = cityIndex.get(cityId);
//		if (StringUtils.isEmpty(cityId)) {
//			cityId = "1";
//		}
//
//		queryBuilder.extraParam(cityId);
//		queryBuilder.setScriptParm(PARAMCITY, cityId);
//		queryBuilder.cityId(cityId);
//
//		// 检索请求catId也需要变化
//		String catId = oReq.getString("catId");
//
//		boolean hasCategory = EmptyUtil.notEmpty(catId);
//		boolean failed = processBuleCouponSearch(queryBuilder, oReq);
//		if (failed) {
//			return oResult;
//		}
//
//
//		long startGetfacet = 0, endGetFacets = 0, startReadProduct = 0, endReadProduct = 0;
//
//		try {
//			parseQuery(queryBuilder, oReq, oResult, hasCategory, catId);
//			Map<String, BasicDBList> selectReqFacetMap = new HashMap<String, BasicDBList>();
//			Map<String, BasicDBList> reqFacetMap = new HashMap<String, BasicDBList>();
//			String reqFacets = oReq.getString("facets");
//			//Test	reqFacets = "11I7";
//			parseReqFacets(queryBuilder, reqFacets, reqFacetMap, selectReqFacetMap, jedis);
//			// 获取elasticsearch的检索结果
//			SearchResponse response = getBuleCouponSearchQueryResponse(
//					queryBuilder, oReq, oResult, true);// es
//
//			BasicDBList facets = new BasicDBList();
//			BasicDBList categories = new BasicDBList();
//			// 初始化分类结果
//			Map<String, Integer> categoryMatches = new HashMap<String, Integer>();
//
//			processAgg(response, categoryMatches, facets, jedis);
//			// 获取解析截至时间
//			endGetFacets = System.currentTimeMillis();
//
//			categories = readCategories(categoryMatches);
//
//			oResult.append("searchTime", response.getTookInMillis());
//			long totalHit = response.getHits().totalHits();
//			oResult.append("totalCount", totalHit);
//
//			
//			oResult.append("categories", categories);
//			oResult.append("facets", facets);
//			startReadProduct = System.currentTimeMillis();
//			endReadProduct = System.currentTimeMillis();
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		try {
//
//			long time = System.currentTimeMillis() - start;
//			int pageSize = oReq.getInt("pageSize", 0);
//			int pageNumber = oReq.getInt("pageNumber", 0);
//			if (time > 0 && time <= 100) {
//				logger.info(
//						"L&s={}ms,c={},q={},m={}&qt={}&tkt={}&sl={}&fsl={}&pt={}{}#hit={}*reqf={}*searchCount={}*ip={}*pnum={}*psize={}",
//						new Object[] { time, oReq.get("catId"), oReq.get(BULECOUPONID),	oReq.get("mobile"), oResult.get("queryTime"),	oResult.get("tkt"), oResult.get("searchLevel"),
//								(endGetFacets - startGetfacet),(endReadProduct - startReadProduct),	oReq.get("t"), oResult.get("totalCount"),	oReq.getString("facets"),
//								count.incrementAndGet(), ip, pageNumber,	pageSize });
//			}
//			if (time > 100) {
//				logger.info(
//						"O&{}#{}s={}ms,c={},q={},m={}&qt={}&tqt={}&tkt={}&sl={}&ft={}&pt={}{}#hit={}*reqf={}*searchCount={}*ip={}*pnum={}*psize={}",
//						new Object[] {oResult.getInt("queryTime") > 100 ? "o" : "l",time > 3000 && time < 6000 ? "3"	: time >= 6000 ? "6" : "n", time,	oReq.get("catId"), oReq.get(BULECOUPONID), oReq.get("mobile"),
//								oResult.get("queryTime"),oResult.get("TookInMillis"),oResult.get("tkt"), oResult.get("searchLevel"),	(endGetFacets - startGetfacet),	(endReadProduct - startReadProduct),
//								oReq.get("t"), oResult.get("totalCount"),	oReq.getString("facets"),	count.incrementAndGet(), ip, pageNumber,	pageSize });
//			}
//		} catch (Exception e) {
//			logger.error("print time log error!,c={}&q={}",
//					new Object[] { oReq.get("catId "), oReq.get(BULECOUPONID) });
//		}
//		return oResult;
//	}
//	
//	public void parseReqFacets(QueryBuilder queryBuilder, String reqFacets, Map<String, BasicDBList> reqFacetMap, Map<String, BasicDBList> selectReqFacetMap,
//			JedisCluster jedis) {
//
//		if (EmptyUtil.notEmpty(reqFacets)) {
//			int length = reqFacets.length();
//			length = length / 4 * 4;// 将页面传入的facet字符串长度转化为4的倍数
//			for (int i = 0; i < length; i = i + 4) {
//				String facetId = reqFacets.substring(i, i + 4);
//				String facetInfoId = esDict.get("f", facetId, jedis);
//				if (EmptyUtil.isEmpty(facetInfoId)) {
//					continue;
//				}
//				BasicDBList dbList = selectReqFacetMap.get(facetInfoId);
//				if (dbList == null) {
//					dbList = new BasicDBList();
//					selectReqFacetMap.put(facetInfoId, dbList);
//				}
//
//				facetInfoId = StringUtil.append("f.", facetInfoId);
//
//				// reqFacetMap {f.catId.facetInfoId:[facetId1,facetId2]}
//				// (当前请求facet)
//				BasicDBList facetIds = reqFacetMap.get(facetInfoId);
//				if (facetIds == null) {
//					facetIds = new BasicDBList();
//					reqFacetMap.put(facetInfoId, facetIds);
//				}
//				facetIds.add(facetId);
//				queryBuilder.addSkuOrFilter(facetInfoId, facetIds);
//				dbList.add(new BasicDBObject().append("id", facetId).append("value", esDict.get("fv", facetId, jedis)));
//			}
//		}
//	}
//
//	/**
//	 * @Title: readCategories
//	 * @Description: 读取分类信息，并设置高亮
//	 * @param @param defaultIds 热门分类的id集和
//	 * @param @param counts
//	 * @param @param pageName
//	 * @param @return 设定文件
//	 * @return BasicDBList 返回类型
//	 * @throws
//	 */
//	public BasicDBList readCategories(Map<String, Integer> counts) {
//		HashSet<String> parents = new HashSet<String>();
//		
//		HashSet<String> defaultNodes = new HashSet<String>();
//
//		for (Iterator<Entry<String, Integer>> i = counts.entrySet().iterator(); i
//				.hasNext();) {
//			Entry<String, Integer> entry = i.next();
//			String catId = entry.getKey();
//			defaultNodes.add(catId);
//			//获取三级分类
//			BasicDBObject oCat = categoryDict.getCategory(catId);
//			if (oCat == null) {
//				continue;
//			}
//			//获取二级分类
//			String secId =  oCat	.getString("parentId");
//			BasicDBObject oParent = categoryDict.getCategory(secId);
//			String firstId = null;
//			while (oParent != null && oParent.containsField("level")	&& oParent.getInt("level") >= 2) {
//				defaultNodes.add(secId);
//				//获取一级分类
//				firstId =  oParent.getString("parentId");
//				oParent = categoryDict.getCategory(firstId);
//			}
//			if (oParent == null || !oParent.containsField("childs")) {
//				continue;
//			}
//			defaultNodes.add(firstId);
//			parents.add(oParent.getString("catId"));
//		}
//		
//		BasicDBList results = new BasicDBList();
//		for (Iterator<String> i = parents.iterator(); i.hasNext();) {
//			String rootId = i.next();
//			//一级分类
//			BasicDBObject oCat = categoryDict.getCategory(rootId);
//			int level = oCat.getInt("level", 1);
//			if (level == 0)
//				continue;
//			//二级分类
//			BasicDBList list2 = (BasicDBList) oCat.get("childs");
//			if (list2 == null) {
//				continue;
//			}
//			String catId = oCat.getString("catId");
//
//			// level-1
//			BasicDBObject oResult = new BasicDBObject().append("count", 0);
//			oResult.append("id", catId);
//			oResult.append("name", oCat.getString("catName"));
//			oResult.append("url", oCat.getString("url", ""));
//			oResult.append("icon", oCat.getString("icon", ""));
//			oResult.append("isDefault", true);
//			BasicDBList childList2 = new BasicDBList();
//			oResult.append("childs", childList2);
//
//			// level-2
//			String id = null;
//			int size2 = list2.size();
//			for (int t = 0; t < size2; t++) {
//				BasicDBObject oItem2 = (BasicDBObject) list2.get(t);
//				id = oItem2.getString("catId");
//				if (!defaultNodes.contains(id)) {
//					continue;
//				}
//				long count = 0;
//				BasicDBObject o = categoryDict.getCategory(id);
//				if (o != null) {
//					oItem2.putAll((Map<String, Object>) o);
//				}
//				BasicDBObject oResult2 = new BasicDBObject();
//				childList2.add(oResult2);
//				oResult2.append("id", id);
//				oResult2.append("name", oItem2.getString("catName"));
//				oResult2.append("order", 0);
//				oResult2.append("isDefault", true);
//				// level-3
//				BasicDBList childList3 = new BasicDBList();
//				oResult2.append("childs", childList3);
//				BasicDBList list3 = (BasicDBList) oItem2.get("childs");
//				if (list3 == null) {
//					continue;
//				}
//				int size3 = list3.size();
//				double secondweight = 0;
//				for (int k = 0; k < size3; k++) {
//					BasicDBObject oItem3 = (BasicDBObject) list3.get(k);
//					id = oItem3.getString("catId");
//					if (!counts.containsKey(id)) {
//						continue;
//					}
//					count = counts.containsKey(id) ? counts.get(id) : 0;
//					BasicDBObject oResult3 = new BasicDBObject();
//					childList3.add(oResult3);
//					oResult3.append("id", id);
//					oResult3.append("order", 0);
//					oResult3.append("name", oItem3.getString("catName"));
//					oResult3.append("isDefault", true);
//					oResult3.put("weight", 0);
//					oResult3.append("count", count);
//					oResult2.append("count", oResult2.getInt("count", 0)
//							+ count);
//				}
//				oResult2.append("weight", secondweight);
//			}
//
//			if (childList2 != null && childList2.size() > 0) {
//				oResult.append(	"count",
//						oResult.getInt("count", 0)
//								+ ((BasicDBObject) childList2.get(0)).getInt(
//										"count", oResult.getInt("count")));
//			} else {
//				oResult.append("count", 0);
//				continue;
//			}
//
//			results.add(oResult);
//		}
//		return results;
//	}
//
//	/**
//	 * @Title: parseRespFacet
//	 * @Description: 解析结果的facet？
//	 * @param @param facetInfoId
//	 * @param @param facetInfos
//	 * @param @param field
//	 * @param @param facets
//	 * @param @param jedis 设定文件
//	 * @return void 返回类型
//	 * @throws
//	 */
//	private void parseRespFacet(String facetInfoId, Terms termAgg,
//			BasicDBList facets, JedisCluster jedis) {
//		if (termAgg.getBuckets() == null || termAgg.getBuckets().size() <= 0) {
//			return;
//		}
//
//		BasicDBObject facetInfo = new BasicDBObject();// 现只组装品牌的facetInfo
//		facetInfo.append("id", "2");
//		facetInfo.append("count", 0);
//		facetInfo.append("type", 1);
//		facetInfo.append("label", "品牌");
//		facetInfo.append("index", 1);
//
//		BasicDBList facetValues = new BasicDBList();
//		facetInfo.append("items", facetValues);
//
//		List<Bucket> bucketList = termAgg.getBuckets();
//		int index = 0;
//		for (Bucket bucket : bucketList) {
//			String facetId = bucket.getKey();
//
//			if (EmptyUtil.isEmpty(facetId)) {
//				continue;
//			}
//			String prefix = esDict.get(ChildIndexStructure.FIELD_DY_PREFIX_FP,
//					facetId, jedis);
//			BasicDBObject facet = new BasicDBObject()
//					.append("id", facetId)
//					.append("value",
//							esDict.get(ChildIndexStructure.FIELD_DY_PREFIX_FV,
//									facetId, jedis))
//					.append("count", bucket.getDocCount());
//
//			if (prefix != null) {
//				facet.append("prefix", prefix);
//			}
//			facet.append("index", index);
//			index++;
//			facetValues.add(facet);
//		}
//		facets.add(facetInfo);
//	}
//
//	/**
//	 * @Title: processAgg
//	 * @Description: agg解析处理
//	 * @param @param response
//	 * @param @param facetInfos
//	 * @param @param categoryMatches
//	 * @param @param facets
//	 * @param @param jedis 设定文件
//	 * @return void 返回类型
//	 * @throws
//	 */
//	private void processAgg(SearchResponse response,
//			Map<String, Integer> categoryMatches, BasicDBList facets,
//			JedisCluster jedis) {
//
//		Nested skuAgg = response.getAggregations().get("sku");
//
//		List<Aggregation> list = skuAgg.getAggregations().asList();
//
//		for (Aggregation agg : list) {
//			Terms termAgg = (Terms) agg;
//			String aggName = termAgg.getName();
//
//			if (termAgg.getBuckets().size() == 0)
//				continue;
//
//			if (aggName.startsWith("f.")) {
//				parseRespFacet(aggName, termAgg, facets, jedis);
//
//			} else {
//				for (Terms.Bucket entry : termAgg.getBuckets()) {
//					String catId = entry.getKey();
//					BasicDBObject category = categoryDict.getCategory(catId);
//					if (EmptyUtil.isEmpty(catId)
//							|| EmptyUtil.isEmpty(category)
//							|| !"gome".equalsIgnoreCase(category
//									.getString("catalog"))) {
//						continue;
//					}
//					// 添加一个分类
//					categoryMatches.put(catId, (int) entry.getDocCount());
//				}
//			}
//		}
//	}
//
//	public void parseQuery(QueryBuilder queryBuilder,
//			BasicDBObject oReq, BasicDBObject oResult, boolean hasCategory,
//			String categoryId) {
//
//		if (hasCategory) {
//			// 如果改成多个分类，分类过滤的代码需要重写？还是搜索页面，根本不会在这个地方设置
//			queryBuilder.addSkuTermFilter(ChildIndexStructure.FIELD_CATS,
//					categoryId);
//		}
//
//		String price = oReq.getString("price", null);
//		if (!EmptyUtil.isEmpty(price) && !StringUtil.isEmpty(price.trim())) {
//			try {
//				queryBuilder.addSkuRangeFilter(ChildIndexStructure.FIELD_PRICE,
//						price);
//			} catch (Exception e) {
//				logger.error(
//						"addSkuRangeFilter error:[{}],query:{},catId:{},price:{}",
//						e, oReq.toString(), categoryId, price);
//				e.printStackTrace();
//			}
//		}
//
//		queryBuilder.addSkuTermFilter(ChildIndexStructure.FIELD_SKUSTATE, 4);
//		queryBuilder.addSkuTermFilter(ChildIndexStructure.FIELD_PRODSTATE, 4);
//		queryBuilder.addFacet(CATALOG);// Agg for category
//		queryBuilder.addFacet("f.2");// Agg for facetInfo
//
//		BasicDBList fakeFacets = (BasicDBList) oReq.get("fakeFacets");// 请求里面的facet条件
//		if (EmptyUtil.notEmpty(fakeFacets)) {
//			queryBuilder.addFacet(fakeFacets.toArray(new String[0]));
//		}
//		int pageSize = oReq.getInt("pageSize", 10);
//		int pageNumber = oReq.getInt("pageNumber", 1) - 1;
//		pageNumber = pageNumber < 0 ? 0 : pageNumber;
//		pageSize = pageSize > 50 ? 50 : pageSize;
//
//		queryBuilder.from(pageNumber * pageSize);
//		queryBuilder.size(pageSize);
//
//		BasicDBList sorts = (BasicDBList) oReq.get("sorts");
//		ParentChildOrNestedESProduct esProduct = ParentChildOrNestedESProduct.getInstance();
//		if (sorts != null && sorts.size() == 1) {
//			Object obj = sorts.get(0);
//			BasicDBObject oSort = (BasicDBObject) obj;
//			if (oSort != null && !oSort.isEmpty()) {
//
//				final boolean isAlwaysFalse = false;// 使用isAlwaysFalse，意味着列表也和搜索的其他排序都使用插件
//				// 价格，新品，销量，评论排序过滤掉补差价商品
//				if (esProduct.isPriceSort(oSort) || esProduct.isSaleSort(oSort)
//						|| esProduct.isNewProductSort(oSort)
//						|| esProduct.evaluateCountSort(oSort)) {
//					BoolFilterBuilder orFilter = FilterBuilders.boolFilter()
//							.cache(true);
//					orFilter.should(FilterBuilders.termFilter(
//							ChildIndexStructure.FIELD_MAKEUP_FLAG,
//							ChildIndexStructure.FIELD_MAKEUP_FLAG_VALUE_N));
//					queryBuilder.addSkuTermFilter(
//							ChildIndexStructure.FIELD_PRODSTATE, 4);
//					queryBuilder.addSkuFilter(orFilter);
//				}
//
//				if (esProduct.isNotCategorySearchButSalesVolumeSort(
//						isAlwaysFalse, oSort)) {
//					// 销量降序
//					queryBuilder.sort("_score", SortOrder.DESC);
//					queryBuilder.setScriptParm("gomeSortType", 1);
//					queryBuilder.setScriptParm("gomeSortAsc", false);
//
//				} else if (esProduct.isNotCategorySearchButPriceSort(
//						isAlwaysFalse, oSort)) {
//					// 价格排序
//					queryBuilder.sort("_score", SortOrder.DESC);
//					queryBuilder.setScriptParm("gomeSortType", 2);
//					if (oSort.getString("order", "desc").equalsIgnoreCase(
//							"desc")) {
//						queryBuilder.setScriptParm("gomeSortAsc", false);
//					} else {
//						queryBuilder.setScriptParm("gomeSortAsc", true);
//					}
//				} else if (esProduct.isNotCategorySearchButNewProductSort(
//						isAlwaysFalse, oSort)) {
//					// 新品降序
//					queryBuilder.sort("_score", SortOrder.DESC);
//					queryBuilder.setScriptParm("gomeSortType", 3);
//					queryBuilder.setScriptParm("gomeSortAsc", false);
//				} else if (esProduct.isNotCategorySearchButEvaluateCountSort(
//						isAlwaysFalse, oSort)) {
//					// 评论
//					queryBuilder.sort("_score", SortOrder.DESC);
//					queryBuilder.setScriptParm("gomeSortType", 4);
//					queryBuilder.setScriptParm("gomeSortAsc", false);
//				} else {
//					queryBuilder.setScriptParm("gomeSortType", 0);
//					queryBuilder.setScriptParm("gomeSortAsc", false);
//					// 针对云眼的促销排序
//					if (oSort.getString("name", "_score").compareToIgnoreCase(
//							"promoScore") == 0) {
//						queryBuilder
//								.sort("sku.promoScore",
//										oSort.getString("order", "desc")
//												.equalsIgnoreCase("desc") ? SortOrder.DESC
//												: SortOrder.ASC);
//					} else {
//						queryBuilder
//								.sort(oSort.getString("name", "_score"),
//										oSort.getString("order", "desc")
//												.equalsIgnoreCase("desc") ? SortOrder.DESC
//												: SortOrder.ASC);
//					}
//				}
//
//			} else {
//				queryBuilder.setScriptParm("gomeSortType", 0);
//				queryBuilder.setScriptParm("gomeSortAsc", false);
//				queryBuilder.sort("_score", SortOrder.DESC);
//			}
//		}
//
//		String testSorts = oReq.getString("testSorts");
//		if (StringUtils.isNotEmpty(testSorts)) {
//			testSorts = testSorts.replace(",", ":").replace(" ", ":")
//					.replace("-1", "d").replace("1", "a").replace("desc", "d")
//					.replace("asc", "a");
//			String[] testSortsArr = testSorts.split(":");
//			queryBuilder.sort(testSortsArr[0], testSortsArr[1]
//					.equalsIgnoreCase("d") ? SortOrder.DESC : SortOrder.ASC);
//		}
//
//		boolean debug = oReq.getBoolean("debug", false);
//		if (debug) {
//			queryBuilder.setExplain(true);
//		}
//
//		int instock = oReq.getInt("instock", 0);
//		String cityId = oReq.getString("regionId", "11010200");
//		if (instock == 1) {
//
//			if (cityId.equalsIgnoreCase("0")) {
//				cityId = "11010200";
//			}
//			cityId = blackDragonList.getCityIndex().get(cityId);
//			if (StringUtil.isEmpty(cityId)) {
//				cityId = "1";
//			}
//			queryBuilder.addSkuFilter(new MMFilterBuilder("sku.skuNo", cityId
//					.trim()).cache(true).cacheKey("dr_" + cityId.trim()));
//		}
//	}
//
//	public SearchResponse getBuleCouponSearchQueryResponse(
//			QueryBuilder queryBuilder, BasicDBObject oReq,
//			BasicDBObject oResult, boolean comMode) throws Exception {
//
//		long start = System.currentTimeMillis();
//		SearchResponse reponse = null;
//		start = System.currentTimeMillis();
//		queryBuilder.setScriptParm(PARAM_FAKE, false).setScriptParm(
//				PARAM_COMMODE, true);
//		queryBuilder.fake(false);
//		queryBuilder.makeUpQuery("(*:*)");
//		reponse = queryBuilder.get();
//		oResult.append("searchLevel", 1);
//		oResult.append("TookInMillis", oResult.getLong("TookInMillis", 0)
//				+ reponse.getTookInMillis());
//		oResult.append("queryTime",
//				oResult.getLong("queryTime", 0)
//						+ (System.currentTimeMillis() - start));
//
//		return reponse;
//	}
//
//	/**
//	 * 校验处理搜索请求
//	 * 
//	 * @param queryBuilder
//	 * @param oReq
//	 * @return boolean 失败返回true(Look Here)
//	 */
//	private boolean processBuleCouponSearch(
//			QueryBuilder queryBuilder, BasicDBObject oReq) {
//		// TODO 暂时只作用于自营的商品
//		queryBuilder.addSkuTermFilter(ChildIndexStructure.FIELD_PRODTAG, 1);
//		boolean failed = false;
//
//		return failed;
//	}
//}
