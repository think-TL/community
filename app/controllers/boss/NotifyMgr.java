package controllers.boss;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.EntityTransaction;
import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.corba.se.impl.protocol.giopmsgheaders.Message;

import controllers.util.RedisUtil;
import controllers.util.Utils;
import models.Notice;
import play.Logger;
import play.Play;
import play.data.validation.Valid;
import play.db.DB;
import play.db.jpa.JPA;
import play.mvc.Controller;
import play.mvc.With;
import redis.clients.jedis.Jedis;

/**
 * 公告管理
 * 
 * @author Au QQ:594919495
 *
 */
@With(BossIntercepter.class)
public class NotifyMgr extends Controller {
	
	private static DataSource ds = DB.getDataSource();
	private static QueryRunner run = new QueryRunner(ds);
	
	/**
	 * 公告管理
	 */
	public static void listNotify() {
		// 获取jedis
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
		render(permissionList);
	}
	
	/**
	 * 公告管理
	 */
	public static void listNotifyData(int limit, int offset,String text_search) {
		System.out.println(text_search);
		try {
			JsonObject jsonObject = new JsonObject();
			
			StringBuffer sql = new StringBuffer("SELECT n.content,n.id,n.add_time,n.title FROM t_notice n WHERE n.deleteflag=1");
			
			if(text_search != null && !"".equals(text_search)){
				sql.append(" and n.title like ?'" + Utils.getSecurityParm(text_search) + "'");
			}
			
			// 获得数据
			List statisticsList = run.query(sql.toString() + "  limit ?,?",new MapListHandler(),offset,limit);
			// 获得总条数
			List list = run.query("SELECT COUNT(n.id) as total FROM t_notice n WHERE n.deleteflag=1  ", new MapListHandler());
		
			Map map = (Map) list.get(0);
			jsonObject.addProperty("total", map.get("total").toString());
			jsonObject.add("rows", new Gson().toJsonTree(statisticsList));
			renderJSON(jsonObject);
		} catch (Exception e) {
			Logger.error("获取公告管理错误", e.getMessage());
		}
	}
	
	
	/**
	 *  增加公告管理
	 */
	public static void addNotify(Notice notice) {
		
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			notice.bizuser_id = jedis.hmget(session.getId() + ":bizuser:info", "id").get(0);
			System.out.println(jedis.hmget(session.getId() + ":bizuser:info", "id").get(0));
			notice.add_time = Utils.getCurrentTime();
			notice.deleteflag = "1";
			notice = notice.save();
			
			// 更新redis里的数据
			List list = Notice.find("deleteflag = 1").fetch();
			String resultStr = new Gson().toJsonTree(list).toString();

			// 放入内存
			jedis.set("mf:notify:json", resultStr);
			
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			listNotify();
		} catch (Exception e) {
			e.printStackTrace();
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("增加公告错误", e.getMessage());
		}
	}
	
	/**
	 * 更改公告管理
	 */
	public static void updateNotify(Notice notice) {
		
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			Notice oldNotify = Notice.find("id=? and deleteflag = 1", notice.id).first();
			oldNotify.bizuser_id = jedis.hmget(session.getId() + ":bizuser:info", "id").get(0);
			oldNotify.upd_time = Utils.getCurrentTime();
			oldNotify.content = notice.content;
			oldNotify.title = notice.title;
			oldNotify = oldNotify.save();
			
			// 更新redis里的数据
			List list = Notice.find("deleteflag = 1").fetch();
			String resultStr = new Gson().toJsonTree(list).toString();
			// 放入内存
			jedis.set("mf:notify:json", resultStr);
			
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			listNotify();
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("修改公告错误", e.getMessage());
		}
	}
	/**
	 * 删除公告
	 */
	public static void delNotify(Notice notice) {
		System.out.println(notice);
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			
			Notice oldNotify = Notice.find("id=? and deleteflag = 1", notice.id).first();
			oldNotify.bizuser_id = jedis.hmget(session.getId() + ":bizuser:info", "id").get(0);
			oldNotify.add_time = Utils.getCurrentTime();
			oldNotify.deleteflag = "0";
			oldNotify = oldNotify.save();
			
			// 更新redis里的数据
			List list = Notice.find("deleteflag = 1").fetch();
			String resultStr = new Gson().toJsonTree(list).toString();
			// 放入内存
			jedis.set("mf:notify:json", resultStr);
			
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			listNotify();
		} catch (Exception e) {
			e.printStackTrace();
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("删除公告错误", e.getMessage());
		}
	}
	
	
}
