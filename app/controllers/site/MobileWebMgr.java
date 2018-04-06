package controllers.site;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
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
import play.Logger;
import play.db.DB;
import play.libs.Codec;
import play.mvc.Controller;
import redis.clients.jedis.Jedis;

public class MobileWebMgr extends Controller {
	private static DataSource ds = DB.getDataSource();
	private static QueryRunner run = new QueryRunner(ds);
	
	public void mobileIndex() {
		render();
	}
	
}