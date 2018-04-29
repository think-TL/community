package controllers.boss;

import java.util.List;

import controllers.util.RedisUtil;
import play.mvc.Controller;
import redis.clients.jedis.Jedis;

public class CheckUserMgr extends Controller{
   
	/**
	 * 用户搜索
	 */
	public static void listUserV2(String msg) {
		// 获取jedis
		System.out.println(msg);
		Jedis jedis = RedisUtil.getJedis();
		List permissionList = null;
		try {
			permissionList = IndexMgr.getPermission(jedis);
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
		}
		render(permissionList,msg);
	}
	
	
	
	
}
