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
			order.resources_name = resources.name;
			order.resources_type_id = resources.resources_type_id;
			order.resources_cmt = resources.cmt;
			order.resources_img1 = resources.img1;
			order.resources_img2 = resources.img2;
			order.resources_img3 = resources.img3;
			order.price = resources.price;
			order.resources_creditnumber = resources.credit_number;
			order.maiuser_id = resources.user_id;
			
			// 获取卖家信息
			List merchantUserInfo = jedis.hmget(resources.user_id + ":user:info", "iphone","address","realname");
			order.phone = merchantUserInfo.get(0).toString();
			order.address = merchantUserInfo.get(1).toString();
			order.name = merchantUserInfo.get(2).toString();
			order.order_status = "0";
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
