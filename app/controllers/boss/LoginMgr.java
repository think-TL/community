package controllers.boss;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import com.taobao.api.ApiException;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.request.AlibabaAliqinFcSmsNumSendRequest;
import com.taobao.api.response.AlibabaAliqinFcSmsNumSendResponse;

import controllers.util.RedisUtil;
import controllers.util.Utils;
import models.Bizuser;
import models.BizuserRole;
import play.Logger;
import play.Play;
import play.cache.Cache;
import play.db.DB;
import play.mvc.Controller;
import redis.clients.jedis.Jedis;

public class LoginMgr extends Controller {
	private static DataSource ds = DB.getDataSource();
	private static QueryRunner run = new QueryRunner(ds);

	// AsyncHttpClient异步http请求
	private static AsyncHttpClient client;

	/**
	 * 登陆成功进入后台主页
	 */
	public static void boss() {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		
		try {
			String username = params.get("username");
			String password = params.get("password");
			String sql = "select id from  t_bizuser where name = ? and password = ?";	
			List list = run.query(sql, new MapListHandler(), username,password);
			if(list.size()!=0) {
			 
			// 验证账号密码
			//if (1==1||("18723112574".equals(params.get("username")) && "2574".equals(params.get("code")))||(jedis.get(params.get("username")) != null && params.get("code").equals(jedis.get(params.get("username"))))) {
				//将sesstion存储到redis中。
				jedis.set(session.getId().toString(), session.getAuthenticityToken().toString());
				
				Bizuser bizuser = Bizuser.find("name=? and deleteflag = '1' ",params.get("username")).first();
				// 把bizuser对象转成map
				Map<String, String> bizuserInfo = Utils.beanToMap(bizuser);
				// 放入内存
				jedis.hmset(session.getId() + ":bizuser:info", bizuserInfo);
				
				// 保存5小时后失效
				jedis.expire(session.getId().toString(), 60 * 60 * 5);
				jedis.expire(session.getId() + ":bizuser:info", 60 * 60 * 5);
				
			} else {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				String msg = "用户名或验证码有误，请重新输入";
				login(msg);
			}
		
			// 进入后台首页
			List permissionList = IndexMgr.getPermission(jedis);
			renderTemplate("boss/IndexMgr/index.html",permissionList);
		} catch (Exception e) {
			// 关闭链接
		    e.printStackTrace();
			RedisUtil.closeJedisPool(jedis);
			Logger.error("登陆失败！");
		}
	}

	/**
	 * 登陆
	 */
	public static void login(String msg) {
		try {
			// 获取bing搜索每日图片。
			client = new AsyncHttpClient();
			Future<Response> f = client.prepareGet("http://cn.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1")
					.execute();
			Response response = f.get();
			Gson gson = new Gson();
			HashMap allmap = gson.fromJson(response.getResponseBody(), HashMap.class);
			ArrayList imagelist = (ArrayList) allmap.get("images");
			LinkedTreeMap map = (LinkedTreeMap) imagelist.get(0);
			String url = "http://cn.bing.com"+map.get("url").toString();
			render(url, msg);
		} catch (Exception e) {
			// 获取异常时直接返回
			Logger.error("获取bing搜索每日图片失败！");
			render();
		}
	}
	/**
	 * 渠道管理登陆
	 */
	public static void channelLogin(String msg) {
		try {
			// 获取bing搜索每日图片。
			client = new AsyncHttpClient();
			Future<Response> f = client.prepareGet("http://cn.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1")
					.execute();
			Response response = f.get();
			Gson gson = new Gson();
			HashMap allmap = gson.fromJson(response.getResponseBody(), HashMap.class);
			ArrayList imagelist = (ArrayList) allmap.get("images");
			LinkedTreeMap map = (LinkedTreeMap) imagelist.get(0);
			String url = "http://cn.bing.com"+map.get("url").toString();
			render(url, msg);
		} catch (Exception e) {
			// 获取异常时直接返回
			Logger.error("渠道管理登陆获取bing搜索每日图片失败！");
			render();
		}
	}

	/**
	 * 渠道管理登陆成功进入主页
	 */
	public static void channelLogging() {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			String sql = " SELECT * FROM t_channel_package WHERE name = ? AND password = ? ";
			Map bizuser = run.query(sql, new MapHandler(),params.get("username"),params.get("password"));
			
			// 验证账号密码
			if (bizuser != null) {
				// 将sesstion存储到redis中。
				jedis.set(session.getId().toString(), session.getAuthenticityToken().toString());
				
				// 存储当前用户的渠道到redis
				jedis.set(session.getId().toString() +":channel", bizuser.get("channel").toString());
				// 保存5小时后失效
				jedis.expire(session.getId().toString(), 60 * 60 * 5);
				jedis.expire(session.getId() + ":bizuser:info", 60 * 60 * 5);
				
			} else {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				String msg = "用户名或密码有误，请重新输入";
				channelLogin(msg);
			}
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			
			// 获取分包信息
			sql = "SELECT pack FROM t_channel_package cp WHERE cp.deleteflag = 1 AND cp.channel = ? ";
			List packList = run.query(sql, new MapListHandler(),bizuser.get("channel"));
			
			renderTemplate("boss/IndexMgr/listPackage.html",packList);
		} catch (Exception e) {
			e.printStackTrace();
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("登陆失败！");
		}
	}
	/**
	 * 获取验证码
	 */
	public static void getAdminCode(String phone) {
		Jedis jedis = RedisUtil.getJedis();
		try {
			Bizuser user = Bizuser.find("name = ? and deleteflag = 1", phone).first();
			if(user != null ){
				String code = Utils.getCode();
//				System.out.println(code);
				String json = "{\"product\":\"春聊\", \"code\":\""+code+"\"}";
				sendMessage(json,phone,jedis);
				
				// 生成验证码 保存redis 3分钟
				jedis.set(phone,code);
				jedis.expire(phone, 180);
				
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				renderText("success", new Gson());
			}else{
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				renderText("滚滚滚，你没资格", new Gson());
			}
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			renderText("'后台异常", new Gson());
			Logger.error("后台登录获取验证码出错", e.getMessage());
		}
	}
	
	private static void sendMessage(String sendJson,String phone,Jedis jedis) throws ClientException{
		TaobaoClient client = new DefaultTaobaoClient("http://gw.api.taobao.com/router/rest", "24619384",
				"c9f33e769d7b63eb7d69fbb214fa962e");
		AlibabaAliqinFcSmsNumSendRequest req = new AlibabaAliqinFcSmsNumSendRequest();
		// 公共回传参数，在“消息返回”中会透传回该参数；举例：用户可以传入自己下级的会员ID，在消息返回时，该会员ID会包含在内，用户可以根据该会员ID识别是哪位会员使用了你的应用
		req.setExtend(phone);
		req.setSmsType("normal");
		req.setSmsFreeSignName("感娱科技");
		req.setSmsParamString(sendJson);
		// 接收验证码的手机号码
		req.setRecNum(phone);
		req.setSmsTemplateCode("SMS_57485026");
		AlibabaAliqinFcSmsNumSendResponse rsp = null;
		try {
			rsp = client.execute(req);
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			renderJSON("{'code':'500','msg':'后台接口异常'}", new Gson());
		}
		// 正常响应
		if (rsp.getBody().contains("alibaba_aliqin_fc_sms_num_send_response")) {
		} else {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			renderText("发送短信失败...请重试", new Gson());
		}
	}
	
//	private static void sendMessage(String sendJson,String phone,Jedis jedis) throws ClientException{
//		//设置超时时间-可自行调整
//		System.setProperty("sun.net.client.defaultConnectTimeout", "10000");
//		System.setProperty("sun.net.client.defaultReadTimeout", "10000");
//		//初始化ascClient需要的几个参数
//		final String product = "Dysmsapi";//短信API产品名称（短信产品名固定，无需修改）
//		final String domain = "dysmsapi.aliyuncs.com";//短信API产品域名（接口地址固定，无需修改）
//		//替换成你的AK
//		final String accessKeyId = "LTAI60AxqWhFp6hN";//你的accessKeyId,参考本文档步骤2
//		final String accessKeySecret = "FKRRWdZqEpZ29SK4recerAQf1NRDCh";//你的accessKeySecret，参考本文档步骤2
//		//初始化ascClient,暂时不支持多region（请勿修改）
//		IClientProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKeyId,
//		accessKeySecret);
//		DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou", product, domain);
//		IAcsClient acsClient = new DefaultAcsClient(profile);
//		 //组装请求对象
//		 SendSmsRequest request = new SendSmsRequest();
//		 //使用post提交
//		 request.setMethod(MethodType.POST);
//		 //必填:待发送手机号。支持以逗号分隔的形式进行批量调用，批量上限为1000个手机号码,批量调用相对于单条调用及时性稍有延迟,验证码类型的短信推荐使用单条调用的方式
//		 request.setPhoneNumbers(phone);
//		 //必填:短信签名-可在短信控制台中找到
//		 request.setSignName("感娱科技");
//		 //必填:短信模板-可在短信控制台中找到
//		 request.setTemplateCode("SMS_57485026");
//		 //可选:模板中的变量替换JSON串,如模板内容为"亲爱的${name},您的验证码为${code}"时,此处的值为
//		 //友情提示:如果JSON中需要带换行符,请参照标准的JSON协议对换行符的要求,比如短信内容中包含\r\n的情况在JSON中需要表示成\\r\\n,否则会导致JSON在服务端解析失败
//		 request.setTemplateParam(sendJson);
//		 //可选-上行短信扩展码(扩展码字段控制在7位或以下，无特殊需求用户请忽略此字段)
//		 //request.setSmsUpExtendCode("90997");
//		 //可选:outId为提供给业务方扩展字段,最终在短信回执消息中将此值带回给调用者
////		 request.setOutId("yourOutId");
//		//请求失败这里会抛ClientException异常
//		SendSmsResponse sendSmsResponse = acsClient.getAcsResponse(request);
//		if(sendSmsResponse.getCode() != null && sendSmsResponse.getCode().equals("OK")) {
//		//请求成功
//		}else{
//			// 关闭链接
//			RedisUtil.closeJedisPool(jedis);
//			renderText("发送短信失败...请重试", new Gson());
//		}
//	}
	
	/**
	 * 退出
	 */
	public static void exit() {
		session.clear();
		login("已退出登陆");
	}
	
}
