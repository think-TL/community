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
import models.Opinion;
import play.Logger;
import play.Play;
import play.data.validation.Valid;
import play.db.DB;
import play.mvc.Controller;
import play.mvc.With;
import redis.clients.jedis.Jedis;

/**
 * 意见反馈
 *
 */
@With(BossIntercepter.class)
public class FeekBackMgr extends Controller {
	
	private static DataSource ds = DB.getDataSource();
	private static QueryRunner run = new QueryRunner(ds);
	
	/**
	 * 意见反馈管理
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
	 * 意见反馈管理
	 */
	public static void listOpinionData(int limit, int offset,String status,String startTime,String endTime) {
		try {
			JsonObject jsonObject = new JsonObject();
			
			StringBuffer sql = new StringBuffer("SELECT u.idnum,u.getuicid,o.id,o.content,o.user_nickname,o.user_sex,o.user_id,o.status,o.itime FROM t_opinion o INNER JOIN t_user u ON u.id = o.user_id WHERE 1 = 1 ");
			
			StringBuffer wheres = new StringBuffer();
			// 判断是否根据状态查找
			if(status != null && !"".equals(status) && !"-1".equals(status)){
				wheres.append(" and o.status = '" + Utils.getSecurityParm(status) + "'");
			}
			
			// 判断是否根据时间查询
			if(startTime != null && !"".equals(startTime)){
				wheres.append(" AND o.itime > " + Utils.getSecurityParm(startTime.replaceAll("-", ""))+"000000");
			}
			
			// 判断是否根据时间查询
			if(endTime != null && !"".equals(endTime)){
				wheres.append(" AND o.itime < " + Utils.getSecurityParm(endTime.replaceAll("-", ""))+"000000");
			}
			
			// 获得数据
			List statisticsList = run.query(sql.toString() + wheres.toString() + " order by itime desc limit ?,?",new MapListHandler(),offset,limit);
			// 获得总条数
			List list = run.query("SELECT COUNT(o.id) as total FROM t_opinion o WHERE 1 = 1 " + wheres.toString(), new MapListHandler());
		
			Map map = (Map) list.get(0);
			jsonObject.addProperty("total", map.get("total").toString());
			jsonObject.add("rows", new Gson().toJsonTree(statisticsList));
			renderJSON(jsonObject);
		} catch (Exception e) {
			Logger.error("获取意见反馈管理错误", e.getMessage());
		}
	}
	
	/**
	 * 处理意见反馈
	 */
	public static void updateStatusById(String id) {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			Opinion o = Opinion.find("id = ?", id).first();
			o.status = "0";
			o.utime = Utils.getCurrentTime();
			o.bizuser_id = jedis.hmget(session.getId() + ":bizuser:info", "id").get(0);
			o.save();
			
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
