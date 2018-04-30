package controllers.site;

import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.http.util.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import controllers.util.RedisUtil;
import controllers.util.Utils;
import models.MessageBoard;
import models.Notice;
import play.Logger;
import play.db.DB;
import redis.clients.jedis.Jedis;

public class NoteMgr {
	private static DataSource ds = DB.getDataSource();
	private static QueryRunner allRun = new QueryRunner(ds);
	/**
	 *  根据参数查询公告
	 *  yyyyMMdd
	 */
	public static void listNoticeByParm() {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			Notice notice = Utils.getBody(Notice.class);
			
			StringBuffer wheres = new StringBuffer();
			
			// 判断是否根据id
			if(notice != null && notice.title != null && !TextUtils.isEmpty(notice.title.trim())) {
				wheres.append(" AND n.title like '%"+ Utils.getSecurityParm(notice.title.trim()) +"%'");
			}
			
			// 判断是否根据时间查询
			if(notice != null && notice.startTime != null && !TextUtils.isEmpty(notice.startTime.trim())){
				wheres.append(" AND n.add_time > " + Utils.getSecurityParm(notice.startTime+"000000"));
			}
			
			// 判断是否根据时间查询
			if(notice != null && notice.endTime != null && !TextUtils.isEmpty(notice.endTime.trim())){
				wheres.append(" AND n.add_time < " + Utils.getSecurityParm(notice.endTime+"000000"));
			}
			
			// 查询公告，放入redis
			String sql = "SELECT "
									+ "n.id,n.title,n.content,bu.memo "
							+ "FROM t_notice n "
							+ "INNER JOIN t_bizuser bu ON bu.id = n.bizuser_id "
							+ "WHERE n.deleteflag = 1 " + wheres 
							+ " ORDER BY n.add_time DESC";
			List list = allRun.query(sql, new MapListHandler());
			
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			
			
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("根据参数查询公告出错"+ e.getMessage());
		}
	}
	/**
	 * 评论公告
	 * {"user_id":"1","notice_id":"1","content":""}
	 */
	public static void commentNotic(){
		try {
			MessageBoard messageBoard = Utils.getBody(MessageBoard.class);
 
			messageBoard.deleteflag = "1";
			messageBoard.add_time = Utils.getCurrentTime();
			messageBoard.status = "0";
			messageBoard.save();
			
			// 返回数据
		} catch (Exception e) {
			Logger.error("评论公告错误"+ e.getMessage());
		}
	}
	
	
	/**
	 *  查询公告
	 */
	public static void listNotice() {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			JsonObject jsonObject = new JsonObject();
			
			// 如果缓存里不为空则取缓存中数据
			String resultStr = jedis.get("notice:json");
			if (resultStr == null) {
				// 查询公告，放入redis
				String sql = "SELECT "
										+ "n.id,n.title,n.content,bu.memo "
								+ "FROM t_notice n "
								+ "INNER JOIN t_bizuser bu ON bu.id = n.bizuser_id "
								+ "WHERE n.deleteflag = 1";
				List list = allRun.query(sql, new MapListHandler());
				resultStr = new Gson().toJsonTree(list).toString();
				// 放入redis
				jedis.set("notice:json", resultStr);
			}
			
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("查询公告出错"+ e.getMessage());
		}
	}
	
	/**
	 * 查看公告评论
	 * {"page":"1","notice_id":"1"}
	 */
	public static void listNoticMessage(){
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			MessageBoard messageBoard = Utils.getBody(MessageBoard.class);
			
			String sql = "SELECT "
									+ "m.content,m.add_time,u.user_name,u.head_img  "
								+ "FROM t_message_board m "
									+ " LEFT JOIN t_user u ON u.user_id = m.user_id "
									+ " JOIN (select id from t_message_board where deleteflag = 1 and notice_id = ? ORDER BY add_time DESC limit ?,10)  l on l.id = m.id"
								+ " ORDER BY m.add_time DESC";
			
			List list = allRun.query(sql,new MapListHandler(),messageBoard.notice_id,Utils.strMul(Utils.strSub(messageBoard.page + "", "1") + "", "10"));

			
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			// 返回数据
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("查看公告评论错误"+ e.getMessage());
		}
	}
}
