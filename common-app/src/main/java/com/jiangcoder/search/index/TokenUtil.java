package com.jiangcoder.search.index;

import java.util.ArrayList;
import java.util.Map;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiangcoder.search.segment.GomeSegment;
import com.jiangcoder.search.segment.TermVec;
import com.mongodb.BasicDBObject;

public class TokenUtil{

	protected static Logger logger = LoggerFactory.getLogger(TokenUtil.class);

	private GomeSegment gomeAnalyzer;

	public static enum GomeTokenMode {
		search, index
	}

	private void setSegment(GomeSegment gomeAnalyzer) {
		this.gomeAnalyzer = gomeAnalyzer;
	}

	public GomeSegment getAnalyzer() {
		return gomeAnalyzer;
	}

	

	public ArrayList<BasicDBObject> parseTokensGome(String content,
			GomeTokenMode mode, boolean enableGram) {
		ArrayList<BasicDBObject> results = new ArrayList<BasicDBObject>();
		try {
			Map<String, TermVec> tokens = gomeAnalyzer
					.QuerySegment_Pos(content);
			if (tokens == null || tokens.size() == 0) {
				logger.warn("QuerySegment null.");
				return results;
			}
			delEdgeTerm(content, tokens);
			for (Map.Entry<String, TermVec> entry : tokens.entrySet()) {

				String key = entry.getKey();
				TermVec value = entry.getValue();
				BasicDBObject oToken = new BasicDBObject();

				switch (value.mType) {
				case 1:
				case 4:
					oToken.append("text", key).append("level", 1)
							.append("pro", value.mPro);
					break;
				case 2:
				case 3:
					oToken.append("text", key).append("level", 3);
					break;
				default:
					logger.warn(
							"QuerySegment_Pos key [{}],no type termVec.mtype  [{}] ",
							new Object[] { key, value.mType });
					break;

				}
				results.add(oToken);
				// logger.info("query content:"+content+"----key:"+key+"----termVec.mtype"+value.mType);
			}
		} catch (Exception e) {
			logger.error("parseTokensGome fail.", e);
		}

		return results;
	}

	private void delEdgeTerm(String content, Map<String, TermVec> tokens) {
		String maxTerm = "";
		Integer maxPos = 0;
		if (content.length() < 40)
			return;
		for (Map.Entry<String, TermVec> entry : tokens.entrySet()) {

			TermVec value = entry.getValue();
			for (Integer pos : value.mStartPos) {
				if (pos > maxPos) {
					maxPos = pos;
					maxTerm = entry.getKey();

				}
			}
		}
		if (tokens.get(maxTerm) != null
				&& tokens.get(maxTerm).mStartPos.size() == 1
				&& StringUtils.isNotEmpty(maxTerm)) {
			tokens.remove(maxTerm);
		}
	}

	
	public ArrayList<String> parseTokensGome(String content, boolean enableGram) {
		ArrayList<String> results = new ArrayList<String>();
		long start = System.currentTimeMillis();
		Map<String, TermVec> tokens = gomeAnalyzer
				.ProductTitleSegment_Pos(content);
		if (tokens == null || tokens.size() == 0)
			return results;
		StringBuilder keys = new StringBuilder();
		StringBuilder words = new StringBuilder();
		for (Map.Entry<String, TermVec> entry : tokens.entrySet()) {

			String key = entry.getKey();
			TermVec value = entry.getValue();

			switch (value.mType) {
			case 1:
			case 4:
				keys.append(key).append(" ");
				break;
			case 2:
			case 3:
				if (enableGram)
					words.append(key).append(" ");
				break;
			default:
				logger.warn(
						"ProductTitleSegment_Pos key [{}],no type termVec.mtype [{}] ,time:",
						new Object[] { key, value.mType,
								System.currentTimeMillis() - start });
				break;
			}
		}
		results.add(keys.toString());
		if (words.length() > 0) {
			results.add(words.toString());
		}

		return results;
	}

}
