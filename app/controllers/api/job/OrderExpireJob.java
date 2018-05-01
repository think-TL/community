package controllers.api.job;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;

import com.aliyun.openservices.ons.api.Action;
import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Consumer;
import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.MessageListener;
import com.aliyun.openservices.ons.api.ONSFactory;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.aliyun.openservices.ons.api.order.OrderProducer;
import com.gexin.rp.sdk.base.IPushResult;
import com.gexin.rp.sdk.base.impl.ListMessage;
import com.gexin.rp.sdk.base.impl.Target;
import com.gexin.rp.sdk.base.payload.APNPayload;
import com.gexin.rp.sdk.http.IGtPush;
import com.gexin.rp.sdk.template.TransmissionTemplate;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import controllers.util.RedisUtil;
import controllers.util.Utils;
import models.User;
import play.Logger;
import play.Play;
import play.db.DB;
import play.jobs.Job;
import play.jobs.On;
import play.jobs.OnApplicationStart;
import redis.clients.jedis.Jedis;

@On(" 0 * * * * ?")
public class OrderExpireJob extends Job  {
	private static DataSource DS = DB.getDataSource();
	private static QueryRunner RUN = new QueryRunner(DS);
	@Override
	public void doJob() {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			// 获取半个小时前的时间
			String startTime = Utils.afterNMin(-30);
			String sql = "select id,user_id from t_order where deleteflag = 1 and order_status = 1 and shipping_status = 1 and utime > ?";
			List orderListInfo = RUN.query(sql, new MapListHandler(),startTime);
			
			for (int i = 0; i < orderListInfo.size(); i++) {
				Map order = (Map) orderListInfo.get(i);
				// 用户信用值减10
				User user = User.find("user_id = ? ", order.get("user_id")).first();
				user.credit = Utils.strSub(user.credit, "10").toString();
				user.save();
				// redis更新
				Map<String, String> userInfo = Utils.beanToMap(user);
				jedis.hmset(userInfo.get("user_id") + ":user:info", userInfo);
				// 订单关闭
				RUN.update("update t_order set order_status = 2,deleteflag = 0 where id = ?",order.get("id"));
			}
			
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("OrderExpireJob定时任务出错"+e.getMessage());
		}
	}
}
