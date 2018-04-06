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
 * 用户信息接口
 */
public class UserApi extends Controller {
	private static DataSource ds = DB.getDataSource();
	private static QueryRunner allRun = new QueryRunner(ds);
	
	/**
	 * 用户传资料认证
	 */
	public static void userAuthentication() {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			JsonObject jsonObject = new JsonObject();
			User user = Utils.getBody(User.class);
			
			User oldUser = user.find("user_id = ? ", user.user_id).first();
			
			// 判断用户是否 未审核 或为 审核失败状态
			if(oldUser.user_status == 1 || oldUser.user_status == 3) {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				jsonObject.addProperty("code", "0");
				jsonObject.addProperty("msg", "用户处于不可传资料审核状态");
				renderJSON(jsonObject);
			}
			
			oldUser.authentication_url1 = user.authentication_url1;
			oldUser.authentication_url2 = user.authentication_url2;
			oldUser.user_status = 1;
			oldUser.save();
			// 更新用户表信息到redis
			// 把user对象转成map
			Map<String, String> userInfo = Utils.beanToMap(oldUser);
			// 放入redis
			jedis.hmset(userInfo.get("user_id") + ":user:info", userInfo);
						
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			jsonObject.addProperty("code", "1");
			jsonObject.addProperty("msg", "用户传资料认证成功");
			renderJSON(jsonObject);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("用户传资料认证出错"+ e.getMessage());
			renderJSON(Utils.apiError());
		}
	}
	
	/**
	 * 查询个人信息
	 *  用户名，密码，头像，邮箱 性别， 身份证号，地址 昵称 生日  ，信用值等级，信用值具体信息
	 */
	public static void getUserByUserId() {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			JsonObject jsonObject = new JsonObject();
			User user = Utils.getBody(User.class);
			
			String userinfo = "user_name,user_password,head_img,emali,sex,id_card,address,realname,credit,credit_cmt,birth";
			
			// 如果缓存里不为空则取缓存中数据
			Map map = RedisUtil.hmgetSplitParam(jedis, user.user_id + ":user:info", userinfo);
			if (map != null) {

				jsonObject.add("info", new Gson().toJsonTree(map));
			} else {
				user = User.find("user_id=?", user.user_id).first();
				jsonObject.add("info", new Gson().toJsonTree(user));
			}
						
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			jsonObject.addProperty("code", "1");
			jsonObject.addProperty("msg", "查询个人信息成功");
			jsonObject.add("info", new Gson().toJsonTree(map));
			renderJSON(jsonObject);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("查询个人信息出错"+ e.getMessage());
			renderJSON(Utils.apiError());
		}
	}
	
	/**
	 * 修改密码
	 */
	public static void updateUserPwd() {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			JsonObject jsonObject = new JsonObject();
			User user = Utils.getBody(User.class);
			
			// 简单判断规范，不做详细判断，因为需求不清楚
			if(user.user_password == null || TextUtils.isEmpty(user.user_password.trim())) {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				jsonObject.addProperty("code", "0");
				jsonObject.addProperty("msg", "密码不可为空");
				renderJSON(jsonObject);
			}
			
			User oldUser = user.find("user_id = ? AND user_password = ? ", user.user_id,user.old_pwd).first();
			// 判断旧密码是否正确
			if(oldUser == null) {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				jsonObject.addProperty("code", "0");
				jsonObject.addProperty("msg", "旧密码输入错误");
				renderJSON(jsonObject);
			}
			
			oldUser.user_password = user.user_password;
			oldUser.upd_time = Utils.getCurrentTime();
			oldUser.save();
			
			// 更新用户表信息到redis
			// 把user对象转成map
			Map<String, String> userInfo = Utils.beanToMap(oldUser);
			// 放入redis
			jedis.hmset(userInfo.get("user_id") + ":user:info", userInfo);
						
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			jsonObject.addProperty("code", "1");
			jsonObject.addProperty("msg", "修改密码成功");
			renderJSON(jsonObject);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("修改密码出错"+ e.getMessage());
			renderJSON(Utils.apiError());
		}
	}
	
	/**
	 * 修改用户信息
	 *  用户名，头像，邮 箱 性别，生日  
	 */
	public static void updateUserInfo() {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			JsonObject jsonObject = new JsonObject();
			User user = Utils.getBody(User.class);
			
			
			User oldUser = user.find("user_id = ? ", user.user_id).first();

			if(user.user_name != null && !TextUtils.isEmpty(user.user_name)) {
				// 修改用户名前判断是否存在
				oldUser.user_name = user.user_name.replaceAll("[\\ud800\\udc00-\\udbff\\udfff\\ud800-\\udfff]", "");
				Map map = allRun.query("SELECT user_id FROM t_user WHERE user_name = ? AND user_id <> ? ", new MapHandler(), oldUser.user_name,oldUser.user_id);
				// 不为空则代表已注册
				if(map != null){
					// 关闭链接
					RedisUtil.closeJedisPool(jedis);
					jsonObject.addProperty("code", "0");
					jsonObject.addProperty("msg", "用户名已被注册，请更换");
					renderJSON(jsonObject);
				}
			}
			
			if(user.head_img != null && !TextUtils.isEmpty(user.head_img)) {
				oldUser.head_img = user.head_img;
			}
			
			if(user.birth != null && !TextUtils.isEmpty(user.birth)) {
				oldUser.birth = user.birth;
			}
			
			if(user.emali != null && !TextUtils.isEmpty(user.emali)) {
				oldUser.emali = user.emali.replaceAll("[\\ud800\\udc00-\\udbff\\udfff\\ud800-\\udfff]", "");
			}
			
			if(user.sex == 0) {
				oldUser.sex = 0;
			}else {
				oldUser.sex = 1;
			}
			
			oldUser.upd_time = Utils.getCurrentTime();
			oldUser.save();
			
			// 更新用户表信息到redis
			// 把user对象转成map
			Map<String, String> userInfo = Utils.beanToMap(oldUser);
			// 放入redis
			jedis.hmset(userInfo.get("user_id") + ":user:info", userInfo);
			
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			jsonObject.addProperty("code", "1");
			jsonObject.addProperty("msg", "修改用户信息成功");
			jsonObject.add("info", new Gson().toJsonTree(oldUser));
			renderJSON(jsonObject);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("修改用户信息出错"+ e.getMessage());
			renderJSON(Utils.apiError());
		}
	}
	
	
	
}
