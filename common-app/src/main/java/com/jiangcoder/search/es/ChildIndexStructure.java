package com.jiangcoder.search.es;

/**
* @ClassName: ChildIndexStructure
* @Description: TODO(这里用一句话描述这个类的作用)
* @author A18ccms a18ccms_gmail_com
* @date May 12, 2015 3:37:25 PM
 */
public class ChildIndexStructure {
	
	public static final String FIELD_PRODID = "productId";
	public static final String FIELD_PRODSTATE = "productState";
	public static final String FIELD_PRODTAG = "productTag";

	public static final String FIELD_SKUID = "skuId";
	public static final String FIELD_SKUNO = "skuNo";
	public static final String FIELD_SKUSTATE = "skuState";

	public static final String FIELD_SORTNO = "sortNo";
	public static final String FIELD_SORTNO_INT = "sortNo_Int";

	public static final String FIELD_SHOPID = "shopId";
	public static final String FIELD_SHOPNAM = "shopName";
	public static final String FIELD_SHOPTYPE = "shopType";

	public static final String FIELD_PRICE = "price";
	public static final String FIELD_SALESV = "salesVolume";
	
	public static final String FIELD_ON_LINE_SALEV  = "onlineQuantity" ;//"onLineSale";
	public static final String FIELD_OFF_LINE_SALEV = "offlineQuantity";//"offLineSale";
	
	public static final String FIELD_STARTDATE = "startDate";
	public static final String FIELD_EVALUATECOUNT = "evaluateCount";
	public static final String FIELD_WEIGHT = "weight";

	public static final String FIELD_CATBRAND = "categoryBrand";
	public static final String FIELD_TITLE = "title";
	public static final String FIELD_NGRAM = "n-gram";
	public static final String FIELD_FACET = "facet";
	public static final String FIELD_PROMOSCORE = "promoScore";
	public static final String FIELD_PROMOTAG = "promoTag";
	public static final String FIELD_PROMOFLAG = "promoFlag";

	public static final String FIELD_CATS = "categories";
	public static final String FIELD_IGNOREPRODUCTTag = "ignoreProductTag";
	public static final String FIELD_IGNOREPRODUCTTag_VALUE_Y = "y";
	public static final String FIELD_IGNOREPRODUCTTag_VALUE_N = "n";

	public static final String FIELD_JD_PRICE = "jdPrice";
	public static final String FIELD_JD_TIME = "jdTime";
	public static final String FIELD_SN_PRICE = "snPrice";
	public static final String FIELD_SN_TIME = "snTime";

	public static final String FIELD_DY_PREFIX_F = "f";
	public static final String FIELD_PREFIX_F = "f.";
	public static final String FIELD_DY_PREFIX_FV = "fv";
	public static final String FIELD_DY_PREFIX_FP = "fp";
	public static final String FIELD_CLEAR_PREFIX_FP = "c_catId";
	public static final String FIELD_PREFIX_S = "s.";
	public static final String FIELD_DRAGON_A = "a";
	public static final String FIELD_DRAGON_C = "c";
	
	public static final String FIELD_energy_start_date = "energy_start_date";
	public static final String FIELD_energy_end_date   = "energy_end_date";
	
	public static final String FIELD_tuan_start_date = "tuan_start_date";
	public static final String FIELD_tuan_end_date   = "tuan_end_date";
	
	public static final String FIELD_qiang_start_date = "qiang_start_date";
	public static final String FIELD_qiang_end_date   = "qiang_end_date";
	
	public static final String FIELD_sale_price_start_date = "sale_price_start_date";
	public static final String FIELD_sale_price_end_date   = "sale_price_end_date";
	
	public static final String FIELD_sendRedCoupon_start_date = "sale_sendRedCoupon_start_date";
	public static final String FIELD_sendRedCoupon_end_date   = "sale_sendRedCoupon_end_date";
	
	public static final String FIELD_sendBlueCoupon_start_date = "sale_sendBlueCoupon_start_date";
	public static final String FIELD_sendBlueCoupon_end_date   = "sale_sendBlueCoupon_end_date";
	
	public static final String FIELD_SHOPPINGCART_INCLUDE_ACTIVITIES   = "shoppingcart_include_activities";
	public static final String FIELD_SHOPPINGCART_EXCLUDE_ACTIVITIES   = "shoppingcart_exclude_activities";
	
	
	
	/**
	 * 补差价
	 */
	public static final String FIELD_MAKEUP_FLAG = "makeupFlag";
	public static final String FIELD_MAKEUP_FLAG_STORE = "makeupFlagStore";
	public static final int FIELD_MAKEUP_FLAG_VALUE_Y = 1;
	public static final int FIELD_MAKEUP_FLAG_VALUE_N = 0;
		
	/**
	 * 海外购
	 */
	public static final String FIELD_MARKETTAG = "marketTag";
	public static final String FIELD_MARKETTAG_STORE = "marketTagStore";
	public static final int    FIELD_MARKETTAG_VALUE_Y = 1;
	public static final int    FIELD_MARKETTAG_VALUE_N = 0;
	
	
	
	
	/**
	 * 活动id
	 */
	public static final String FIELD_ACTIVITY_ID = "activityId";
	
	
	public static final String NEST_PATH = "sku";

}
