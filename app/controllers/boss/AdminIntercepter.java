package controllers.boss;

import java.util.Map;

import com.google.gson.Gson;

import controllers.util.RedisUtil;
import play.cache.Cache;
import play.mvc.Before;
import play.mvc.Controller;
import redis.clients.jedis.Jedis;

public class AdminIntercepter extends Controller {

	/**
	 * 后台session拦截器 －－拦截器方法不能定义为public，但必须是static，并通过有效的拦截标记进行注解。
	 */
	@Before
	static void sessionIntercepter() {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			Map headers = request.headers;
			 String conString = headers.get("referer")+"";
			 
			if (!session.getAuthenticityToken().equals(jedis.get(session.getId()))) {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				LoginMgr.login("请登陆");
			}else if("null".equals(conString) || "".equals(conString)){
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				LoginMgr.login("权限不足");
			} else {
				// 获取操作人信息

				// 判断操作人角色
				if (!"0".equals(jedis.get(session.getId().toString()+":bizuser:roleId"))) {
					// 关闭链接
					RedisUtil.closeJedisPool(jedis);
					LoginMgr.login("权限越界，请重新登录");
				}
				// 一直在操作的时候，向后延迟5个小时时效。
				jedis.expire(session.getId().toString(), 60 * 60 * 5);
				jedis.expire(session.getId() + ":bizuser:info", 60 * 60 * 5);
				jedis.expire(session.getId().toString()+":bizuser:roleId", 60 * 60 * 5);
			}
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
		}
	}
}
