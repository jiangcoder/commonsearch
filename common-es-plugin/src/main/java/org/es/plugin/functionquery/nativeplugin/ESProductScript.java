package org.es.plugin.functionquery.nativeplugin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.util.CollectionUtil;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.index.fielddata.ScriptDocValues.Strings;
import org.elasticsearch.script.AbstractFloatSearchScript;
import org.es.plugin.functionquery.ESProductScriptConst;

/**
 * script for product sort
 *
 */
public class ESProductScript extends AbstractFloatSearchScript {
	static ESLogger logger = ESLoggerFactory.getLogger("[custom script ]", "script.esproduct");
	private String regionId;
	private int sCityId = 1;
	private boolean comMode;
	private boolean fake;
	// gomeSortType表示的排序类型，其中0表示综合;1表示销量;2表示价格;3表示新品;4表示评论，其中是默认值
	private int gomeSortType;
	private boolean gomeSortAsc;

	// private String fakeCatId;
	private static float productTagWight = 0.02f;
	private static float marketTaWight = 0.013f;// 海外购权重

	private static float promoScoreWeight = 0.25f;
	private static float weightWight = 1f;
	private static float cateRadix = 285f;
	private static float cateTotal = 300f;
	private Map<String, Double> hotCatIds;
	private List<String> hotCategoriesAfterSort;

	@SuppressWarnings({ "unchecked", "static-access" })
	public ESProductScript(Map<String, Object> params) {

		this.regionId = (String) params.get("regionId");

		if (params.get("sCityId") != null) {
			this.sCityId = Integer.parseInt((String) params.get("sCityId"));
		}

		this.fake = (Boolean) params.get("fake");
		this.comMode = (Boolean) params.get("comMode");
		this.productTagWight = params.get("productTagWight") == null ? productTagWight
				: (Float) params.get("productTagWight");
		this.hotCatIds = (Map<String, Double>) params.get("hotCatIds");
		Integer value = (Integer) params.get("gomeSortType");
		this.gomeSortType = (value != null) ? value : 0;
		Boolean gomeSortAscValue = (Boolean) params.get("gomeSortAsc");
		this.gomeSortAsc = (gomeSortAscValue != null) ? gomeSortAscValue : false;
		this.hotCategoriesAfterSort = this.getHotCategoriesAfterSort(hotCatIds);
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

			// 是否是海外购商品
			boolean isMakeup = ((!this.docFieldLongs(ESProductScriptConst.FIELD_MAKEUP_FLAG_STORE).isEmpty())
					&& this.docFieldLongs(ESProductScriptConst.FIELD_MAKEUP_FLAG_STORE).getValue() == 1) ? true : false;
			// 是否是价格补差商品
			boolean isMarketTag = ((!this.docFieldLongs(ESProductScriptConst.FIELD_MARKETTAG_STORE).isEmpty())
					&& this.docFieldLongs(ESProductScriptConst.FIELD_MARKETTAG_STORE).getValue() == 1) ? true : false;
			if (isMakeup) {
				return 10;
			}
			long areasalesVolume = 0;
			int stock = 0;
			stock = getStock(skuNo, sCityId);
			if (stock > 1)
				stock = 1;
			String addtionalScore = "0";
			Strings categories = this.docFieldStrings("categories");

			if (!categories.isEmpty() && categories.getValues().size() > 0 && hotCatIds != null
					&& hotCatIds.size() > 0) {
				addtionalScore = String.valueOf(getCategoryScore(categories.getValues(), hotCatIds));
			}
			if (comMode) {
				if (fake) {
					result = Float.valueOf(sum(Math.sqrt(weight * weightWight), mul(stock, 1000),
							mul(promScore, promoScoreWeight), addtionalScore, divl(areasalesVolume, 10000)));

				} else {
					result = Float.valueOf(sum(Math.sqrt(div(weight, 100)), mul(stock, 1000), mul(promScore, 80)));
				}
				if (productTag == 1) {
					if (stock == 1) {
						float noStock = result - 1000;
						result = (noStock + noStock * productTagWight) + 1000;
					} else {
						result = result * productTagWight + result;
					}
				} else {
					// 存在忽略联营和自营排序的字段,且该字段的值为y,那么当产品是联营时,忽略联营这自营的排序处理
					if (ignoreProductTag) {
						if (stock == 1) {
							float noStock = result - 1000;
							result = (noStock + noStock * productTagWight) + 1000;
						} else {
							result = result * productTagWight + result;
						}
					} else if (isMarketTag) {
						// 如果是海外购商品，那么要进行一个加权
						if (stock == 1) {
							float noStock = result - 1000;
							result = (noStock + noStock * marketTaWight) + 1000;
						} else {
							result = result * marketTaWight + result;
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("runAsFloat fail ", e);
		}
		return result;
	}

	/**
	 * 获取商品分类得分
	 * 
	 * @param catIds
	 * @param hotCatIds
	 * @return
	 */
	private double getHotCategoryIdStageScore(List<String> catIds, Map<String, Double> hotCatIds) {
		if (catIds == null || catIds.size() == 0) {
			return 0.0;
		} else {
			int index = this.getHotCategroyIndex(catIds, hotCategoriesAfterSort);
			double score = (this.MAX_THREE - index) * 100.0;
			return score;
		}
	}

	final int MAX_CATEGORY_INDEX = 10;
	final int MAX_THREE = 3;

	/**
	 * 获取分类所在热门分类的下标
	 * 
	 * @param catIds
	 * @param hotCategoriesAfterSort
	 * @return
	 */
	private int getHotCategroyIndex(List<String> catIds, List<String> hotCategoriesAfterSort) {

		if (catIds == null || catIds.size() == 0)
			return MAX_CATEGORY_INDEX;

		if (hotCategoriesAfterSort == null || hotCategoriesAfterSort.size() == 0)
			return MAX_CATEGORY_INDEX;

		for (int i = 0; i < hotCategoriesAfterSort.size(); i++) {
			if (catIds.contains(hotCategoriesAfterSort.get(i))) {
				if (i < MAX_THREE)
					return i;
				else
					return MAX_THREE;
			}
		}

		return MAX_THREE;
	}

	/**
	 * 热门分类排序
	 * 
	 * @param hotCatIds
	 * @return
	 */
	private List<String> getHotCategoriesAfterSort(Map<String, Double> hotCatIds) {
		if (hotCatIds != null && hotCatIds.size() != 0) {

			List<String> result = new ArrayList<String>();
			result.addAll(hotCatIds.keySet());

			CollectionUtil.introSort(result, new MapComparator(hotCatIds));
			return result;
		} else {
			return new ArrayList<String>();
		}
	}

	class MapComparator implements Comparator<String> {

		public Map<String, Double> map;

		public MapComparator(Map<String, Double> map) {
			this.map = map;
		}

		@Override
		public int compare(String o1, String o2) {
			return (map.get(o2) > map.get(o1)) ? 1 : 0;
		}
	}

	/**
	 * 获取销量排序得分
	 * 
	 * @return
	 */
	private float getSaleVolumeFloat() {
		float result = 10;
		final int denominatorBase = 10000;

		try {
			long salesVolume = this.docFieldLongs("salesVolume").isEmpty() ? 0
					: this.docFieldLongs("salesVolume").getValue();
			double fieldValue = 0;
			if (gomeSortAsc) {
				fieldValue += new BigDecimal(divl(1, salesVolume + 1)).doubleValue();
			} else {
				fieldValue += new BigDecimal(divl(salesVolume, denominatorBase)).doubleValue();
			}

			String skuNo = this.docFieldStrings("skuNo").isEmpty() ? "" : this.docFieldStrings("skuNo").getValue();

			long areasalesVolume = 0;

			int stock = 0;
			stock = getStock(skuNo, sCityId);
			if (stock > 1)
				stock = 1;
			String addtionalScore = "0";
			Strings categories = this.docFieldStrings("categories");

			if (!categories.isEmpty() && categories.getValues().size() > 0 && hotCatIds != null
					&& hotCatIds.size() > 0) {
				addtionalScore = String.valueOf(this.getHotCategoryIdStageScore(categories.getValues(), hotCatIds));
			}
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
			int stock = 0;
			stock = getStock(skuNo, sCityId);
			if (stock > 1)
				stock = 1;
			String addtionalScore = "0";
			Strings categories = this.docFieldStrings("categories");
			if (!categories.isEmpty() && categories.getValues().size() > 0 && hotCatIds != null
					&& hotCatIds.size() > 0) {
				if (gomeSortAsc) {
					addtionalScore = String
							.valueOf(this.getHotCategoryIdStageScore(categories.getValues(), hotCatIds) * 2);
				} else {
					addtionalScore = String.valueOf(this.getHotCategoryIdStageScore(categories.getValues(), hotCatIds));
				}

			}
			result = Float.valueOf(sum(fieldValue, addtionalScore, divl(areasalesVolume, 10000)));

		} catch (Exception e) {
			logger.error("runAsFloat fail ", e);
		}
		return result;
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

	final long fixDateLong = 315504000000l;// 1980-01-01

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

			//
			int stock = 0;
			stock = getStock(skuNo, sCityId);

			if (stock > 1)
				stock = 1;
			//
			String addtionalScore = "0";
			Strings categories = this.docFieldStrings("categories");

			if (!categories.isEmpty() && categories.getValues().size() > 0 && hotCatIds != null
					&& hotCatIds.size() > 0) {
				addtionalScore = String.valueOf(this.getHotCategoryIdStageScore(categories.getValues(), hotCatIds));
			}

			// result=Float.valueOf(sum(fieldValue,mul(stock,
			// stockBase),addtionalScore,divl(areasalesVolume,10000)));
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
	private float getEvaluateCountFloat() {
		float result = 10;
		final int denominatorBase = 10000;

		try {

			long evaluateCount = this.docFieldLongs("evaluateCount").isEmpty() ? 0
					: this.docFieldLongs("evaluateCount").getValue();

			double fieldValue = 0;
			if (gomeSortAsc) {
				fieldValue += new BigDecimal(divl(1, evaluateCount + 1)).doubleValue();
			} else {
				fieldValue += new BigDecimal(divl(evaluateCount, denominatorBase)).doubleValue();
			}

			String skuNo = this.docFieldStrings("skuNo").isEmpty() ? "" : this.docFieldStrings("skuNo").getValue();

			long areasalesVolume = 0;
			int stock = 0;
			stock = getStock(skuNo, sCityId);

			if (stock > 1)
				stock = 1;
			String addtionalScore = "0";
			Strings categories = this.docFieldStrings("categories");

			if (!categories.isEmpty() && categories.getValues().size() > 0 && hotCatIds != null
					&& hotCatIds.size() > 0) {
				addtionalScore = String.valueOf(this.getHotCategoryIdStageScore(categories.getValues(), hotCatIds));
			}

			result = Float.valueOf(sum(fieldValue, addtionalScore, divl(areasalesVolume, 10000)));

		} catch (Exception e) {
			logger.error("runAsFloat fail ", e);
		}
		return result;
	}

	@Override
	public float runAsFloat() {
		float result = 0;
		// 0表示综合;1表示销量;2表示价格;3表示新品;4表示评论
		switch (gomeSortType) {
		case 0:
			// 0表示综合
			result = this.getScoreSortFloat();
			break;
		case 1:
			// 1表示销量
			result = this.getSaleVolumeFloat();
			break;
		case 2:
			// 2表示价格
			result = this.getPriceFloat();
			break;
		case 3:
			// 3表示新品
			result = this.getStartDateFloat();
			break;
		case 4:
			// 4表示评论
			result = this.getEvaluateCountFloat();
			break;
		default:
			// 其他值都走综合排序
			result = this.getScoreSortFloat();
		}

		return result;

	}

	public Double getCategoryScore(List<String> catIds, Map<String, Double> hotCatIds) {
		double result = 0;
		for (Map.Entry<String, Double> hotCatId : hotCatIds.entrySet()) {
			if (catIds.contains(hotCatId.getKey())) {
				return result = cateRadix + (cateTotal - cateRadix) * hotCatId.getValue();
			}
		}
		return result;
	}

	public static int getStock(String skuNo, int regionId) {
		return 1;// BlackDragonList.getStock(skuNo, regionId);
	}

	public static String sum(double v1, String... vns) {

		BigDecimal sum = new BigDecimal(v1);
		for (String vn : vns) {
			sum = sum.add(new BigDecimal(vn));
		}
		return sum.setScale(10, BigDecimal.ROUND_HALF_UP).toString();
	}

	public static double div(double v1, int v2) {
		return v1 / v2;
	}

	public static String divl(long v1, long v2) {

		BigDecimal b1 = new BigDecimal(v1);
		BigDecimal b2 = new BigDecimal(v2);

		return b1.divide(b2, 8, BigDecimal.ROUND_HALF_EVEN).setScale(8, BigDecimal.ROUND_HALF_EVEN).toString();
	}

	public static String mul(int v1, int v2) {
		BigDecimal b1 = new BigDecimal(v1);
		BigDecimal b2 = new BigDecimal(v2);
		return b1.multiply(b2).toString();
	}

	public static String mul(double v1, int v2) {
		BigDecimal b1 = new BigDecimal(v1);
		BigDecimal b2 = new BigDecimal(v2);
		return b1.multiply(b2).toString();
	}

	public static String mul(double v1, double v2) {
		BigDecimal b1 = new BigDecimal(v1);
		BigDecimal b2 = new BigDecimal(v2);
		return b1.multiply(b2).toString();
	}

	public static double mull(double v1, int v2) {
		BigDecimal b1 = new BigDecimal(v1);
		BigDecimal b2 = new BigDecimal(v2);
		return b1.multiply(b2).doubleValue();
	}

	public static void setProdWeight(float pdw) {
		productTagWight = pdw;
	}

	public static void setProsWeight(float psw) {
		promoScoreWeight = psw;
	}

	public static void setWWeight(float ww) {
		weightWight = ww;
	}

	public static void setCateRadix(float cr) {
		cateRadix = cr;
	}

	public static void setCateTotal(float ct) {
		cateTotal = ct;
	}

	public static float getProdWeight() {
		return productTagWight;
	}

	public static float getProsWeight() {
		return promoScoreWeight;
	}

	public static float getWWeight() {
		return weightWight;
	}

	public static float getCateRadix() {
		return cateRadix;
	}

	public static float getCateTotal() {
		return cateTotal;
	}

	public static float getMarketTaWight() {
		return marketTaWight;
	}

	public static void main(String[] args) {
		System.out.println(1000);
		System.out.println(Float.valueOf(sum(Math.sqrt(div(1, 10)), mul(1, 50), mul(1, 5))));
		System.out.println(0.3 / (1 + 0.3));

		new BigDecimal(1).divide(new BigDecimal(1).add(new BigDecimal(40).divide(new BigDecimal(40d))));
		System.out.println(
				new BigDecimal(40).divide(new BigDecimal(1).add(new BigDecimal(40).divide(new BigDecimal(10)))));

		System.out.println(Math.sqrt(0.3444222 * 10));
		System.out.println(0.3444222 * 10);
	}
}
