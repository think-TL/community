package controllers.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Query;
import javax.sql.DataSource;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.util.TextUtils;
import org.bouncycastle.jce.provider.JCEMac.MD5;
import org.bouncycastle.jce.provider.JDKISOSignature.MD5WithRSAEncryption;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.http.ProtocolType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.aliyuncs.sts.model.v20150401.AssumeRoleRequest;
import com.aliyuncs.sts.model.v20150401.AssumeRoleResponse;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.org.apache.bcel.internal.generic.NEW;
import com.taobao.api.ApiException;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.request.AlibabaAliqinFcSmsNumSendRequest;
import com.taobao.api.response.AlibabaAliqinFcSmsNumSendResponse;

import controllers.util.GsonUtil;
import controllers.util.HostType;
import controllers.util.HttpUtil;
import controllers.util.RedisUtil;
import controllers.util.Utils;
import models.MessageBoard;
import models.Notice;
import models.User;
import play.Logger;
import play.Play;
import play.cache.Cache;
import play.data.parsing.ApacheMultipartParser;
import play.db.DB;
import play.db.jpa.JPA;
import play.libs.Codec;
import play.libs.Crypto;
import play.libs.IO;
import play.libs.Time;
import play.mvc.Controller;
import play.mvc.With;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.SortingParams;

/**
 * 界面接口
 */
public class PageApi extends Controller {
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
			JsonObject jsonObject = new JsonObject();
			
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
				wheres.append(" AND n.add_time < " + Utils.afterNDayByParam(notice.endTime+"000000", 1));
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
			jsonObject.addProperty("code", "1");
			jsonObject.addProperty("msg", "根据参数查询公告成功");
			jsonObject.add("info", new Gson().toJsonTree(list));
			renderJSON(jsonObject);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("根据参数查询公告出错"+ e.getMessage());
			renderJSON(Utils.apiError());
		}
	}
	
	/**
	 * 评论公告
	 * {"user_id":"1","notice_id":"1","content":""}
	 */
	public static void commentNotic(){
		try {
			JsonObject jsonObject = new JsonObject();
			MessageBoard messageBoard = Utils.getBody(MessageBoard.class);
			
			messageBoard.deleteflag = "1";
			messageBoard.add_time = Utils.getCurrentTime();
			messageBoard.status = "0";
			messageBoard.save();
			
			// 返回数据
			jsonObject.addProperty("code", "1");
			jsonObject.addProperty("msg", "评论公告成功");
			renderJSON(jsonObject);
		} catch (Exception e) {
			Logger.error("评论公告错误"+ e.getMessage());
			renderJSON(Utils.apiError());
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
			jsonObject.addProperty("code", "1");
			jsonObject.addProperty("msg", "查询公告成功");
			
			JsonParser jsonParser = new JsonParser();
			JsonElement jsonElement = jsonParser.parse(resultStr);
			jsonObject.add("info", new Gson().toJsonTree(jsonElement));
			renderJSON(jsonObject);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("查询公告出错"+ e.getMessage());
			renderJSON(Utils.apiError());
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
			JsonObject jsonObject = new JsonObject();
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
			jsonObject.addProperty("code", "1");
			jsonObject.addProperty("msg", "查看公告评论成功");
			jsonObject.add("info", new Gson().toJsonTree(list));
			renderJSON(jsonObject);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("查看公告评论错误"+ e.getMessage());
			renderJSON(Utils.apiError());
		}
	}
	
	
}
