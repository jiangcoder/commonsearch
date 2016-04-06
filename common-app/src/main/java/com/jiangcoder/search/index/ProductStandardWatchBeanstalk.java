package com.jiangcoder.search.index;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.t3.client.beanstalk.BeanstalkException;
import org.t3.client.beanstalk.BeanstalkJob;
import org.t3.client.beanstalk.StringSerializer;
import org.t3.client.beanstalk.pool.BeanstalkClient;
import org.t3.client.beanstalk.pool.BeanstalkPool;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;

public class ProductStandardWatchBeanstalk {
	private static  BeanstalkClient watchBeansConnect;
	protected static Logger logger = LoggerFactory.getLogger(ProductStandardWatchBeanstalk.class);
	private  static String watch = "prod_sku1";
	public static  String ip;
	private  static int beanstalkPort;
	private  static ESIndex indexApi = new ESIndex();
	static {
		beanstalkPort = 11300;
		ip = "10.58.50.103";
		try {
			watchBeansConnect = BeanstalkPool.getClient(ip, beanstalkPort, watch, new StringSerializer());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void run() {
		if (ip == null)
			return;
		BeanstalkJob job = null;
		try {
			job = watchBeansConnect.reserve(5);
			if (job != null) {
				watchBeansConnect.deleteJob(job);
				BasicDBList jobData = (BasicDBList) JSON.parse((String) job.get());
				if (!EmptyUtil.isEmpty(jobData)) {
					BasicDBObject basicDBObject = (BasicDBObject) jobData.get(0);
					String type = basicDBObject.getString("type");
					if (StringUtils.isNotEmpty(type)) {
						if (type.equals("product")) {
							indexApi.createIndex(jobData);
						}
					} else {
						logger.info("the type is empty,that is wrong");
					}
				}
			}
		} catch (BeanstalkException e) {
			logger.error(e.toString());
		}
	}

	public static void main(String[] args) {
			while(true){
				run();
			}
	}

}
