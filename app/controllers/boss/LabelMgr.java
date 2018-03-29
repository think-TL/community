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
import models.Resources_type;
import play.Logger;
import play.Play;
import play.data.validation.Valid;
import play.db.DB;
import play.db.jpa.JPA;
import play.mvc.Controller;
import play.mvc.With;
import redis.clients.jedis.Jedis;

/**
 * 分类管理
 * 
 * @author Au QQ:594919495
 *
 */
@With(BossIntercepter.class)
public class LabelMgr extends Controller {
	
	private static DataSource ds = DB.getDataSource();
	private static QueryRunner run = new QueryRunner(ds);
	
	/**
	 * 标签管理
	 */
	public static void listLabel() {
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
	 * 标签管理
	 */
	public static void listLabelData(int limit, int offset) {
		try {
			JsonObject jsonObject = new JsonObject();
			
			StringBuffer sql = new StringBuffer("select t.id,t.add_time,t.name from t_resources_type as t where t.deleteflag =1");
			
			// 获得数据
			List statisticsList = run.query(sql.toString() + " limit ?,?",new MapListHandler(),offset,limit);
			// 获得总条数
			List list = run.query("SELECT COUNT(t.id) as total FROM t_resources_type t WHERE t.deleteflag=1  ", new MapListHandler());
		
			Map map = (Map) list.get(0);
			jsonObject.addProperty("total", map.get("total").toString());
			jsonObject.add("rows", new Gson().toJsonTree(statisticsList));
			renderJSON(jsonObject);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("获取标签管理错误", e.getMessage());
		}
	}
	
	
	/**
	 *  增加标签
	 */
	public static void addLabel(Resources_type label) {
		System.out.println(label.name);
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			label.bizuser_id = jedis.hmget(session.getId() + ":bizuser:info", "id").get(0);
			label.add_time = Utils.getCurrentTime();
			label.deleteflag = "1";
			label = label.save();
			
			Map<String, String> info = Utils.beanToMap(label);
			// 放入redis内存
			jedis.hmset(label.id + ":label:json", info);
			
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			listLabel();
		} catch (Exception e) {
			// 关闭链接
			e.printStackTrace();
			RedisUtil.closeJedisPool(jedis);
			Logger.error("增加资源分类错误", e.getMessage());
		}
	}
	
	/**
	 * 修改标签
	 */
	public static void updateLabel(Resources_type label) {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			Resources_type oldLabel = Resources_type.find("id=? and deleteflag = 1", label.id).first();
			oldLabel.bizuser_id = jedis.hmget(session.getId() + ":bizuser:info", "id").get(0);
			oldLabel.upd_time = Utils.getCurrentTime();
			oldLabel.name = label.name;
			oldLabel = oldLabel.save();
			
			Map<String, String> info = Utils.beanToMap(oldLabel);
			// 放入redis内存
			jedis.hmset(oldLabel.id + ":label:json", info);
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			listLabel();
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("修改标签错误", e.getMessage());
		}
	}
	
	/**
	 * 删除标签
	 */
	public static void delLabel(Resources_type label) {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			Resources_type oldLabel = Resources_type.find("id=? and deleteflag = 1", label.id).first();
			oldLabel.bizuser_id = jedis.hmget(session.getId() + ":bizuser:info", "id").get(0);
			oldLabel.upd_time = Utils.getCurrentTime();
			oldLabel.deleteflag = "0";
			oldLabel = oldLabel.save();
			
			// 更新redis里的数据
			List list = Resources_type.find("deleteflag = 1").fetch();
			String resultStr = new Gson().toJsonTree(list).toString();

			Map<String, String> info = Utils.beanToMap(oldLabel);
			// 放入内存
			jedis.hmset(oldLabel.id + ":label:json", info);
			// 放入内存
			jedis.set("mf:label:json", resultStr);
			
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			listLabel();
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("修改标签错误", e.getMessage());
		}
	}
}
