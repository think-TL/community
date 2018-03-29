package controllers.boss;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;

import controllers.util.RedisUtil;
import play.cache.Cache;
import play.db.DB;
import play.mvc.Before;
import play.mvc.Controller;
import redis.clients.jedis.Jedis;

public class BossIntercepter extends Controller {

	/**
	 * 后台session拦截器 －－拦截器方法不能定义为public，但必须是static，并通过有效的拦截标记进行注解。
	 */
	@Before
	static void sessionIntercepter() {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			DataSource ds = DB.getDataSource();
			QueryRunner run = new QueryRunner(ds);
			
			// 获取当前url的权限
			List list = run.query("SELECT "
								+ "rp.role_id as roleId "
							+ "FROM t_permission p "
							+ "INNER JOIN t_role_permission rp "
							+ "ON rp.permission_id = p.id "
							+ "WHERE p.url = ?", new MapListHandler(),request.path);
			
			int check = 0;
			for (int i = 0; i < list.size(); i++) {
				check = 1;
				Map map = (Map) list.get(i);
				// 判断是否和当前角色id一致
				String roleId= map.get("roleId") + "";
				
				// 登录角色id与url权限id一致
				if(roleId.equals(jedis.get(session.getId().toString()+":bizuser:roleId"))){
					check = 2;
					break;
				}
			}
			
			if(check == 1){
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				LoginMgr.login("权限不足！");
			}
			
			// 获取请求头
			Map headers = request.headers;
			// 获取referer 验证
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
				// 一直在操作的时候，向后延迟5个小时时效。
				jedis.expire(session.getId().toString(), 60 * 60 * 6);
				jedis.expire(session.getId() + ":bizuser:info", 60 * 60 * 6);
				jedis.expire(session.getId().toString()+":bizuser:roleId", 60 *60 * 6);
			}
			
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
		}
	}
}
