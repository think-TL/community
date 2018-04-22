package controllers.boss;

import java.io.InputStream;
import java.math.BigDecimal;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.EntityTransaction;
import javax.sql.DataSource;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pingplusplus.Pingpp;
import com.pingplusplus.exception.APIConnectionException;
import com.pingplusplus.exception.APIException;
import com.pingplusplus.exception.AuthenticationException;
import com.pingplusplus.exception.ChannelException;
import com.pingplusplus.exception.InvalidRequestException;
import com.pingplusplus.exception.RateLimitException;
import com.pingplusplus.model.Transfer;
import com.pingplusplus.util.PingppSignature;
import com.sun.corba.se.impl.protocol.giopmsgheaders.Message;

import controllers.util.RedisUtil;
import controllers.util.Utils;
import play.Logger;
import play.Play;
import play.data.validation.Valid;
import play.db.DB;
import play.db.jpa.JPA;
import play.libs.Codec;
import play.mvc.Controller;
import play.mvc.Http.Header;
import play.mvc.With;
import redis.clients.jedis.Jedis;

/**
 * 订单管理
 * 
 * @author Au QQ:594919495
 *
 */
@With(BossIntercepter.class)
public class OrderMgr extends Controller {

	private static DataSource ds = DB.getDataSource();
	private static QueryRunner run = new QueryRunner(ds);

	/**
	 * 订单管理
	 */
	public static void orderTrade() {
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
		render(permissionList);
	}

	/**
	 * 订单管理
	 */
	public static void oradeData(int limit, int offset, String username, String startTime, String endTime,
			String reusername) {
		System.out.println("1");
		try {
			JsonObject jsonObject = new JsonObject();

			StringBuffer sql = new StringBuffer(
					"select o.id,o.itime,r.cmt,r.name,r.price,r.user_id,u.user_name ,(select user_name from t_user where user_id =r.receive_ser_id ) as re_name from t_order o INNER JOIN t_resources r on o.resources_id = r.id JOIN t_user u on u.user_id = o.user_id  where r.deleteflag ='1'");

			StringBuffer wheres = new StringBuffer();
			// 判断是否根据下单用户名查找
			if (username != null && !"".equals(username)) {
				wheres.append(" and u.user_name = '" + Utils.getSecurityParm(username) + "'");
			}
//			// 判断是否根据资源用户名查找
//			if (reusername != null && !"".equals(reusername)) {
//				wheres.append(" and u.user_name = '" + Utils.getSecurityParm(reusername) + "'");
//			}

			// 判断是否根据时间查询
			if (startTime != null && !"".equals(startTime)) {
				wheres.append(" AND o.itime > " + Utils.getSecurityParm(startTime.replaceAll("-", "")));
			}

			// 判断是否根据时间查询
			if (endTime != null && !"".equals(endTime)) {
				wheres.append(" AND o.itime < " + Utils.getSecurityParm(endTime.replaceAll("-", "")));
			}

			// 获得数据
			List statisticsList = run.query(sql.toString() + wheres.toString() + " limit ?,?", new MapListHandler(),
					offset, limit);
			System.out.println(wheres.toString());
			// 获得总条数
			List list = run.query(
					"SELECT COUNT(o.id) as total FROM t_order o  WHERE o.deleteflag ='1'  " + wheres.toString(),
					new MapListHandler());

			Map map = (Map) list.get(0);
			jsonObject.addProperty("total", map.get("total").toString());
			jsonObject.add("rows", new Gson().toJsonTree(statisticsList));
			renderJSON(jsonObject);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("评论管理错误", e.getMessage());
		}
	}
}
