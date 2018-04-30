package controllers.site;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;

import com.aliyun.oss.OSSClient;

import controllers.util.RedisUtil;
import controllers.util.Utils;
import models.Resources;
import models.User;
import play.Logger;
import play.Play;
import play.db.DB;
import play.mvc.Controller;
import redis.clients.jedis.Jedis;
import sun.net.www.content.text.plain;

public class ResourceMgr extends Controller {

	private static DataSource ds = DB.getDataSource();
	private static QueryRunner run = new QueryRunner(ds);
	private static String userid;

	
	/**
	 * 查询资源分类返回给界面
	 * @param user_id
	 */
	public static void index(String user_id) {
		Jedis jedis = RedisUtil.getJedis();
		userid = user_id;
		User user = User.find("user_id=?", user_id).first();
		if (user != null) {
			try {
				String sql = "select id,name from t_resources_type where deleteflag=1;";
				List resoureTypeList = run.query(sql, new MapListHandler());
				renderTemplate("/site/ResourceMgr/resourcePublish.html", resoureTypeList, user);
			} catch (SQLException e) {
				Logger.error("类型获取出错" + e.getMessage());
			}
		}
	}

	/**
	 * 添加资源
	 * @param resources
	 */
	public static void addResource(Resources resources) {

		// video图片地址拼接。
		Resources resources2 = new Resources();
		if (resources != null) {
			// 名字
			if (resources.name != null) {
				resources2.name = resources.name;
			}
			// 图片
			if (resources.img1 != null) {
				long time = System.currentTimeMillis();
				resources2.imgurl = "https://oss-community.oss-cn-hangzhou.aliyuncs.com/resource/" + time
						+ "_phone.jpg";
				Utils.saveImg2OSS("resource/" + time + "_phone.jpg", resources.img1.get());
			}
			// 描述
			if (resources.cmt != null) {
				resources2.cmt = resources.cmt;
			}
			// 分类
			if (resources.resources_type_id != null) {
				resources2.resources_type_id = resources.resources_type_id;
			}
			resources2.add_time = Utils.getCurrentTime();
			// 所需信用值
			resources2.credit_number = resources.credit_number;
			// 价格
			resources2.price = resources.price;
			// 用户id
			if (userid != null) {
				resources2.user_id = userid;
			}
			resources2.deleteflag="1";
			resources2.save();
			
			try {
				String sql = "select * from t_resources t where t.deleteflag =1";
				List resour_list = run.query(sql, new MapListHandler());
				renderTemplate("/site/indexMgr/index.html", resour_list);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
