package controllers.api.job;

import java.sql.SQLException;
import java.util.ArrayList;
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
import play.Logger;
import play.Play;
import play.db.DB;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import redis.clients.jedis.Jedis;

@OnApplicationStart
public class StartToReadyJob extends Job  {
	@Override
	public void doJob() {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();   
		try {
			DataSource ds = DB.getDataSource();
			QueryRunner allRun = new QueryRunner(ds);
			
			// 查询公告，放入redis
			String sql = "SELECT "
									+ "n.id,n.title,n.content,bu.memo "
							+ "FROM t_notice n "
							+ "INNER JOIN t_bizuser bu ON bu.id = n.bizuser_id "
							+ "WHERE n.deleteflag = 1"
							+ " ORDER BY n.add_time DESC";
			List list = allRun.query(sql, new MapListHandler());
			String resultStr = new Gson().toJsonTree(list).toString();
			// 放入redis
			jedis.set("notice:json", resultStr);
			
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("系统开启初始化数据到redis出错"+e.getMessage());
		}
		
    }
}
