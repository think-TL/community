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
import models.Order;
import models.Resources;
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
 * 订单接口
 */
public class OrderApi extends Controller {
	private static DataSource ds = DB.getDataSource();
	private static QueryRunner allRun = new QueryRunner(ds);
	
	/**
	 * 卖家取消
	 * {"id":"40289fde6312304901631239706b0000","user_id":""}
	 */
	public static void merchantCancelOrder(){
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			JsonObject jsonObject = new JsonObject();
			Order order = Utils.getBody(Order.class);
			
			Map orderInfo = allRun.query("select shipping_status from t_order where deleteflag = 1 and order_status = 1 and shipping_status = 0 and id=? and maiuser_id = ? ", new MapHandler(),order.id,order.user_id);
			
			if(orderInfo == null) {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				// 返回数据
				jsonObject.addProperty("code", "0");
				jsonObject.addProperty("msg", "未找到相应订单");
				renderJSON(jsonObject);
			}
			
			 int updateRow = allRun.update("update t_order set order_status = 2 where deleteflag = 1 AND shipping_status = 0 AND id = ? AND maiuser_id = ?", order.id ,order.user_id);
			
			if(updateRow > 0) {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				// 返回数据
				jsonObject.addProperty("code", "1");
				jsonObject.addProperty("msg", "取消订单成功");
			}else {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				// 返回数据
				jsonObject.addProperty("code", "0");
				jsonObject.addProperty("msg", "未找到相应订单");
			}
			renderJSON(jsonObject);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("卖家发货错误"+ e.getMessage());
			renderJSON(Utils.apiError());
		}
	}
	
	
	/**
	 * 买家取消
	 * {"id":"40289fde6312304901631239706b0000","user_id":""}
	 */
	public static void consumeCancelOrder(){
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			JsonObject jsonObject = new JsonObject();
			Order order = Utils.getBody(Order.class);
			
			Map orderInfo = allRun.query("select shipping_status from t_order where deleteflag = 1 and order_status = 1 and id=? and user_id = ? ", new MapHandler(),order.id,order.user_id);
			
			if(orderInfo == null) {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				// 返回数据
				jsonObject.addProperty("code", "0");
				jsonObject.addProperty("msg", "未找到相应订单");
				renderJSON(jsonObject);
			}
			
			// 0 未发货
			int updateRow = 0;
			if("0".equals(orderInfo.get("shipping_status"))) {
				updateRow = allRun.update("update t_order set order_status = 2 where deleteflag = 1 AND order_status = 1 AND shipping_status = 0 AND id = ? AND user_id = ?", order.id ,order.user_id);
			}else {
				updateRow = allRun.update("update t_order set order_status = 2 where deleteflag = 1 AND order_status = 1 AND shipping_status = 1 AND id = ? AND user_id = ?", order.id ,order.user_id);
				if(updateRow > 0) {
					// 用户信用值减1
					User user = User.find("user_id = ? ", order.user_id).first();
					user.credit = Utils.strSub(user.credit, "1").toString();
					user.save();
					// redis更新
					Map<String, String> userInfo = Utils.beanToMap(user);
					jedis.hmset(userInfo.get("user_id") + ":user:info", userInfo);
				}
			}
			
			if(updateRow > 0) {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				// 返回数据
				jsonObject.addProperty("code", "1");
				jsonObject.addProperty("msg", "取消订单成功");
			}else {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				// 返回数据
				jsonObject.addProperty("code", "0");
				jsonObject.addProperty("msg", "未找到相应订单");
			}
			renderJSON(jsonObject);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("卖家发货错误"+ e.getMessage());
			renderJSON(Utils.apiError());
		}
	}
	
	
	/**
	 * 卖家发货
	 * {"id":"40289fde6312304901631239706b0000","user_id":""}
	 */
	public static void merchantSendGoods(){
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			JsonObject jsonObject = new JsonObject();
			Order order = Utils.getBody(Order.class);
			
			int updateRow = allRun.update("update t_order set shipping_status = 1 where deleteflag = 1 AND order_status = 1 AND id = ? AND maiuser_id = ?", order.id ,order.user_id);
			if(updateRow > 0) {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				// 返回数据
				jsonObject.addProperty("code", "1");
				jsonObject.addProperty("msg", "卖家发货成功");
			}else {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				// 返回数据
				jsonObject.addProperty("code", "0");
				jsonObject.addProperty("msg", "未找到相应订单");
			}
			
			renderJSON(jsonObject);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("卖家发货错误"+ e.getMessage());
			renderJSON(Utils.apiError());
		}
	}
	
	/**
	 * 买家订单查看
	 * {"page":"40289fde6312304901631239706b0000","user_id":""}
	 */
	public static void listMyOrderOnConsume(){
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			JsonObject jsonObject = new JsonObject();
			Order order = Utils.getBody(Order.class);
			
			// 判断用户是否认证
			List userInfo = jedis.hmget(order.user_id + ":user:info", "user_status","credit");
			try {
				if(!"3".equals(userInfo.get(0).toString())) {
					// 关闭链接
					RedisUtil.closeJedisPool(jedis);
					// 返回数据
					jsonObject.addProperty("code", "0");
					jsonObject.addProperty("msg", "请认证信息后再查看");
					renderJSON(jsonObject);
				}
			} catch (NullPointerException e) {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				// 返回数据
				jsonObject.addProperty("code", "0");
				jsonObject.addProperty("msg", "请认证信息后再查看");
				renderJSON(jsonObject);
			}
			StringBuffer wheres = new StringBuffer();
			
			wheres.append(" AND o.deleteflag = 1 AND (o.order_status = 1 or o.order_status = 3) AND o.user_id = '"+ Utils.getSecurityParm(order.user_id.trim()) +"'");
			// 判断是否根据类别
			if(order.orderno != null && !TextUtils.isEmpty(order.orderno.trim())) {
				wheres.append(" AND o.orderno = '"+ Utils.getSecurityParm(order.orderno.trim()) +"'");
			}
			
			// 判断是否根据名字
			if(order.resources_name != null && !TextUtils.isEmpty(order.resources_name.trim())) {
				wheres.append(" AND o.resources_name like '%"+ Utils.getSecurityParm(order.resources_name.trim()) +"%'");
			}
			
			// 判断是否根据时间查询
			if(order.startTime != null && !"".equals(order.startTime)){
				wheres.append(" AND o.itime > " + Utils.getSecurityParm(order.startTime.replaceAll("-", ""))+"000000");
			}
			// 判断是否根据时间查询
			if(order.endTime != null && !"".equals(order.endTime)){
				wheres.append(" AND o.itime <= " + Utils.afterNDayByParam(order.endTime.replaceAll("-", "")+"000000", 1));
			}
			
			// 查询公告，放入redis
			String sql = "SELECT "
					+ "o.order_status,o.shipping_status,o.id,o.orderno,o.itime,o.resources_price,o.resources_creditnumber,o.name, "
					+ "o.resources_name,o.resources_img1,o.resources_cmt,o.address,o.phone "
					+ "FROM  t_order o "
					+ "JOIN (select id from  t_order o where 1=1 "+wheres.toString()+  " order by itime desc limit ?,10 ) l ON l.id = o.id "
					+ "ORDER BY o.itime DESC" ;
			List list = allRun.query(sql, new MapListHandler(),Utils.strMul(Utils.strSub(order.page + "", "1") + "", "10"));
			
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			// 返回数据
			jsonObject.addProperty("code", "1");
			jsonObject.addProperty("msg", "买家查看订单成功");
			jsonObject.add("info", new Gson().toJsonTree(list));
			renderJSON(jsonObject);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("买家查看订单错误"+ e.getMessage());
			renderJSON(Utils.apiError());
		}
	}
	

	/**
	 * 商家订单查看
	 * {"page":"40289fde6312304901631239706b0000","user_id":""}
	 */
	public static void listMyOrderOnMerchant(){
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			JsonObject jsonObject = new JsonObject();
			Order order = Utils.getBody(Order.class);
			
			// 判断用户是否认证
			List userInfo = jedis.hmget(order.user_id + ":user:info", "user_status","credit");
			try {
				if(!"3".equals(userInfo.get(0).toString())) {
					// 关闭链接
					RedisUtil.closeJedisPool(jedis);
					// 返回数据
					jsonObject.addProperty("code", "0");
					jsonObject.addProperty("msg", "请认证信息后再查看");
					renderJSON(jsonObject);
				}
			} catch (NullPointerException e) {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				// 返回数据
				jsonObject.addProperty("code", "0");
				jsonObject.addProperty("msg", "请认证信息后再查看");
				renderJSON(jsonObject);
			}
			StringBuffer wheres = new StringBuffer();
			
			wheres.append(" AND o.deleteflag = 1 AND (o.order_status = 1 or o.order_status = 3) AND o.maiuser_id = '"+ Utils.getSecurityParm(order.user_id.trim()) +"'");
			// 判断是否根据类别
			if(order.orderno != null && !TextUtils.isEmpty(order.orderno.trim())) {
				wheres.append(" AND o.orderno = '"+ Utils.getSecurityParm(order.orderno.trim()) +"'");
			}
			
			// 判断是否根据名字
			if(order.resources_name != null && !TextUtils.isEmpty(order.resources_name.trim())) {
				wheres.append(" AND o.resources_name like '%"+ Utils.getSecurityParm(order.resources_name.trim()) +"%'");
			}
			
			// 判断是否根据时间查询
			if(order.startTime != null && !"".equals(order.startTime)){
				wheres.append(" AND o.itime > " + Utils.getSecurityParm(order.startTime.replaceAll("-", ""))+"000000");
			}
			// 判断是否根据时间查询
			if(order.endTime != null && !"".equals(order.endTime)){
				wheres.append(" AND o.itime <= " + Utils.afterNDayByParam(order.endTime.replaceAll("-", "")+"000000", 1));
			}
			
			// 查询公告，放入redis
			String sql = "SELECT "
									+ "o.order_status,o.shipping_status,o.id,o.orderno,o.itime,o.resources_price,o.resources_creditnumber,o.consumename, "
									+ "o.resources_name,o.resources_img1,o.resources_cmt "
							+ "FROM  t_order o "
							+ "JOIN (select id from  t_order o where 1=1 "+wheres.toString()+  " order by itime desc limit ?,10 ) l ON l.id = o.id "
							+ "ORDER BY o.itime DESC" ;
			List list = allRun.query(sql, new MapListHandler(),Utils.strMul(Utils.strSub(order.page + "", "1") + "", "10"));
			
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			// 返回数据
			jsonObject.addProperty("code", "1");
			jsonObject.addProperty("msg", "商家查看订单成功");
			jsonObject.add("info", new Gson().toJsonTree(list));
			renderJSON(jsonObject);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("商家查看订单错误"+ e.getMessage());
			renderJSON(Utils.apiError());
		}
	}
	
	/**
	 * 买家下单
	 * {"resources_id":"40289fde6312304901631239706b0000","user_id":""}
	 */
	public static void addOrder(){
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			JsonObject jsonObject = new JsonObject();
			Order order = Utils.getBody(Order.class);
			
			// 判断用户是否认证
			List userInfo = jedis.hmget(order.user_id + ":user:info", "user_status","credit");
			try {
				if(!"3".equals(userInfo.get(0).toString())) {
					// 关闭链接
					RedisUtil.closeJedisPool(jedis);
					// 返回数据
					jsonObject.addProperty("code", "0");
					jsonObject.addProperty("msg", "请认证信息后再下单");
					renderJSON(jsonObject);
				}
			} catch (NullPointerException e) {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				// 返回数据
				jsonObject.addProperty("code", "0");
				jsonObject.addProperty("msg", "请认证信息后再下单");
				renderJSON(jsonObject);
			}
			
			Resources resources = Resources.find("id = ? and deleteflag = 1", order.resources_id).first();
			
			if(resources == null) {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				// 返回数据
				jsonObject.addProperty("code", "0");
				jsonObject.addProperty("msg", "资源输入错误");
				renderJSON(jsonObject);
			}
			if(resources.user_id.equals(order.user_id)) {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				// 返回数据
				jsonObject.addProperty("code", "0");
				jsonObject.addProperty("msg", "买方与卖方不可为同一人");
				renderJSON(jsonObject);
			}
			
			// 判断用户信用是否足够
			if(Utils.strcompareTo(resources.credit_number,userInfo.get(1).toString())) {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				// 返回数据
				jsonObject.addProperty("code", "0");
				jsonObject.addProperty("msg", "你的信用值不足");
				renderJSON(jsonObject);
			}
			order.itime = Utils.getCurrentTime();
			order.orderno = System.currentTimeMillis() + "";
			order.resources_name = resources.name;
			order.resources_type_id = resources.resources_type_id;
			order.resources_cmt = resources.cmt;
			order.resources_img1 = resources.img1;
			order.resources_img2 = resources.img2;
			order.resources_img3 = resources.img3;
			order.resources_price = resources.price;
			order.resources_creditnumber = resources.credit_number;
			order.maiuser_id = resources.user_id;
			
			// 获取卖家信息
			List merchantUserInfo = jedis.hmget(resources.user_id + ":user:info", "iphone","address","realname");
			order.phone = merchantUserInfo.get(0).toString();
			order.address = merchantUserInfo.get(1).toString();
			order.name = merchantUserInfo.get(2).toString();
			
			// 获取买家信息
			List consumeUserInfo = jedis.hmget(order.user_id + ":user:info", "realname");
			order.consumename = consumeUserInfo.get(0).toString();
			
			order.order_status = "1";
			order.shipping_status = "0";
			order.deleteflag = "1";
			order.save();
			
			resources.deleteflag = "0";
			resources.save();
			
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			// 返回数据
			jsonObject.addProperty("code", "1");
			jsonObject.addProperty("msg", "下单成功");
			renderJSON(jsonObject);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("下单错误"+ e.getMessage());
			renderJSON(Utils.apiError());
		}
	}
}
