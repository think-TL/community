package controllers.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import controllers.util.RedisUtil;
import play.Logger;
import play.cache.Cache;
import play.exceptions.PlayException;
import play.mvc.Before;
import play.mvc.Controller;
import redis.clients.jedis.Jedis;

/**
 * 项目拦截器
 * 
 * @author Au QQ:594919495
 *
 */
public class ApiInterceptor extends Controller {
	/*
	 * 用户验证拦截器 －－拦截器方法不能定义为public，但必须是static，并通过有效的拦截标记进行注解。cache
	 * set会更新原来的key,cache add只新增
	 */
	@Before
	static void secure() {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			// 获取token 
			String token = params.get("token");
			// 获取redis中的token
			String cacheToken = jedis.get(token.substring(0,token.length()-4) + ":token");
			
			if (token.equals(cacheToken)) {
				// 验证通过，token过期时间刷新，30天过期
				jedis.expire(token.substring(0,token.length()-4) + ":token", 60 *60 * 24 * 30);
				jedis.expire(token.substring(0,token.length()-4) + ":aeskey", 60 *60 * 24 * 30);
			} else {
				JsonObject jsonObject = new JsonObject();
				jsonObject.addProperty("code", "509");
				jsonObject.addProperty("msg", "token验证失败,请重新登陆");
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				renderJSON(jsonObject);
			}
			
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
		} catch (Exception e) {
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty("code", "509");
			jsonObject.addProperty("msg", "token验证失败,请重新登陆");
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			renderJSON(jsonObject);
		} 
	}
}
