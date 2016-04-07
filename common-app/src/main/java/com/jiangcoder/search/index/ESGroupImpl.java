package com.jiangcoder.search.index;

import java.util.ArrayList;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.replication.ReplicationType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.engine.DocumentMissingException;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.ScriptService.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiangcoder.search.es.ESClientUtils;
import com.jiangcoder.search.index.TokenUtil.GomeTokenMode;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

public class    ESGroupImpl {
	protected static Logger logger = LoggerFactory.getLogger(ESGroupImpl.class);
	private static final String FIELD_ID = "id";
	private static final String FIELD_PRODUCT_NAME = "groupName";
	private static final String FIELD_END_TIME = "endTime";
	private static final String FIELD_CATEGORY = "category";
	private static final String FIELD_ISCOO8PRODUCT = "isCoo8Product";
	private static final String FIELD_SORT = "sort";
	private static final String FIELD_CATEGORY_NAME = "categoryName";

	private static final String FIELD_TITLE = "title";

	private Client client;



	protected String indexName = "group_v2";
	protected String indexType = "group";
	protected String indexAlias = "groupType";
	protected TokenUtil tokenUtil;

	public void setTokenUtil(TokenUtil tokenUtil){
		this.tokenUtil = tokenUtil;
	}
	public void setClientUtils(ESClientUtils clientUtils) {
		this.client = clientUtils.getTransportClient();
	}
	public Client getClient(){
		return client;
	}
	public BasicDBObject search(BasicDBObject req) {
		logger.info("ESGroup search......");
		BasicDBObject result = new BasicDBObject("total", 0);
		long start = System.currentTimeMillis();
		try {
			String category = req.getString("category", "");
			String question = req.getString("question");
			if (StringUtils.isEmpty(question)
					&& StringUtils.isEmpty(req.getString("category"))) {
				logger.warn("search group input invalid.");
				return result;
			}
			Client client = getClient();
			SearchRequestBuilder searchBuilder = client.prepareSearch(
					indexAlias).setTypes(indexType);
			BoolFilterBuilder boolFilter = FilterBuilders.boolFilter().cache(
					true);

			String sql = "";
			if (!StringUtils.isEmpty(question)) {
				if (question.length() > 40)
					question = question.substring(0, 40);
				sql = this.parseQuestion(question);

			} else {
				sql = "*:*";
			}

			long current = System.currentTimeMillis();
			boolFilter.must(FilterBuilders.rangeFilter(FIELD_END_TIME)
					.gte(current).cache(true));

			if (StringUtils.isNotEmpty(category)) {
				boolFilter.must(FilterBuilders.termFilter(FIELD_CATEGORY,
						category));
			}
				try {
					String isCoo8Product = req.getString("isCoo8Product");
					if ((isCoo8Product != null)
							&& (isCoo8Product instanceof String)) {
						boolFilter.must(FilterBuilders.termFilter(
								FIELD_ISCOO8PRODUCT,
								Boolean.valueOf(isCoo8Product)));
					} else {
						if (isCoo8Product == null) {
							logger.info("necessary param isCoo8Product is null");
						} else if (!(isCoo8Product instanceof String)) {
							logger.info("necessary param isCoo8Product is  not String");
						}
					}
				} catch (Exception e) {
					logger.info("necessary param isCoo8Product is wrong,e is"
							+ e.toString());
				}

			searchBuilder.setQuery(QueryBuilders.filteredQuery(
					QueryBuilders.queryStringQuery(sql.toString()), boolFilter));
			
			BasicDBObject oSort = (BasicDBObject) req.get("sort");
			if (oSort == null) {
				oSort = new BasicDBObject();
				oSort.append("name", "_score").append("order", "desc");
			}
			searchBuilder.addSort(SortBuilders.fieldSort(
					oSort.getString("name", "_score"))
					.order(oSort.getString("order", "desc").equalsIgnoreCase(
							"desc") ? SortOrder.DESC : SortOrder.ASC));
			int pageNum = req.getInt("pageNum", 1);
			int pageSize = req.getInt("size", 60);
			searchBuilder.setFrom((pageNum - 1) * pageSize).setSize(pageSize);
			SearchResponse response = searchBuilder.execute().actionGet();

			result.put("total", response.getHits().getTotalHits());
			long count = response.getHits().getTotalHits();
			if (count != 0) {
				BasicDBList data = new BasicDBList();
				result.append("data", data);
				for (SearchHit hit : response.getHits()) {
					BasicDBObject item = new BasicDBObject(hit.sourceAsMap());
					item.append("id", hit.getId());
					data.add(item);
				}
			}

			long useTime = System.currentTimeMillis() - start;
			logger.info(
					"gs&s={}ms,q={},cat={},sort={}|{},hit={} ",
					new Object[] { useTime, question, category,
							oSort.getString("name"), oSort.getString("order"),
							count });
		} catch (Exception e) {
			logger.error("search fail", e);
		}

		return result;
	}
	private String parseQuestion(String question) {

		StringBuilder sql = new StringBuilder();
		ArrayList<BasicDBObject> tokens = parseTokens(question,
				GomeTokenMode.search, true);
		if (tokens == null || tokens.size() == 0) {
			logger.error("question [{}]  tokens empty!",
					 question );
			return "*:*";
		}
		BasicDBObject tokenList = this.reParseTokens(tokens);
		ArrayList<String> keys = null;
		sql.append("title:( ");
		if (tokenList.containsField("keys")) {
			keys = (ArrayList<String>) tokenList.get("keys");
			sql.append(StringUtil.join(keys, "AND"));

		}
		if (tokenList.containsField("ngrams")) {
			if (keys != null) {
				sql.append(" AND ");
			}
			ArrayList<String> ngrams = (ArrayList<String>) tokenList
					.get("ngrams");
			sql.append(StringUtil.join(ngrams, "AND"));
		}
		sql.append(" )");
		return sql.toString();
	}
	public int add(BasicDBList req) {

		if (req == null || req.size() == 0)
			return 0;

		BulkRequestBuilder bulkRequest = client.prepareBulk();
		try {
			BasicDBList indexDocs =req;
			if (indexDocs == null || indexDocs.size() == 0)
				return 0;
			this.buildBulkParam(bulkRequest, indexDocs);

		} catch (Exception e) {
			logger.error("build Bulk Param fail", e);
			return 0;
		}

		BulkResponse bulkResponse = bulkRequest
				.setReplicationType(ReplicationType.SYNC)
				.setConsistencyLevel(WriteConsistencyLevel.ALL).execute()
				.actionGet();
		if (bulkResponse.hasFailures()) {
			logger.error(bulkResponse.buildFailureMessage());
			return 0;
		}
		return 0;
	}
	public void buildBulkParam(Object bulkRequestObj, BasicDBList input) {
		BulkRequestBuilder bulkRequest = (BulkRequestBuilder) bulkRequestObj;
		String current = DateUtils.getCurYearMonthDay_HHmmss();
		for (int i = 0; i < input.size(); i++) {
			BasicDBObject doc = (BasicDBObject) input.get(i);
			StringBuilder titleBuilder = new StringBuilder();
			String cagegoryName = doc.getString(FIELD_CATEGORY_NAME);
			String groupName = doc.getString(FIELD_PRODUCT_NAME);
			int sort = doc.getInt(FIELD_SORT, -1);
			if (sort == -1) {
				sort = Integer.MAX_VALUE;
			}
			doc.put(FIELD_SORT, sort);
			doc.put("groupOnId", doc.getString(FIELD_ID));
			doc.put("lastTime", current);
			titleBuilder.append(cagegoryName).append(" ").append(groupName);

			ArrayList<String> tokens = parseTokens(titleBuilder.toString(),
					true);
			doc.put(FIELD_TITLE, tokens.get(0) + " "
					+ (tokens.size() == 2 ? tokens.get(1) : ""));
			bulkRequest.add(getClient().prepareIndex(indexName, indexType,
					doc.getString(FIELD_ID)).setSource(doc.toMap()));
		}
	}
	public boolean updateFiledById(String id, String field, Object value,
			boolean isScheduler) {
		return updateFiledById(id, field, value);
	}

	public boolean updateFiledById(String id, String field, Object value) {

		try {
			StringBuffer script = new StringBuffer("ctx._source.")
					.append(field).append("=");

			if (value instanceof String) {
				script.append("'").append(value).append("'");
			} else {
				script.append(value);
			}
			client.prepareUpdate(indexName, indexType, id)
					.setScript(script.toString(), ScriptType.INLINE)
					.setRetryOnConflict(3)
					.setReplicationType(ReplicationType.SYNC)
					.setConsistencyLevel(WriteConsistencyLevel.ALL).execute();

		} catch (DocumentMissingException mis) {
			logger.error(
					"update  indexName [{}] type[{}] id [{}] for field [{}] fail,no such identifier.",
					new Object[] { indexName, indexType, id, field });
			return false;
		} catch (Exception e) {
			logger.error("update filed fail", e);
			return false;
		}

		return true;
	}

	public boolean updateFiledsById(String id, BasicDBObject values) {

		if (StringUtils.isEmpty(id) || values == null || values.size() == 0)
			return false;
		try {

			StringBuffer script = new StringBuffer();

			for (Entry<String, Object> entry : values.entrySet()) {
				Object value = entry.getValue();
				script.append("ctx._source.").append(entry.getKey())
						.append("=");
				if (value instanceof String) {
					script.append("'").append(value).append("'");
				} else {
					script.append(value);
				}
				script.append(";");
			}

			client.prepareUpdate(indexName, indexType, id)
					.setScript(script.toString(), ScriptType.INLINE)
					.setRetryOnConflict(3)
					.setReplicationType(ReplicationType.SYNC)
					.setConsistencyLevel(WriteConsistencyLevel.ALL).execute()
					.actionGet();

		} catch (DocumentMissingException mis) {
			logger.error(
					"update  indexName [{}] type[{}] id [{}] for series fields [{}] fail,no such identifier.",
					new Object[] { indexName, indexType, id, values.toString() });
			return false;
		} catch (Exception e) {
			logger.error("update filed fail", e);
			return false;
		}

		return true;
	}
	
	public ArrayList<String> parseTokens(String content, boolean enableGram) {
			return tokenUtil.parseTokensGome(content, enableGram);
	}

	public ArrayList<BasicDBObject> parseTokens(String content,
			GomeTokenMode mode, boolean enableGram) {
		if (StringUtils.isEmpty(content)) {
			return new ArrayList<BasicDBObject>();
		}
		return tokenUtil.parseTokensGome(content, mode, enableGram);
	}

	public BasicDBObject reParseTokens(ArrayList<BasicDBObject> tokens) {
		BasicDBObject result = new BasicDBObject();
		ArrayList<String> keys = new ArrayList<String>();
		ArrayList<String> ngrams = new ArrayList<String>();
		for (int i = 0; i < tokens.size(); i++) {
			BasicDBObject oToken = tokens.get(i);
			String token = oToken.getString("text");
			if (EmptyUtil.isEmpty(token))
				continue;
			switch (oToken.getInt("level")) {
			case 1:
			case 2:
				if (!token.contains(Global.SPECIFIC_SYMBOL)) {
					keys.add(token);
				}
				break;
			case 3:
				String[] ns = token.split(" ");
				for (String n : ns) {
					if (!n.contains(Global.SPECIFIC_SYMBOL)) {
						ngrams.add(n);
					}
				}
				break;
			}
		}

		if (keys != null && keys.size() != 0) {
			result.append("keys", keys);
		}
		if (ngrams != null && ngrams.size() != 0) {
			result.append("ngrams", ngrams);
		}
		return result;
	}





}