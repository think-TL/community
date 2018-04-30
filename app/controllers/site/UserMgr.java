package controllers.site;

import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.http.util.TextUtils;
import org.omg.CORBA.PUBLIC_MEMBER;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.org.apache.bcel.internal.generic.NEW;

import controllers.util.RedisUtil;
import controllers.util.Utils;
import models.User;
import play.Logger;
import play.db.DB;
import play.mvc.Controller;
import redis.clients.jedis.Jedis;

public class UserMgr  extends Controller{
	private static DataSource ds = DB.getDataSource();
	private static QueryRunner allRun = new QueryRunner(ds);
	
	
	public static void userApprove(String userid) {
		User user = User.find("user_id=?", userid).first();
		renderTemplate("/site/UserMgr/userApprove.html",user);
	}
	
	/**
	 * 用户传资料认证
	 */
	public static void userAuthentication(User user) {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			
			User oldUser = User.find("user_id = ? ", user.user_id).first();
			
			// 判断用户是否 未审核 或为 审核失败状态
			if(oldUser.user_status == 1 || oldUser.user_status == 3) {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
			}
			if(user.authentication_url1_blob!=null) {
				long time = System.currentTimeMillis();
				oldUser.authentication_url1 = "https://oss-community.oss-cn-hangzhou.aliyuncs.com/certifyPhone/" + time
						+ "_certify.jpg";
				Utils.saveImg2OSS("certifyPhone/" + time + "__certify.jpg", oldUser.authentication_url1_blob.get());
			}else {
				RedisUtil.closeJedisPool(jedis);
			}
			
			
			if(user.authentication_url2_blob!=null) {
				long time = System.currentTimeMillis();
				oldUser.authentication_url2 = "https://oss-community.oss-cn-hangzhou.aliyuncs.com/certifyPhone/" + time
						+ "_certify.jpg";
				Utils.saveImg2OSS("certifyPhone/" + time + "__certify.jpg", oldUser.authentication_url2_blob.get());
			}
			oldUser.user_status = 1;
			oldUser.save();
			// 更新用户表信息到redis
			// 把user对象转成map
			Map<String, String> userInfo = Utils.beanToMap(oldUser);
			// 放入redis
			jedis.hmset(userInfo.get("user_id") + ":user:info", userInfo);
			
			userApprove(user.user_id);
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("用户传资料认证出错"+ e.getMessage());
		}
	}
	
	
	/**
	 * 查询个人信息
	 *  用户名，密码，头像，邮箱 性别， 身份证号，地址 昵称 生日  ，信用值等级，信用值具体信息
	 */
	public static void getUserByUserId(String userid) {
		
		try {
			User user = User.find("user_id=?", userid).first();
			// 关闭链接
			renderTemplate("/site/UserMgr/userInformation.html",user);
		} catch (Exception e) {
			// 关闭链接
			Logger.error("查询个人信息出错"+ e.getMessage());
		}
	}
	
	
	
	
	/**
	 * 修改个人信息之前的查询个人信息
	 */
	public  static void userIchangeGetByUserId(String userid){
		try {
			User user = User.find("user_id=?", userid).first();
			renderTemplate("/site/UserMgr/userIChange.html",user);
		} catch (Exception e) {
			Logger.error("查询个人信息出错"+ e.getMessage());
		}
	}
	
	public static void updateUserPage(String userid){
		User user = User.find("user_id=?", userid).first();
		renderTemplate("site/UserMgr/userPChange.html",user);
	}
	
	/**
	 * 修改密码
	 */
	public static void updateUserPwd(String user_password,String user_id) {
		// 获取jedis
		try {
			User user = User.find("user_id = ?",user_id).first();
			user.user_password = user_password;
			user.upd_time = Utils.getCurrentTime();
			user.save();
			System.out.println(user);
			renderTemplate("/site/UserMgr/userInformation.html",user);
			// 关闭链接
		} catch (Exception e) {
			// 关闭链接
			Logger.error("修改密码出错"+ e.getMessage());
		}
	}
	
	
	
	/**
	 * 修改用户信息
	 *  用户名，头像，邮 箱 性别，生日  
	 */
	public static void updateUserInfo(User user) {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			
			User oldUser = user.find("user_id = ? ", user.user_id).first();
			if(user.user_name != null) {
				// 修改用户名前判断是否存在
				oldUser.user_name = user.user_name;
				Map map = allRun.query("SELECT user_id FROM t_user WHERE user_name = ? AND user_id <> ? ", new MapHandler(), oldUser.user_name,oldUser.user_id);
				// 不为空则代表已注册
				if(map != null){
					// 关闭链接
					RedisUtil.closeJedisPool(jedis);
				}
			}
			
			if(user.headImg.get()!=null){
				long time = System.currentTimeMillis();
				Utils.saveImg2OSS("headImg/" + time + "_head.jpg", user.headImg.get());
				user.head_img = "https://oss-community.oss-cn-hangzhou.aliyuncs.com/headImg/" +time
						+ "_head.jpg";
				oldUser.head_img = user.head_img;
			}
			
			if(user.address !=null) {
				oldUser.birth = user.birth;
			}
			if(user.birth != null) {
				oldUser.birth = user.birth;
			}
			
			if(user.emali != null) {
				oldUser.emali = user.emali;
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
			renderTemplate("/site/UserMgr/userInformation.html",user);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("修改用户信息出错"+ e.getMessage());
		}
	}
}
