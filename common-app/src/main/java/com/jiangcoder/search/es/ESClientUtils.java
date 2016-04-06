package com.jiangcoder.search.es;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

/**
 * es客户端工具 es java client内部维护一个链接池，只需要单例即可
 *
 * @author dinghongbo-ds1
 */
public class ESClientUtils {
	private static Logger logger = Logger.getLogger(ESClientUtils.class.getName());
	private static ResourceBundle rb = ResourceBundle.getBundle("es-product", Locale.getDefault());
	private static TransportClient client;
	private static ConcurrentHashMap<String, String> nodesByIP = new ConcurrentHashMap<String, String>();
	static {
		initTransportClient();
		ImmutableList<DiscoveryNode> connectedNodes = client.connectedNodes();
		logger.info("es connectedNodes=======" + connectedNodes.size());
		for (DiscoveryNode node : connectedNodes) {
			logger.info(node.address().toString());
			if (!node.getId().contains("transport")) {

				String address = node.address().toString();
				String ipPort = address.substring(address.indexOf("/") + 1, address.length() - 1);
				nodesByIP.put(ipPort, node.getId());
				if (ipPort.contains("9300")) {
					nodesByIP.put(node.getHostAddress(), node.getId());
				}
			}
		}
		ImmutableList<DiscoveryNode> listedNodes = client.listedNodes();
		logger.info("es listedNodes=======" + listedNodes.size());
		for (DiscoveryNode node : listedNodes) {
			logger.info(node.address().toString());
		}
	}

	/**
	 * 初始化transportclient
	 */
	private static void initTransportClient() {
		try {
			
			logger.info("cluster.name:{}"+ rb.getString("cluster.name"));
			logger.info("node.address:{}"+ rb.getString("node.address"));
			
			Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", rb.getString("cluster.name")).put("client.transport.sniff", true).build();
			client =  new TransportClient(settings);
			String addresses = rb.getString("node.address");
			String[] addressArr = addresses.split(",");
			for (String address : addressArr) {
				String[] ipAndPort = address.split(":");
				client.addTransportAddress(new InetSocketTransportAddress(ipAndPort[0], Integer.parseInt(ipAndPort[1])));
			}
		} catch (Exception e) {
			logger.error("init client fail", e);
		}
	}

	/**
	 * 获取TransportClient
	 *
	 * @return
	 */
	public static TransportClient getTransportClient() {
		return client;
	}

	/**
	 * 关闭客户端
	 */
	public static void close() {
		client.close();
	}

	public static String getNodeIdByIp(String ipPort) {
		return nodesByIP.get(ipPort);
	}

	public static void main(String[] args) {
		ESClientUtils.getTransportClient();
	}

}
