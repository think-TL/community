package controllers.boss;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import models.MessageBoard;
import play.Logger;
import play.Play;
import play.data.validation.Valid;
import play.db.DB;
import play.mvc.Controller;
import play.mvc.With;
import redis.clients.jedis.Jedis;

/**
 *  评论管理
 * 
 * @author Au QQ:594919495
 *
 */
@With(BossIntercepter.class)
public class OpinionMgr extends Controller {
	
	private static DataSource ds = DB.getDataSource();
	private static QueryRunner run = new QueryRunner(ds);
	
	/**
	 *  评论管理管理
	 */
	public static void listOpinion() {
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
	 * 评论管理
	 */
	public static void listOpinionData(int limit, int offset,String status,String startTime,String endTime) {
		try {
			JsonObject jsonObject = new JsonObject();
			
			StringBuffer sql = new StringBuffer("SELECT o.id,u.user_name,u.sex,o.status,o.content,o.add_time,t.title FROM t_message_board o INNER JOIN t_user u ON u.user_id = o.user_id JOIN t_notice t on t.id = o.notice_id  WHERE 1 = 1");
			
			StringBuffer wheres = new StringBuffer();
			// 判断是否根据状态查找
			if(status != null && !"".equals(status) && !"-1".equals(status)){
				wheres.append(" and o.status = '" + Utils.getSecurityParm(status) + "'");
			}
			
			// 判断是否根据时间查询
			if(startTime != null && !"".equals(startTime)){
				wheres.append(" AND o.add_time > " + Utils.getSecurityParm(startTime.replaceAll("-", "")));
			}
			
			// 判断是否根据时间查询
			if(endTime != null && !"".equals(endTime)){
				wheres.append(" AND o.add_time < " + Utils.getSecurityParm(endTime.replaceAll("-", "")));
			}
			// 获得数据
			List statisticsList = run.query(sql.toString() + wheres.toString() + " limit ?,?",new MapListHandler(),offset,limit);
			// 获得总条数
			List list = run.query("SELECT COUNT(o.id) as total FROM t_message_board o WHERE 1 = 1 " + wheres.toString(), new MapListHandler());
		
			Map map = (Map) list.get(0);
			jsonObject.addProperty("total", map.get("total").toString());
			jsonObject.add("rows", new Gson().toJsonTree(statisticsList));
			renderJSON(jsonObject);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("评论管理错误", e.getMessage());
		}
	}
	
	/**
	 * 评论阅读
	 */
	public static void updateStatusById(MessageBoard messageBoard) {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			MessageBoard m =messageBoard .find("id = ?", messageBoard.id).first();
			m.status = "0";
			m.upd_time = Utils.getCurrentTime();
			m.bizuser_id = jedis.hmget(session.getId() + ":bizuser:info", "id").get(0);
			m.save();
			
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			listOpinion();
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("处理意见反馈错误", e.getMessage());
		}
	}
	
	
}
