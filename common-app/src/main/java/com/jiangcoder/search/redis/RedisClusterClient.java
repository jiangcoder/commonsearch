package com.jiangcoder.search.redis;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Strings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;

public class RedisClusterClient {
	private static Logger logger = LoggerFactory.getLogger(RedisClusterClient.class);
	private static Map<String, JedisCluster> clusters = new HashMap<String, JedisCluster>();

	private enum redisModule {
		es, def
	};

	public RedisClusterClient() {
	}

	static {
		init();
	}

	public static void init() {
		String ips = "10.58.47.139:7000,10.58.47.139:7001,10.58.47.139:7002,10.58.47.139:7003,10.58.47.139:7004,10.58.47.139:7005";
		if (Strings.isNullOrEmpty(ips)) {
			logger.error("Redis cluster is not config!!!!");
		}
		String[] ipPorts = ips.split(",");
		Set<HostAndPort> nodes = new HashSet<HostAndPort>();
		for (String item : ipPorts) {
			String[] addrItem = item.split(":");
			nodes.add(new HostAndPort(addrItem[0], Integer.parseInt(addrItem[1])));
		}

		JedisCluster es = new JedisCluster(nodes, 5000, poolConfig(redisModule.es));
		JedisCluster def = new JedisCluster(nodes, 5000, poolConfig(redisModule.def));

		clusters.put("es", es);
		clusters.put("default", def);
	}

	private static JedisPoolConfig poolConfig(redisModule module) {
		JedisPoolConfig conf = new JedisPoolConfig();
		switch (module) {
		case es:
			conf.setMaxWaitMillis(10000);
			conf.setMaxIdle(60000);
			conf.setMaxTotal(6000);
		case def:
			conf.setMaxWaitMillis(10000);
			conf.setMaxIdle(60000);
			conf.setMaxTotal(6000);
		}
		return conf;
	}

	public JedisCluster es() {
		return clusters.get("es");
	}

	public JedisCluster def() {
		return clusters.get("default");
	}

	public static void main(String[] args) {
	}
}