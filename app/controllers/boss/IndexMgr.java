package controllers.boss;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;

import com.gexin.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.corba.se.impl.protocol.giopmsgheaders.Message;

import controllers.util.RedisUtil;
import models.Bizuser;
import models.User;
import play.Logger;
import play.data.validation.Valid;
import play.db.DB;
import play.mvc.Controller;
import play.mvc.With;
import redis.clients.jedis.Jedis;

/**
 * 后台管理首页
 * 
 * @author Au QQ:594919495
 *
 */
@With(BossIntercepter.class)
public class IndexMgr extends Controller {
	
	private static DataSource ds = DB.getDataSource();
	private static QueryRunner run = new QueryRunner(ds);
	
	
	/**
	 * 空白界面
	 */
	public static void index(Bizuser bizuser) {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			//List permissionList = getPermission(jedis);
			
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			//render(permissionList);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("后台首页进入失败");
		}
	}


	/**
	 *  获取权限列表
	 * @param bizuser
	 * @return
	 * @throws SQLException
	 */
	public static List getPermission(Jedis jedis) {
		List permissionList  = null;
		try {
			// 获得登录人的角色
			String sql1 = "select role_id from t_bizuser_role where user_id = ? and deleteflag = 1";
			List list = run.query(sql1, new MapListHandler(),jedis.hmget(session.getId() + ":bizuser:info", "id").get(0));
			Map map = (Map)list.get(0);
			// 将登陆角色放入redis
			jedis.set(session.getId().toString()+":bizuser:roleId", map.get("role_id").toString());
			// 有效时期30分钟
			jedis.expire(session.getId().toString()+":bizuser:roleId", 1800);
			
			// 查找登录角色拥有的权限
			String sql = " SELECT p.url,p.name,p.permission_id,p.id,p.activeflag,p.img "+
								" FROM t_role_permission rp LEFT JOIN t_permission p ON p.id = rp.permission_id"+
								" WHERE rp.role_id = ? and rp.deleteflag = '1' order by sequence+0 asc ";
			permissionList = run.query(sql,new MapListHandler(),map.get("role_id"));
			System.out.println(permissionList);
		} catch (Exception e) {
			Logger.error("获取权限列表失败");
		}
		return permissionList;
	}
	
}
