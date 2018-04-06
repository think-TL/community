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
 */
public class IndexApi extends Controller {
	private static DataSource ds = DB.getDataSource();
	private static QueryRunner allRun = new QueryRunner(ds);
	
	/**
	 * 用户注册
	 * 传入 用户名，密码，头像，邮箱，性别，身份证号，地址 真实姓名     生日
	 */
	public static void register() {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			JsonObject jsonObject = new JsonObject();
			User user = Utils.getBody(User.class);
			
			User userInfo = new User();
			// 先过滤一次
			userInfo.user_name = user.user_name.replaceAll("[\\ud800\\udc00-\\udbff\\udfff\\ud800-\\udfff]", "");
			
			Map map = allRun.query("SELECT user_id FROM t_user WHERE user_name = ?", new MapHandler(), userInfo.user_name);
			// 不为空则代表已注册
			if(map != null){
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				jsonObject.addProperty("code", "0");
				jsonObject.addProperty("msg", "用户已注册");
				renderJSON(jsonObject);
			}
			// 需要每个单独重新复制到新类，因为直接转成model的user来自客户端，不要完全信任客户端的数据
			// 过滤emoji等表情
			userInfo.user_password = user.user_password;
			userInfo.head_img = user.head_img;
			userInfo.emali = user.emali.replaceAll("[\\ud800\\udc00-\\udbff\\udfff\\ud800-\\udfff]", "");
			userInfo.sex = user.sex;
			userInfo.id_card = user.id_card.replaceAll("[\\ud800\\udc00-\\udbff\\udfff\\ud800-\\udfff]", "");
			userInfo.address = user.address.replaceAll("[\\ud800\\udc00-\\udbff\\udfff\\ud800-\\udfff]", "");
			userInfo.realname = user.realname.replaceAll("[\\ud800\\udc00-\\udbff\\udfff\\ud800-\\udfff]", "");
			userInfo.birth = user.birth;
			userInfo.deleteflag = "1";
			userInfo.credit = "100";
			userInfo.credit_cmt = 0;
			userInfo.add_time = Utils.getCurrentTime();
			userInfo.save();
						
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			jsonObject.addProperty("code", "1");
			jsonObject.addProperty("msg", "注册成功");
			renderJSON(jsonObject);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("用户注册出错"+ e.getMessage());
			renderJSON(Utils.apiError());
		}
	}
	
	/**
	 * 用户登陆
	 * {"user_name":"18523831972","user_password":"1234"}
	 */
	public static void login() {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			JsonObject jsonObject = new JsonObject();
			User user = Utils.getBody(User.class);
			
			user = user.find("user_name = ? AND user_password = ?", user.user_name,user.user_password).first();
			
			// 验证是否存在
			if (user != null) {
				// 更新用户表信息到redis
				// 把user对象转成map
				Map<String, String> userInfo = Utils.beanToMap(user);
				// 放入redis
				jedis.hmset(userInfo.get("user_id") + ":user:info", userInfo);
				
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				jsonObject.addProperty("code", "1");
				jsonObject.addProperty("msg", "用户登陆成功");
				jsonObject.add("info", new Gson().toJsonTree(user));
				renderJSON(jsonObject);
			} else {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				jsonObject.addProperty("code", "0");
				jsonObject.addProperty("msg", "账号或密码输入错误");
				renderJSON(jsonObject);
			}
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("用户登陆出错"+ e.getMessage());
			renderJSON(Utils.apiError());
		}

	}
	
	// 目前只有"cn-hangzhou"这个region可用, 不要使用填写其他region的值
  public static final String REGION_CN_HANGZHOU = "cn-hangzhou";
  // 当前 STS API 版本
  public static final String STS_API_VERSION = "2015-04-01";
	
	/**
	 * STS服务临时授权
	 */
	public static void ossTokenSTS(){
		try {
			JsonObject jsonObject = new JsonObject();
			
			// 只有 RAM用户（子账号）才能调用 AssumeRole 接口
		    // 阿里云主账号的AccessKeys不能用于发起AssumeRole请求
		    // 请首先在RAM控制台创建一个RAM用户，并为这个用户创建AccessKeys
		    String accessKeyId = Play.configuration.getProperty("ossAccessKeyId");
		    String accessKeySecret = Play.configuration.getProperty("ossAccessKeySecret");
		    // AssumeRole API 请求参数: RoleArn, RoleSessionName, Policy, and DurationSeconds
		    // RoleArn 需要在 RAM 控制台上获取
		    String roleArn = Play.configuration.getProperty("ossRoleArn");
		    // RoleSessionName 是临时Token的会话名称，自己指定用于标识你的用户，主要用于审计，或者用于区分Token颁发给谁
		    // 但是注意RoleSessionName的长度和规则，不要有空格，只能有'-' '_' 字母和数字等字符
		    // 具体规则请参考API文档中的格式要求
		    String roleSessionName = "alice-001";
		    // 如何定制你的policy?
		    String policy = "{\n" +
		            "    \"Version\": \"1\", \n" +
		            "    \"Statement\": [\n" +
		            "        {\n" +
		            "            \"Action\": [\n" +
//		            "                \"oss:*\" \n" +     // 有可能会出现权限不足的情况，此行代码获取全部权限
		            "                \"oss:GetBucket\", \n" +
		            "                \"oss:GetObject\", \n" +
//		            "                \"oss:DeleteObject\", \n" +
					"                \"oss:PutObject\" \n" +
		            "            ], \n" +
		            "            \"Resource\": [\n" +
		            "                \"acs:oss:*:*:*\"\n" +
		            "            ], \n" +
		            "            \"Effect\": \"Allow\"\n" +
		            "        }\n" +
		            "    ]\n" +
		            "}";
		    // 此处必须为 HTTPS
		    ProtocolType protocolType = ProtocolType.HTTPS;
			 final AssumeRoleResponse response = assumeRole(accessKeyId, accessKeySecret,roleArn, roleSessionName, policy, protocolType);
			
			jsonObject.addProperty("code", "1");
			jsonObject.addProperty("msg", "STS服务临时授权成功");
			jsonObject.addProperty("expiration", response.getCredentials().getExpiration());
			jsonObject.addProperty("accessKeyId", response.getCredentials().getAccessKeyId());
			jsonObject.addProperty("accessKeySecret", response.getCredentials().getAccessKeySecret());
			jsonObject.addProperty("securityToken", response.getCredentials().getSecurityToken());
			renderJSON(jsonObject);
		} catch (Exception e) {
			Logger.error("STS服务临时授权错误"+ e.getMessage());
			renderJSON(Utils.apiError());
		}
	}
	
	private static AssumeRoleResponse assumeRole(String accessKeyId, String accessKeySecret, String roleArn,
			String roleSessionName, String policy, ProtocolType protocolType) throws ClientException {
		
		 try {
		      // 创建一个 Aliyun Acs Client, 用于发起 OpenAPI 请求
		      IClientProfile profile = DefaultProfile.getProfile(REGION_CN_HANGZHOU, accessKeyId, accessKeySecret);
		      DefaultAcsClient client = new DefaultAcsClient(profile);
		      // 创建一个 AssumeRoleRequest 并设置请求参数
		      final AssumeRoleRequest request = new AssumeRoleRequest();
		      request.setVersion(STS_API_VERSION);
		      request.setMethod(MethodType.POST);
		      request.setProtocol(protocolType);
		      request.setRoleArn(roleArn);
		      request.setRoleSessionName(roleSessionName);
		      request.setPolicy(policy);
		      request.setDurationSeconds(900L);
		      // 发起请求，并得到response
		      final AssumeRoleResponse response = client.getAcsResponse(request);
		      return response;
		    } catch (Exception e) {
		      Logger.error("STS服务临时授权,assumeRole方法错误"+ e.getMessage());
		    }
		return null;
			
	}
	
	
}
