package controllers.site;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;

import com.aliyuncs.exceptions.ClientException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.taobao.api.ApiException;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.request.AlibabaAliqinFcSmsNumSendRequest;
import com.taobao.api.response.AlibabaAliqinFcSmsNumSendResponse;

import controllers.util.RedisUtil;
import controllers.util.Utils;
import models.User;
import play.Logger;
import play.db.DB;
import play.libs.Codec;
import play.mvc.Controller;
import redis.clients.jedis.Jedis;

public class LoginMgr extends Controller {
	private static DataSource ds = DB.getDataSource();
	private static QueryRunner run = new QueryRunner(ds);

	public void login() {
		render();
	}

	public void registerUser() {
		renderTemplate("/site/LoginMgr/register.html");
	}

	/**
	 * 用户注册 传入 用户名，密码，头像，邮箱，性别，身份证号，地址 真实姓名 生日
	 */
	public static void register(User user) {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			// 先过滤一次
			Map map = run.query("SELECT user_id FROM t_user WHERE user_name = ?", new MapHandler(), user.user_name);
			// 不为空则代表已注册
			if (map != null) {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				String msg = "用户名已被注册，请重新注册";
				renderTemplate("/site/LoginMgr/register.html", msg);
			}
			User userInfo = new User();
			// 需要每个单独重新复制到新类，因为直接转成model的user来自客户端，不要完全信任客户端的数据
			userInfo.user_name = user.user_name;
			userInfo.user_password = user.user_password;
			userInfo.head_img = user.head_img;
			userInfo.emali = user.emali;
			userInfo.sex = user.sex;
			userInfo.id_card = user.id_card;
			userInfo.address = user.address;
			userInfo.realname = user.realname;
			userInfo.birth = user.birth;
			userInfo.deleteflag = "1";
			userInfo.credit = "100";
			userInfo.credit_cmt = 0;
			userInfo.add_time = Utils.getCurrentTime();
			userInfo.save();
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			String msg = "用户名已被注册，请重新注册";
			renderTemplate("/site/LoginMgr/login.html", msg);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("用户注册出错" + e.getMessage());
		}
	}

	/**
	 * 登陆操作
	 * 
	 * @param username
	 * @param password
	 */
	public void loginUser(String username, String password) {
		Jedis jedis = RedisUtil.getJedis();
		try {
			User user = User.find("user_name = ? AND user_password = ?", username, password).first();
			if (user != null) {
				// 更新用户表信息到redis
				// 把user对象转成map
				Map<String, String> userInfo = Utils.beanToMap(user);
				// 放入redis
				jedis.hmset(userInfo.get("user_id") + ":user:info", userInfo);
				System.out.println(jedis.hmget(userInfo.get("user_id") + ":user:info", "user_name").get(0));
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				String sql = "select * from t_resources t where t.deleteflag =1";
				List resour_list = run.query(sql, new MapListHandler());
				renderTemplate("/site/IndexMgr/index.html", resour_list,user);
			} else {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				String msg = "账号或密码输入错误";
				renderTemplate("/site/LoginMgr/login.html", msg);
			}
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("用户登陆出错" + e.getMessage());
		}

	}

}