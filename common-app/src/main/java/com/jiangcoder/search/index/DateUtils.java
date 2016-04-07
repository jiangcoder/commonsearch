package com.jiangcoder.search.index;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
	
	public static SimpleDateFormat simpleDateFormat;

	public static String YEAR = "yyyy";

	public static String YEAR_MONTH = "yyyy-MM";

	public static String YEAR_MONTH_DAY = "yyyy-MM-dd";

	public static String YEAR_MONTH_DAY_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

	public static String YEAR_MONTH_DAY_HH_MM = "yyyy-MM-dd HH:mm";

	public static String HH_MM_SS = "HH:mm:ss";

	public static String YEAR_MONTH_DAY_HH = "yyyy-MM-dd HH";

	public static  synchronized SimpleDateFormat getSimpleDateFormat(String pattern) {
		if (simpleDateFormat == null) {
			simpleDateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
		}
		simpleDateFormat.applyPattern(pattern);

		return simpleDateFormat;
	}

	/**
	 * 
	 * @return 返回当前年月
	 */
	public static String getCurYear() {
		return getSimpleDateFormat(YEAR).format(new Date());
	}

	/**
	 * 
	 * @return 返回当前年月
	 */
	public static String getCurYearMonth() {
		return getSimpleDateFormat(YEAR_MONTH).format(new Date());
	}

	/**
	 * 返回当前日期
	 * 
	 * @return
	 */
	public static String getCurDate() {
		return getSimpleDateFormat(YEAR_MONTH_DAY).format(new Date());
	}

	public static String getStringAsDate(Date date, String dateFormat) {
		if (date == null)
			return "";
		return getSimpleDateFormat(dateFormat).format(date);
	}

	/**
	 * 返回当前日期 + 小时：分：秒
	 * 
	 * @return
	 */
	public static String getCurYearMonthDay_HHmmss() {
		return getSimpleDateFormat(YEAR_MONTH_DAY_HH_MM_SS).format(new Date());
	}

	/**
	 * 返回当前日期 + 小时：分：秒
	 * 
	 * @return
	 */
	public static String getCurYearMonthDay_HH() {
		return getSimpleDateFormat(YEAR_MONTH_DAY_HH).format(new Date());
	}

	/**
	 * 返回当前小时：分：秒
	 * 
	 * @return
	 */
	public static String getCurHHmmss() {
		return getSimpleDateFormat(HH_MM_SS).format(new Date());
	}

	/**
	 * 根据格式解析字符串为日期
	 * 
	 * @param pattern
	 *            日期格式
	 * @param date
	 *            日期字符串
	 * @return
	 * @throws ParseException
	 */
	public static Date parseDateFromString(String pattern, String date) {
		try {
			return getSimpleDateFormat(pattern).parse(date);
		}catch (Exception e) {
			throw new RuntimeException(e.getMessage()+":日期解析错误!");
		}
	
	}

	/**
	 * 根据年月，取得该年月的最后一天
	 * 
	 * @param year
	 *            年(如:2000)
	 * @param month
	 *            月(如:01)
	 * @return
	 */
	public static int getLastDayByYearMonth(String year, String month) {
		Calendar cal = Calendar.getInstance();
 
			cal.setTime(parseDateFromString("yyyy-MM", year + "-" + month));
 
		cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.add(Calendar.MONTH, 1);
		cal.add(Calendar.DATE, -1);
		return cal.get(Calendar.DAY_OF_MONTH);
	}

	/**
	 * 获取下个月
	 * 
	 * @param month
	 * @return
	 */
	public static String getLastMonth(String month) {
		String[] s = month.split("-");
		int a = Integer.valueOf(s[1]);
		if (a > 0 && a <= 10) {
			month = s[0] + "-0" + (a - 1);
		} else {
			month = s[0] + "-" + (a - 1);
		}
		return month;
	}

	/**
	 * 将秒转换为时间格式 000：00：00
	 * 
	 * @param second
	 * @return
	 */
	public static String convertNumToTime(long second) {
		StringBuffer buf = new StringBuffer();
		long hour = second / 3600;
		if (hour < 10) {
			buf.append("0").append(hour).append(":");
		} else {
			buf.append(hour).append(":");
		}
		long remainSecond = second - hour * 60 * 60;
		long minute = remainSecond / 60;
		if (minute < 10) {
			buf.append("0").append(minute).append(":");
		} else {
			buf.append(minute).append(":");
		}
		remainSecond = remainSecond - minute * 60;
		if (remainSecond < 10) {
			buf.append("0").append(remainSecond);
		} else {
			buf.append(remainSecond);
		}

		return buf.toString();

	}
}
