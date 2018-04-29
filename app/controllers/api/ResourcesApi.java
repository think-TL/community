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
 * 资源接口
 */
public class ResourcesApi extends Controller {
	private static DataSource ds = DB.getDataSource();
	private static QueryRunner allRun = new QueryRunner(ds);
	
	/**
	 * 删除资源
	 * {""user_id":"","id":""}
	 */
	public static void delResources(){
		try {
			JsonObject jsonObject = new JsonObject();
			Resources resources = Utils.getBody(Resources.class);
			
			List list = allRun.query("select id from t_order where resources_id = ? and deleteflag = 1 and order_status = 1", new MapListHandler(),resources.id);
			if(list.size() > 0) {
				// 返回数据
				jsonObject.addProperty("code", "0");
				jsonObject.addProperty("msg", "有包含此资源的订单正在进行中");
				renderJSON(jsonObject);
			}
			
			allRun.update("update t_resources set deleteflag =0,upd_time = ? where id = ? and user_id = ? ",Utils.getCurrentTime(),resources.id,resources.user_id);
			
			// 返回数据
			jsonObject.addProperty("code", "1");
			jsonObject.addProperty("msg", "删除资源成功");
			renderJSON(jsonObject);
		} catch (Exception e) {
			Logger.error("删除资源错误"+ e.getMessage());
			renderJSON(Utils.apiError());
		}
	}
	
	
	/**
	 * 修改资源
	 * {"name":"资源1","resources_type_id":"1","cmt":"","img1":"","img2":"","img3":"","price":"","user_id":"","credit_number":""}
	 */
	public static void updateResources(){
		try {
			JsonObject jsonObject = new JsonObject();
			Resources resources = Utils.getBody(Resources.class);
			Resources oldResources = Resources.find("id=? and user_id = ?", resources.id,resources.user_id).first();
			
			List list = allRun.query("select id from t_order where resources_id = ? and deleteflag = 1 and order_status = 1", new MapListHandler(),resources.id);
			if(list.size() > 0) {
				// 返回数据
				jsonObject.addProperty("code", "0");
				jsonObject.addProperty("msg", "有包含此资源的订单正在进行中");
				renderJSON(jsonObject);
			}
			
			if(resources.name != null && !"".equals(resources.name.trim())) {
				oldResources.name = resources.name;
			}
			if(resources.cmt != null && !"".equals(resources.cmt.trim())) {
				oldResources.cmt = resources.cmt;
			}
			if(resources.img1 != null && !"".equals(resources.img1.trim())) {
				oldResources.img1 = resources.img1;
			}
			if(resources.img2 != null && !"".equals(resources.img2.trim())) {
				oldResources.img2 = resources.img2;
			}
			if(resources.img3 != null && !"".equals(resources.img3.trim())) {
				oldResources.img3 = resources.img3;
			}
			if(resources.price != 0.0) {
				oldResources.price = resources.price;
			}
			if(resources.credit_number != null && !"".equals(resources.credit_number.trim())) {
				oldResources.credit_number = resources.credit_number;
			}
			oldResources.upd_time = Utils.getCurrentTime();
			oldResources.save();
			
			// 返回数据
			jsonObject.addProperty("code", "1");
			jsonObject.addProperty("msg", "修改资源成功");
			renderJSON(jsonObject);
		} catch (Exception e) {
			Logger.error("修改资源错误"+ e.getMessage());
			renderJSON(Utils.apiError());
		}
	}
	
	/**
	 *  用户查看自己发布的资源
	 */
	public static void listResourcesByUser() {
		try {
			Resources resources = Utils.getBody(Resources.class);
			JsonObject jsonObject = new JsonObject();
			
			StringBuffer wheres = new StringBuffer();
			
			wheres.append(" AND deleteflag = 1 ");
			
			// 判断是否根据类别
			if(resources != null && resources.name != null && !TextUtils.isEmpty(resources.name.trim())) {
				wheres.append(" AND r.name like '%"+ Utils.getSecurityParm(resources.name.trim()) +"%'");
			}
			
			// 查询公告，放入redis
			String sql = "SELECT "
					+ " r.id,r.name,r.add_time,r.credit_number,r.price,r.img1 "
					+ "FROM t_resources r "
					+ "JOIN (select id from  t_resources r where 1=1 "+wheres.toString()+ "order by r.add_time desc limit ?,10 ) l ON l.id = r.id "
					+ "ORDER BY r.add_time desc ";
			List list = allRun.query(sql, new MapListHandler(),Utils.strMul(Utils.strSub(resources.page + "", "1") + "", "10"));
			
			jsonObject.addProperty("code", "1");
			jsonObject.addProperty("msg", "用户查看自己发布的资源成功");
			jsonObject.add("info", new Gson().toJsonTree(list));
			renderJSON(jsonObject);
		} catch (Exception e) {
			Logger.error("用户查看自己发布的资源出错"+ e.getMessage());
			renderJSON(Utils.apiError());
		}
	}
	/**
	 *  根据参数查询资源
	 */
	public static void listResourcesByParm() {
		try {
			Resources resources = Utils.getBody(Resources.class);
			JsonObject jsonObject = new JsonObject();
			
			StringBuffer wheres = new StringBuffer();
			StringBuffer orderBy = new StringBuffer();
			boolean ordrByBoolean = true;
			
			// 不可为空
			if(resources == null) {
				// 返回数据
				jsonObject.addProperty("code", "0");
				jsonObject.addProperty("msg", "参数为空"); 
				renderJSON(jsonObject);
			}
			wheres.append(" AND deleteflag = 1 ");
			
			// 判断是否根据类别
			if(resources.resources_type_id != null && !TextUtils.isEmpty(resources.resources_type_id.trim())) {
				wheres.append(" AND r.resources_type_id = '"+ Utils.getSecurityParm(resources.resources_type_id.trim()) +"'");
			}
			
			// 判断时间根据升序还是降序排列
			if(ordrByBoolean && "0".equals(resources.order_by_time)) {
				orderBy.append(" order by r.add_time ASC ");
				ordrByBoolean = false;
			}else if(ordrByBoolean &&  "1".equals(resources.order_by_time)) {
				orderBy.append(" order by r.add_time DESC ");
				ordrByBoolean = false;
			}
			
			// 判断价格根据升序还是降序排列
			if(ordrByBoolean &&  "0".equals(resources.order_by_price)) {
				orderBy.append(" order by r.price ASC ");
				ordrByBoolean = false;
			}else if(ordrByBoolean &&  "1".equals(resources.order_by_price)) {
				orderBy.append(" order by r.price DESC ");
				ordrByBoolean = false;
			}
			
			// 判断信用根据升序还是降序排列
			if(ordrByBoolean &&  "0".equals(resources.order_by_credit)) {
				orderBy.append(" order by r.credit_number ASC ");
				ordrByBoolean = false;
			}else if(ordrByBoolean &&  "1".equals(resources.order_by_credit)) {
				orderBy.append(" order by r.credit_number DESC ");
				ordrByBoolean = false;
			}
			
			// 查询公告，放入redis
			String sql = "SELECT "
									+ " r.id,r.name,r.add_time,r.credit_number,r.price,r.img1 "
							+ "FROM t_resources r "
							+ "JOIN (select id from  t_resources r where 1=1 "+wheres.toString()+ orderBy.toString() + " limit ?,10 ) l ON l.id = r.id "
							+ orderBy.toString() ;
			List list = allRun.query(sql, new MapListHandler(),Utils.strMul(Utils.strSub(resources.page + "", "1") + "", "10"));
			
			jsonObject.addProperty("code", "1");
			jsonObject.addProperty("msg", "根据参数查询资源成功");
			jsonObject.add("info", new Gson().toJsonTree(list));
			renderJSON(jsonObject);
		} catch (Exception e) {
			Logger.error("根据参数查询资源出错"+ e.getMessage());
			renderJSON(Utils.apiError());
		}
	}
	
	/**
	 * 发布资源
	 * {"name":"资源1","resources_type_id":"1","cmt":"","img1":"","img2":"","img3":"","price":"","user_id":"","credit_number":""}
	 */
	public static void addResources(){
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			JsonObject jsonObject = new JsonObject();
			Resources resources = Utils.getBody(Resources.class);
			
			// 如果缓存里不为空则取缓存中数据
			List userInfo = jedis.hmget(resources.user_id + ":user:info", "user_status");
			try {
				if(!"3".equals(userInfo.get(0).toString())) {
					// 关闭链接
					RedisUtil.closeJedisPool(jedis);
					// 返回数据
					jsonObject.addProperty("code", "0");
					jsonObject.addProperty("msg", "请认证信息后再发布");
					renderJSON(jsonObject);
				}
			} catch (NullPointerException e) {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				// 返回数据
				jsonObject.addProperty("code", "0");
				jsonObject.addProperty("msg", "请认证信息后再发布");
				renderJSON(jsonObject);
			}
			
			
			resources.deleteflag = "1";
			resources.add_time = Utils.getCurrentTime();
			resources.save();
			
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			// 返回数据
			jsonObject.addProperty("code", "1");
			jsonObject.addProperty("msg", "发布资源成功");
			renderJSON(jsonObject);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("发布资源错误"+ e.getMessage());
			renderJSON(Utils.apiError());
		}
	}
	
	/**
	 * 展示资源类型
	 */
	public static void listResourcesType(){
		try {
			JsonObject jsonObject = new JsonObject();
			
			List resourcesType = allRun.query("select id,name from t_resources_type where deleteflag = 1", new MapListHandler());
			
			// 返回数据
			jsonObject.addProperty("code", "1");
			jsonObject.addProperty("msg", "展示资源类型成功");
			jsonObject.add("info", new Gson().toJsonTree(resourcesType));
			renderJSON(jsonObject);
		} catch (Exception e) {
			Logger.error("展示资源类型s错误"+ e.getMessage());
			renderJSON(Utils.apiError());
		}
	}
}
