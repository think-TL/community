package controllers.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.UUID;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.concurrent.Future;
import java.util.Calendar;
import java.util.Collections;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import javax.xml.crypto.dsig.SignatureMethod;

import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.FloatRange;
import org.apache.commons.lang.time.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;

import com.aliyun.oss.OSSClient;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.http.ProtocolType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.aliyuncs.sts.model.v20150401.AssumeRoleRequest;
import com.aliyuncs.sts.model.v20150401.AssumeRoleResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

import play.Logger;
import play.Play;
import play.cache.Cache;
import play.exceptions.UnexpectedException;
import play.libs.Codec;
import play.libs.Crypto;
import play.mvc.Controller;
import play.mvc.Http.Header;
import redis.clients.jedis.Jedis;

public class Utils extends Controller {

	/**
	 * 得到某个时间点后N天后的日期
	 * 
	 * @param str
	 *            当前时间
	 * @param n
	 *            天数
	 * @return
	 */
	public static String afterNDayByParam(String str, int n) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		// 将字符串的日期转为Date类型，ParsePosition(0)表示从第一个字符开始解析
		Date date = sdf.parse(str, new ParsePosition(0));
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.DATE, n);
		Date date1 = calendar.getTime();
		String out = sdf.format(date1);
		return out;
	}

	/**
	 * 提供精确的减法运算。
	 * 
	 * @param v1
	 *            被减数
	 * @param v2
	 *            减数
	 * @return 两个参数的差
	 */
	public static BigDecimal strSub(String v1, String v2) {
		BigDecimal b1 = new BigDecimal(v1);
		BigDecimal b2 = new BigDecimal(v2);
		return b1.subtract(b2);
	}

	/**
	 * 提供精确的乘法运算。
	 * 
	 * @param v1
	 *            被乘数
	 * @param v2
	 *            乘数
	 * @return 两个参数的积
	 */
	public static BigDecimal strMul(String v1, String v2) {
		BigDecimal b1 = new BigDecimal(v1);
		BigDecimal b2 = new BigDecimal(v2);
		return b1.multiply(b2);
	}

	/**
	 * 得到某个时间点后N分钟后的日期
	 * 
	 * @param n
	 * @return
	 */
	public static String afterNMinByParam(String str, int n) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		// 将字符串的日期转为Date类型，ParsePosition(0)表示从第一个字符开始解析
		Date date = sdf.parse(str, new ParsePosition(0));
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.MINUTE, n);
		Date date1 = calendar.getTime();
		String out = sdf.format(date1);
		return out;
	}

	/**
	 * 得到N分钟后的日期
	 * 
	 * @param n
	 * @return
	 */
	public static String afterNMin(int n) {
		Calendar calendar2 = Calendar.getInstance();
		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMddHHmmss");
		calendar2.add(Calendar.MINUTE, n);
		return sdf2.format(calendar2.getTime());
	}

	/**
	 * 得到N天后的日期
	 * 
	 * @param n
	 * @return
	 */
	public static String afterNDay(int n) {
		Calendar calendar2 = Calendar.getInstance();
		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMddHHmmss");
		calendar2.add(Calendar.DATE, n);
		return sdf2.format(calendar2.getTime());
	}

	/**
	 * 将javabean实体类转为map类型，然后返回一个map类型的值
	 * 
	 * @param obj
	 * @return
	 */
	public static Map<String, String> beanToMap(Object obj) {
		Map<String, String> params = new HashMap<String, String>(0);
		try {
			PropertyUtilsBean propertyUtilsBean = new PropertyUtilsBean();
			PropertyDescriptor[] descriptors = propertyUtilsBean.getPropertyDescriptors(obj);
			for (int i = 0; i < descriptors.length; i++) {
				String name = descriptors[i].getName();
				if (!"class".equals(name)) {
					String value = "";
					if (propertyUtilsBean.getNestedProperty(obj, name) != null) {
						value = propertyUtilsBean.getNestedProperty(obj, name) + "";
					}
					params.put(name, value);
				}
			}
		} catch (Exception e) {
		}
		return params;
	}

	/**
	 * 获取客户端body中的json对象,并转换为实体
	 * 
	 * @return 客户端请求的json
	 */

	public static <T> T getBody(Class<T> c) {
		InputStream is = request.body;
		String json = null;
		try {
			json = IOUtils.toString(is, "UTF-8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Logger.error("获取API请求body出错", e.getMessage());
		}
		// 跨域设置
		response.setHeader("Access-Control-Allow-Origin", "*");
		return new Gson().fromJson(json, c);
	}

	/**
	 * 根据时间戳生成验证码
	 * 
	 * @return
	 */
	public static String getCode() {
		Long time = DateTimeUtils.currentTimeMillis();
		return time.toString().substring(8, 12);
	}

	public static String getCurrentTime() {
		return DateTime.now().toString("yyyyMMddHHmmss");
	}

	/**
	 * 格式转换 yyyyMMddHHmmss 转 yyyy-MM-dd HH:mm:ss
	 * 
	 * @return
	 */
	public static String timeFormat(String time) {
		SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMddhhmmss");
		Date date = null;
		try {
			date = (Date) sdf1.parse(time);
		} catch (ParseException e) {
		}
		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf2.format(date);
	}

	/**
	 * 防止sql注入(参数)
	 * 
	 * @param sql
	 * @return
	 */
	public static String getSecurityParm(String sql) {
		sql = StringEscapeUtils.escapeSql(sql);
		return sql;
	}

	/**
	 * 接口异常返回
	 * 
	 * @return
	 */
	public static JsonObject apiError() {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("code", "500");
		jsonObject.addProperty("msg", "后台接口异常");
		return jsonObject;
	}

	/**
	 * 保存图片到oss,写成一般方法，如是void方法，会走play返回
	 */
	public static int saveImg2OSS(String filename, InputStream is) {
		int re = 0;
		try {
			// endpoint以杭州为例，其它region请按实际情况填写
			String endpoint = "http://" + Play.configuration.getProperty("ossEndPoint");
			// accessKey请登录https://ak-console.aliyun.com/#/查看
			String accessKeyId = Play.configuration.getProperty("ossAccessKeyId");
			String accessKeySecret =Play.configuration.getProperty("ossAccessKeySecret");
			// 创建OSSClient实例
			OSSClient client = new OSSClient(endpoint, accessKeyId, accessKeySecret);
			client.putObject(Play.configuration.getProperty("ossBucketName"), filename, is);
			// 关闭client
			client.shutdown();
			re = 1;
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("保存图片到oss出错", e.getMessage());
		}
		return re;
	}
}
