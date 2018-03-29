package controllers.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.Logger;
import play.Play;
import play.mvc.Controller;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisUtil {

	private static JedisPool jedisPool = null;

	/**
	 * 初始化Redis连接池
	 */
	static {
		try {
			String host = Play.configuration.getProperty("redisHost");
			int port = Integer.parseInt(Play.configuration.getProperty("redisPort"));
			int timeout = Integer.parseInt(Play.configuration.getProperty("redisTimeout"));
			String pwd = Play.configuration.getProperty("redisPwd");
			
			JedisPoolConfig config = new JedisPoolConfig();
			// 最大空闲连接数, 应用自己评估，不要超过ApsaraDB for Redis每个实例最大的连接数
			config.setMaxIdle(200);
			// 最大连接数, 应用自己评估，不要超过ApsaraDB for Redis每个实例最大的连接数
			config.setMaxTotal(9999);

			jedisPool = new JedisPool(config, host, port, timeout, pwd);
		} catch (Exception e) {
			Logger.error("初始化Redis连接池出错", e.getMessage());
		}
	}

	/**
	 * 获取Jedis实例
	 * 
	 * @return
	 */
	public synchronized static Jedis getJedis() {
		try {
			if (jedisPool != null) {
				Jedis resource = jedisPool.getResource();
				return resource;
			} else {
				return null;
			}
		} catch (Exception e) {
			Logger.error("获取redis连接实例出错", e.getMessage());
			return null;
		}
	}

	/**
	 * 释放jedis资源
	 * @param jedis
	 */
	public static void closeJedisPool(final Jedis jedis) {
		if (jedis != null) {
			jedis.close();
		}
	}
	
	/**
	 * 在redis获取map对象返回
	 * @return
	 */
	public static Map hmget(Jedis jedis,String key, String... fields){
		List list = jedis.hmget(key, fields);
		
		Map map = new HashMap<>();
		for (int i = 0; i < fields.length; i++) {
			map.put(fields[i], list.get(i));
		}
		return map;
	}
	/**
	 * 在redis获取map对象返回
	 * @return
	 */
	public static Map hmgetSplitParam(Jedis jedis,String key, String str){
		String[] fields = str.split(",");
		
		List list = jedis.hmget(key, fields);
		
		Map map = new HashMap<>();
		for (int i = 0; i < fields.length; i++) {
			map.put(fields[i], list.get(i));
		}
		return map;
	}
}
