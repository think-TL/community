package controllers.boss;

import java.awt.Graphics2D;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.corba.se.impl.protocol.giopmsgheaders.Message;

import controllers.util.RedisUtil;
import controllers.util.Utils;
import models.User;
import play.Logger;
import play.data.validation.Valid;
import play.db.DB;
import play.mvc.Controller;
import play.mvc.With;
import redis.clients.jedis.Jedis;

@With(BossIntercepter.class)
public class UserMgr extends Controller{
	private static DataSource ds = DB.getDataSource();
	private static QueryRunner run = new QueryRunner(ds);
	
	/**
	 * 用户管理
	 */
	public static void listUserV2(String msg) {
		System.out.println(msg);
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		List permissionList = null;
		try {
			permissionList = IndexMgr.getPermission(jedis);
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
		}
		render(permissionList,msg);
	}
	
	/**
	 * 用户管理
	 */
	public static void listUserV2Data(int limit, int offset,String userid,String sex,String goddessflag,String startTime,String endTime,
			String isrecommend,String iswithdraw,String isfreshman,String indexshowflag) {
		try {
			JsonObject jsonObject = new JsonObject();
			
			StringBuffer sql = new StringBuffer("select a.user_id,a.user_name,a.head_img,a.sex,a.credit,a.credit_cmt,a.user_status,a.add_time,a.address,a.birth,a.emali,a.id_card,a.realname from t_user as a  where a.deleteflag='1'");
			
			StringBuffer wheres = new StringBuffer();
			// 判断是否根据id查找
			if(userid != null && !"".equals(userid)){
				wheres.append(" and a.user_id = '" + Utils.getSecurityParm(userid) + "'");
			}
			
//			// 判断是否根据性别查找
			if(sex != null && !"-1".equals(sex) && !"".equals(sex)){
           		wheres.append(" and a.sex = '" + Utils.getSecurityParm(sex) + "'");
			}
//			// 判断是否根据女神认证查找
		if(goddessflag != null && !"-1".equals(goddessflag) && !"".equals(goddessflag)){
				wheres.append(" and a.user_status = '" + Utils.getSecurityParm(goddessflag) + "'");
 		}
		System.out.println(startTime.replaceAll("-", ""));
//			// 判断是否根据时间查询
		     if(startTime != null && !"".equals(startTime)){
				wheres.append(" AND a.add_time > " + Utils.getSecurityParm(startTime.replaceAll("-", "")));
				}
//			
//			// 判断是否根据时间查询
			if(endTime != null && !"".equals(endTime)){
				wheres.append(" AND a.add_time < " + Utils.getSecurityParm(endTime.replaceAll("-", "")));
			}
			
			// 获得数据
			List statisticsList = run.query(sql.toString() + wheres.toString() + " limit ?,?",new MapListHandler(),offset,limit);
			System.out.println(wheres.toString());
			// 获得总条数
			List list = run.query("SELECT COUNT(a.user_id) as total FROM t_user a WHERE a.deleteflag='1'" + wheres.toString(), new MapListHandler());
		
			Map map = (Map) list.get(0);
			jsonObject.addProperty("total", map.get("total").toString());
			jsonObject.add("rows", new Gson().toJsonTree(statisticsList));
			renderJSON(jsonObject);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("获取用户管理错误", e.getMessage());
		}
	}
}

