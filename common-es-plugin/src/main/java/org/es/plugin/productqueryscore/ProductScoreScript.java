package org.es.plugin.productqueryscore;

import java.math.BigDecimal;
import java.util.Map;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.index.fielddata.ScriptDocValues.Strings;
import org.elasticsearch.script.AbstractFloatSearchScript;

public class ProductScoreScript extends AbstractFloatSearchScript {
	static ESLogger logger = ESLoggerFactory.getLogger("[custom script ]", "script.esproduct");
	private static float weightWight = 1f;
	// gomeSortType表示的排序类型，其中0表示综合;1表示销量;2表示价格;3表示新品;4表示评论，其中是默认值
	private int gomeSortType;
	private boolean gomeSortAsc;
	final long fixDateLong = 315504000000l;// 1980-01-01
	public ProductScoreScript(Map<String, Object> params) {
		Integer value = (Integer) params.get("gomeSortType");
		this.gomeSortType = (value != null) ? value : 0;
		Boolean gomeSortAscValue = (Boolean) params.get("gomeSortAsc");
		this.gomeSortAsc = (gomeSortAscValue != null) ? gomeSortAscValue : false;
	}

	@Override
	public float runAsFloat() {
		float score = 0;
		switch (gomeSortType) {
		case 0:
			score = this.getScoreSortFloat();
			break;
		case 3:
			// 3表示新品
			score = this.getStartDateFloat();
			break;
		default:
			break;
		}
		return 0;
	}

	/**
	 * 获取综合排序得分
	 * 
	 * @return
	 */
	private float getScoreSortFloat() {
		float result = 10;
		try {
			double promScore = this.docFieldDoubles("promoScore").isEmpty() ? 0
					: this.docFieldDoubles("promoScore").getValue();
			double weight = this.docFieldDoubles("weight").isEmpty() ? 0 : this.docFieldDoubles("weight").getValue();
			String skuNo = this.docFieldStrings("skuNo").isEmpty() ? "" : this.docFieldStrings("skuNo").getValue();
			long productTag = this.docFieldLongs("productTag").isEmpty() ? 0
					: this.docFieldLongs("productTag").getValue();
			boolean ignoreProductTag = false;
			if ((!this.docFieldStrings(ESProductScriptConst.FIELD_IGNOREPRODUCTTag).isEmpty())
					&& this.docFieldStrings(ESProductScriptConst.FIELD_IGNOREPRODUCTTag).getValue()
							.equals(ESProductScriptConst.FIELD_IGNOREPRODUCTTag_VALUE_Y)) {
				ignoreProductTag = true;// 存在忽略联营和自营排序的字段,且该字段的值为y,那么当产品是联营时,忽略联营这自营的排序处理
			} else {
				ignoreProductTag = false;// 若不存在ignoreProductTag字段,且该字段的不值为y,那么当产品是联营时,继续差异排序处理
			}
			result = Float.valueOf(sum(Math.sqrt(weight * weightWight)));
		} catch (Exception e) {
			logger.error("runAsFloat fail ", e);
		}
		return result;
	}

	/**
	 * 获取价格排序得分
	 * 
	 * @return
	 */
	private float getStartDateFloat() {
		float result = 10;
		final int denominatorBase = 10000;

		try {

			long startDate = this.docFieldLongs("startDate").isEmpty() ? 0 : this.docFieldLongs("startDate").getValue();
			int days = getIntervalDays(startDate, fixDateLong);

			double fieldValue = 0;
			if (gomeSortAsc) {
				fieldValue += new BigDecimal(divl(30000, days + 1)).doubleValue();
			} else {
				fieldValue += new BigDecimal(divl(days, denominatorBase)).doubleValue();
			}

			String skuNo = this.docFieldStrings("skuNo").isEmpty() ? "" : this.docFieldStrings("skuNo").getValue();

			long areasalesVolume = 0;
			String addtionalScore = "0";
			Strings categories = this.docFieldStrings("categories");
			result = Float.valueOf(sum(fieldValue, addtionalScore, divl(areasalesVolume, 10000)));

		} catch (Exception e) {
			logger.error("runAsFloat fail ", e);
		}
		return result;
	}

	/**
	 * 获取价格排序得分
	 * 
	 * @return
	 */
	private float getPriceFloat() {
		float result = 10;
		final int denominatorBase = 10000;

		try {

			double price = this.docFieldDoubles("price").isEmpty() ? 0 : this.docFieldDoubles("price").getValue();
			double fieldValue = 0;
			if (gomeSortAsc) {
				if (price > 100000) {
					fieldValue += new BigDecimal(divl(1000000, (long) price + 1)).doubleValue() * 0.01;
				} else if (price <= 100000 && price > 10000) {
					fieldValue += new BigDecimal(divl(100000, (long) price + 1)).doubleValue() * 0.1;
				} else if (price <= 10000 && price > 1000) {
					fieldValue += new BigDecimal(divl(10000, (long) price + 1)).doubleValue();
				} else if (price <= 1000 && price > 100) {
					fieldValue += new BigDecimal(divl(1000, (long) price + 1)).doubleValue() * 10;
				} else if (price <= 100 && price > 10) {
					fieldValue += new BigDecimal(sum(price * -1, "200")).doubleValue();
				} else {
					fieldValue += new BigDecimal(sum(price * -1, "200")).doubleValue();
				}

			} else {
				fieldValue += new BigDecimal(divl((long) price, denominatorBase)).doubleValue();
			}

			String skuNo = this.docFieldStrings("skuNo").isEmpty() ? "" : this.docFieldStrings("skuNo").getValue();

			long areasalesVolume = 0;
			String addtionalScore = "0";
			result = Float.valueOf(sum(fieldValue, addtionalScore));

		} catch (Exception e) {
			logger.error("runAsFloat fail ", e);
		}
		return result;
	}

	public static String sum(double v1, String... vns) {

		BigDecimal sum = new BigDecimal(v1);
		for (String vn : vns) {
			sum = sum.add(new BigDecimal(vn));
		}
		return sum.setScale(10, BigDecimal.ROUND_HALF_UP).toString();
	}
	/**
	 * 两个long值差的天数
	 * 
	 * @param statDateLong
	 * @param fixDateLong
	 * @return
	 */
	public static int getIntervalDays(long statDateLong, long fixDateLong) {
		long intervalMilli = statDateLong - fixDateLong;
		return (int) (intervalMilli / (24 * 60 * 60 * 1000));

	}
	public static String divl(long v1, long v2) {

		BigDecimal b1 = new BigDecimal(v1);
		BigDecimal b2 = new BigDecimal(v2);

		return b1.divide(b2, 8, BigDecimal.ROUND_HALF_EVEN).setScale(8, BigDecimal.ROUND_HALF_EVEN).toString();
	}
}
