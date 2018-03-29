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
	private static final String CHARSET = "utf-8";  
 // LOGO宽度  
    private static final int LOGO_WIDTH = 28;  
    // LOGO高度  
    private static final int LOGO_HEIGHT = 28;
    
    /**
     * 确定一个时间，求是否在某个时间点与某个时间点后的时间之中
     * @param nowTime
     * @param beginTime
     * @param endTime
     * @return
     */
    public static boolean withinTheScopeOf(Date nowTime,Date beginTime, Date endTime) {
    	
    	Calendar now = Calendar.getInstance();
    	now.setTime(nowTime);
    	
        Calendar begin = Calendar.getInstance();
        begin.setTime(beginTime);

        Calendar end = Calendar.getInstance();
        end.setTime(endTime);
        // 判断是否同天，同天直接调用util中方法进行比较
        if (DateUtils.isSameDay(beginTime, endTime)){
        	return Utils.belongCalendar(nowTime, beginTime, endTime);
       }else{
    	   // 则判断时间是否在开始时间之后，在之后则判断完毕，在之前继续判断，因为存在开始时间与结束时间不在同一天
    	   if(now.after(begin)){
    		   return true;
    	   }else{
    		   // 结束时间减去一天，判断是否在时间之内，返回true
    		   end.add(Calendar.DATE, -1);
    		   if(now.before(end)){
    			   return true;
    		   }
    	   }
       }
        return false;
    }
    
    /**
	 * 获取视频的第2秒的截图到oss
	 * @throws Exception 
	 */
	public static int submitSnapshotJob(String videoUrl) throws Exception {
		Map<String, String> parameterMap = new HashMap<String, String>();
		// 请求公共参数
		parameterMap.put("Version", "2014-06-18");
		parameterMap.put("AccessKeyId", Play.configuration.getProperty("ossBossAccessKeyId")); 
		parameterMap.put("Timestamp",Utils.formatIso8601Date(new Date()));
		parameterMap.put("SignatureMethod", "HMAC-SHA1");
		parameterMap.put("SignatureVersion", "1.0");
		parameterMap.put("SignatureNonce", UUID.randomUUID().toString()); 
		parameterMap.put("Format", "XML");
		
		parameterMap.put("Action", "SubmitSnapshotJob");
		parameterMap.put("Input", "{\"Bucket\":\""+Play.configuration.getProperty("ossBucketName")+"\",\"Location\":\""+Play.configuration.getProperty("ossLocation")+"\",\"Object\":\""+videoUrl+"\"}"); 
		parameterMap.put("SnapshotConfig", "{\"OutputFile\": {\"Bucket\": \""+Play.configuration.getProperty("ossBucketName")+"\",\"Location\": \""+Play.configuration.getProperty("ossLocation")+"\",\"Object\": \""+videoUrl+"_jpg\"},\"Time\": \"2000\"}"); 
		
		
		String canonicalizedQuery = Utils.buildCanonicalizedQueryString(parameterMap);
		String stringToSign= Utils.buildStringToSign(canonicalizedQuery);
		String signature = Utils.buildSignature(Play.configuration.getProperty("ossBossAccessKeySecret") , stringToSign);
		String url = Utils.buildRequestURL("http://mts.cn-shenzhen.aliyuncs.com","Signature",signature,parameterMap);
		
		// 建立连接，获取session_key和openid
		AsyncHttpClient client = new AsyncHttpClient();
		Future<Response> f = client.prepareGet(url).execute();
		Response response = f.get();
		return 1;
	}
    
    /**
     * 判断时间是否在时间段内
     * 	SimpleDateFormat df = new SimpleDateFormat("HH:mm");// 设置日期格式
     *		Date now = df.parse(df.format(new Date()));
     *		Date beginTime = df.parse("21:00");
     *		Date endTime = df.parse("24:00");
     * @param nowTime
     * @param beginTime
     * @param endTime
     * @return
     */
    public static boolean belongCalendar(Date nowTime, Date beginTime, Date endTime) {
        Calendar date = Calendar.getInstance();
        date.setTime(nowTime);

        Calendar begin = Calendar.getInstance();
        begin.setTime(beginTime);

        Calendar end = Calendar.getInstance();
        end.setTime(endTime);

        if (date.after(begin) && date.before(end)) {
            return true;
        } else {
            return false;
        }
    }
    
	/**
     * 生成包含字符串信息的二维码图片
     * 
     */
	public static BufferedImage createImage(String content, String logoPath, boolean needCompress,int orcodeSize) throws Exception {  
        Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();  
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);  
        hints.put(EncodeHintType.CHARACTER_SET, CHARSET);  
        hints.put(EncodeHintType.MARGIN, 1);  
        BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, orcodeSize, orcodeSize,  
                hints);  
        int width = bitMatrix.getWidth();  
        int height = bitMatrix.getHeight();  
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);  
        for (int x = 0; x < width; x++) {  
            for (int y = 0; y < height; y++) {  
                image.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);  
            }  
        }  
        if (logoPath == null || "".equals(logoPath)) {  
            return image;  
        }  
        // 插入图片  
        insertImage(image, logoPath, needCompress,orcodeSize);  
        return image;  
    }  
	
	/** 
     * 插入LOGO 
     *  
     * @param source 
     *            二维码图片 
     * @param logoPath 
     *            LOGO图片地址 
     * @param needCompress 
     *            是否压缩 
     * @throws Exception 
     */  
    private static void insertImage(BufferedImage source, String logoPath, boolean needCompress,int orcodeSize) throws Exception {  
        File file = new File(logoPath);  
        if (!file.exists()) {  
            throw new Exception("logo file not found.");  
        }  
        Image src = ImageIO.read(new File(logoPath));  
        int width = src.getWidth(null);  
        int height = src.getHeight(null);  
        if (needCompress) { // 压缩LOGO  
            if (width > LOGO_WIDTH) {  
                width = LOGO_WIDTH;  
            }  
            if (height > LOGO_HEIGHT) {  
                height = LOGO_HEIGHT;  
            }  
            Image image = src.getScaledInstance(width, height, Image.SCALE_SMOOTH);  
            BufferedImage tag = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);  
            Graphics g = tag.getGraphics();  
            g.drawImage(image, 0, 0, null); // 绘制缩小后的图  
            g.dispose();  
            src = image;  
        }  
        // 插入LOGO  
        Graphics2D graph = source.createGraphics();  
        int x = (orcodeSize - width) / 2;  
        int y = (orcodeSize - height) / 2;  
        graph.drawImage(src, x, y, width, height, null);  
        Shape shape = new RoundRectangle2D.Float(x, y, width, width, 6, 6);  
        graph.setStroke(new BasicStroke(3f));  
        graph.draw(shape);  
        graph.dispose();  
    }  
      
    /**
     * 读二维码并输出携带的信息
     */
    public static void readQrCode(InputStream inputStream) throws IOException{  
        //从输入流中获取字符串信息
        BufferedImage image = ImageIO.read(inputStream);  
        //将图像转换为二进制位图源
        LuminanceSource source = new BufferedImageLuminanceSource(image);  
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));  
        QRCodeReader reader = new QRCodeReader();  
        Result result = null ;  
        try {
         result = reader.decode(bitmap);  
        } catch (ReaderException e) {
            e.printStackTrace();  
        }
        System.out.println(result.getText());  
    }
	
	private static final String ISO8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	private static final String ENCODE_TYPE = "UTF-8";
	private static final String ALGORITHM = "HmacSHA1";
	private static final String HTTP_METHOD = "GET";
	private static final String SEPARATOR = "&";
	private static final String EQUAL = "=";
	
    /**
     * 获取iso8601 时间
     * @param date
     * @return
     */
    public static String formatIso8601Date(Date date) {
        SimpleDateFormat df = new SimpleDateFormat(ISO8601_DATE_FORMAT);
        df.setTimeZone(new SimpleTimeZone(0, "GMT"));
        return df.format(date);
    }
    
	/**
	 * 建立请求url，
	 * @param signature
	 * @param parameterMap
	 * @return
	 * @throws Exception
	 */
	public static String buildRequestURL(String main,String firstParmName,String firstParmValue, Map<String, String> parameterMap) throws Exception {
        // 生成请求URL
        StringBuilder temp = new StringBuilder(main+"?");
        temp.append(URLEncoder.encode(firstParmName, ENCODE_TYPE)).append("=").append(firstParmValue);
        for (Map.Entry<String, String> e : parameterMap.entrySet()) {
            temp.append("&").append(percentEncode(e.getKey())).append("=").append(percentEncode(e.getValue()));
        }
        return temp.toString();
    }
	
	/**
	 * 使用请求参数构造规范化的请求字符串
	 */
    public static String buildCanonicalizedQueryString(Map<String, String> parameterMap) throws Exception {
        // 对参数进行排序
        List<String> sortedKeys = new ArrayList<String>(parameterMap.keySet());
        Collections.sort(sortedKeys);
        StringBuilder temp = new StringBuilder();
        for (String key : sortedKeys) {
            // 此处需要对key和value进行编码
            String value = parameterMap.get(key);
            temp.append(SEPARATOR).append(percentEncode(key)).append(EQUAL).append(percentEncode(value));
        }
        return temp.toString().substring(1);
    }
    
    /**
	 * HMAC签名
	 */
    public static String buildStringToSign(String canonicalizedQueryString) throws Exception {
        // 生成stringToSign字符
        StringBuilder temp = new StringBuilder();
        temp.append(HTTP_METHOD).append(SEPARATOR);
        temp.append(percentEncode("/")).append(SEPARATOR);
        // 此处需要对canonicalizedQueryString进行编码
        temp.append(percentEncode(canonicalizedQueryString));
        return temp.toString();
    }
    
    /**
  	 * Base64编码
  	 */
    public static String buildSignature(String keySecret, String stringToSign) throws Exception {
        SecretKey key = new SecretKeySpec((keySecret + SEPARATOR).getBytes(ENCODE_TYPE), SignatureMethod.HMAC_SHA1);
        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(key);
        byte[] hashBytes = mac.doFinal(stringToSign.toString().getBytes(ENCODE_TYPE));
        byte[] base64Bytes = new com.aliyun.openservices.shade.org.apache.commons.codec.binary.Base64().encode(hashBytes);
        String base64UTF8String = new String(base64Bytes, "utf-8");
        return URLEncoder.encode(base64UTF8String, ENCODE_TYPE);
    }
    
    /**
  	 * 构造请求的URL
  	 */
    private static String percentEncode(String value) throws Exception {
        return URLEncoder.encode(value, ENCODE_TYPE).replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
    }
	
	// 出生日期字符串转化成Date对象
			public static Date parse(String strDate) throws ParseException {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
				return sdf.parse(strDate);
			}

			// 由出生日期获得年龄
			public static int getAge(Date birthDay) throws Exception {
				Calendar cal = Calendar.getInstance();

				if (cal.before(birthDay)) {
					throw new IllegalArgumentException("The birthDay is before Now.It's unbelievable!");
				}
				int yearNow = cal.get(Calendar.YEAR);
				int monthNow = cal.get(Calendar.MONTH);
				int dayOfMonthNow = cal.get(Calendar.DAY_OF_MONTH);
				cal.setTime(birthDay);

				int yearBirth = cal.get(Calendar.YEAR);
				int monthBirth = cal.get(Calendar.MONTH);
				int dayOfMonthBirth = cal.get(Calendar.DAY_OF_MONTH);

				int age = yearNow - yearBirth;

				if (monthNow <= monthBirth) {
					if (monthNow == monthBirth) {
						if (dayOfMonthNow < dayOfMonthBirth)
							age--;
					} else {
						age--;
					}
				}
				return age;
			}
			
			/**
			 * 得到某个时间点后N天后的日期
			 * @param str 当前时间 
			 * @param n 天数
			 * @return
			 */
			public static String afterNDayByParam(String str,int n){   
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
				// 将字符串的日期转为Date类型，ParsePosition(0)表示从第一个字符开始解析
				Date date = sdf.parse(str, new ParsePosition(0));
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);
				calendar.add(Calendar.DATE, n);
				Date date1 = calendar.getTime();
				String out = sdf.format(date1);
				return  out;
			}  
	
			/**
			 * 得到某个时间点后N分钟后的日期
			 * @param n
			 * @return
			 */
			public static String afterNMinByParam(String str,int n){   
		        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		        // 将字符串的日期转为Date类型，ParsePosition(0)表示从第一个字符开始解析
		        Date date = sdf.parse(str, new ParsePosition(0));
		        Calendar calendar = Calendar.getInstance();
		        calendar.setTime(date);
		        calendar.add(Calendar.MINUTE, n);
		        Date date1 = calendar.getTime();
		        String out = sdf.format(date1);
		        return  out;
			}  
	/**
	 * 得到N分钟后的日期
	 * @param n
	 * @return
	 */
	public static String afterNMin(int n){   
		Calendar calendar2 = Calendar.getInstance();
		  SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMddHHmmss");
		  calendar2.add(Calendar.MINUTE, n);
		  return sdf2.format(calendar2.getTime());
	}  
	/**
	 * 得到N天后的日期
	 * @param n
	 * @return
	 */
	public static String afterNDay(int n){   
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
	 * 提供精确的加法运算。
	 * 
	 * @param v1
	 *            被加数
	 * @param v2
	 *            加数
	 * @return 两个参数的和
	 */
	public static BigDecimal strAdd(String v1, String v2) {
		BigDecimal b1 = new BigDecimal(v1);
		BigDecimal b2 = new BigDecimal(v2);
		return b1.add(b2);
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
	 * 精确的除法运算。除不尽时，由scale参数指 定精度 四舍五入。string
	 * 
	 * @param v1
	 *            被除数
	 * @param v2
	 *            除数
	 * @param scale
	 *            表示需要精确到小数点以后几位。
	 * @return 两个参数的商
	 */
	public static BigDecimal strDiv(String v1, String v2, int scale) {
		if (scale < 0) {
			throw new IllegalArgumentException("The scale must be a positive integer or zero");
		}
		BigDecimal b1 = new BigDecimal(v1);
		BigDecimal b2 = new BigDecimal(v2);
		return b1.divide(b2, scale, BigDecimal.ROUND_HALF_UP);
	}
	/**  
     * 对一个数字取精度  
     * @param v  
     * @param scale  
     * @return  
     */   
    public static BigDecimal round(String v, int scale) {   
        if (scale < 0) {   
            throw new IllegalArgumentException(   
                    "The scale must be a positive integer or zero");   
        }   
        BigDecimal b = new BigDecimal(v);   
        BigDecimal one = new BigDecimal("1");   
        return b.divide(one, scale, BigDecimal.ROUND_HALF_UP);   
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
	 * 比较大小 如果v1 大于v2 则 返回true 否则false
	 * 
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static boolean strcompareTo(String v1, String v2) {
		BigDecimal b1 = new BigDecimal(v1);
		BigDecimal b2 = new BigDecimal(v2);
		int bj = b1.compareTo(b2);
		boolean res;
		if (bj > 0)
			res = true;
		else
			res = false;
		return res;
	}

	/**
	 * 对BODY进行AES解密
	 * 
	 * @param c
	 * @return
	 */
	public static <T> T getAesBody(Class<T> c, Jedis jedis) {
		InputStream is = request.body;
		String json = null;
		try {
			// 客户端传过来的是16进制字符串，流需要单独赋值给String
			String body = IOUtils.toString(is, "UTF-8");
			byte[] hexjson = Codec.hexStringToByte(body);

			// 获取token
			String token = params.get("token");
			if (token != null) {
				// 获取redis中的aeskey
				String cacheAeskey = jedis.get(token.substring(0, token.length() - 4) + ":aeskey");
				json = decryptAES(hexjson, cacheAeskey);
			} else {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				JsonObject jsonObject = new JsonObject();
				jsonObject.addProperty("code", "509");
				jsonObject.addProperty("msg", "token验证失败,请重新登陆");
				renderJSON(jsonObject);
			}

		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("获取API请求body出错", e.getMessage());
			renderJSON(Utils.apiError());
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
			String endpoint ="http://"+Play.configuration.getProperty("ossEndPoint");
			// accessKey请登录https://ak-console.aliyun.com/#/查看
			String accessKeyId = Play.configuration.getProperty("ossBossAccessKeyId");
			String accessKeySecret = Play.configuration.getProperty("ossBossAccessKeySecret");
			// 创建OSSClient实例
			OSSClient client = new OSSClient(endpoint, accessKeyId, accessKeySecret);
			client.putObject(Play.configuration.getProperty("ossBucketName"),  filename, is);
			// 关闭client
			client.shutdown();
			re = 1;
		} catch (Exception e) {
			Logger.error("保存图片到oss出错", e.getMessage());
		}
		return re;
	}

	/**
	 * 保存内容文件到oss,写成一般方法，如是void方法，会走play返回
	 */
	public static int saveFile2OSS(String filename, String content) {
		int re = 0;
		try {
			// endpoint以杭州为例，其它region请按实际情况填写
			String endpoint = "oss-cn-hangzhou.aliyuncs.com";
			// accessKey请登录https://ak-console.aliyun.com/#/查看
			String accessKeyId = "LTAIvLAcjddwNLsa";
			String accessKeySecret = "W4KiGhCAQ6OHfbHxPk8fWQCUMY5jNe";
			// 创建OSSClient实例
			OSSClient client = new OSSClient(endpoint, accessKeyId, accessKeySecret);
			client.putObject("idollyfile", "file/" + filename, new ByteArrayInputStream(content.getBytes()));
			// 关闭client
			client.shutdown();
			re = 1;
		} catch (Exception e) {
			Logger.error("保存文件到oss出错", e.getMessage());
		}
		return re;
	}

	// AES加密初始向量，需要与iOS端的统一
	private static final String IV_STRING = "16-Bytes--String";

	/**
	 * AES加密
	 * 
	 * @param content
	 * @param key
	 * @return
	 * @throws Exception
	 */
	public static String encryptAES(String content, String key)
            throws Exception {
		byte[] byteContent = content.getBytes("UTF-8");
		// 注意，为了能与 iOS 统一
		// 这里的 key 不可以使用 KeyGenerator、SecureRandom、SecretKey 生成
        byte[] enCodeFormat = key.getBytes();
        SecretKeySpec secretKeySpec = new SecretKeySpec(enCodeFormat, "AES");
        byte[] initParam = IV_STRING.getBytes();
        IvParameterSpec ivParameterSpec = new IvParameterSpec(initParam);
     // 指定加密的算法、工作模式和填充方式
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
        byte[] encryptedBytes = cipher.doFinal(byteContent);
        // 同样对加密后数据进行 base64 编码
        Encoder encoder = Base64.getEncoder();
        return bytesToHexString(encoder.encodeToString(encryptedBytes).getBytes());
    }
	
	/***
     * 将数组转为16进制的字符串
     *
     * @param src 需要转换的数组
     * @return 16进制的字符串
     */
    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }
	/**
	 * AES解密
	 * 
	 * @param content
	 * @param key
	 * @return
	 * @throws Exception
	 */
	public static String decryptAES(byte[] content, String key) throws Exception {
		// base64 解码
		Decoder decoder = Base64.getDecoder();
		byte[] encryptedBytes = decoder.decode(content);
		byte[] enCodeFormat = key.getBytes();
		SecretKeySpec secretKey = new SecretKeySpec(enCodeFormat, "AES");
		byte[] initParam = IV_STRING.getBytes();
		IvParameterSpec ivParameterSpec = new IvParameterSpec(initParam);
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
		byte[] result = cipher.doFinal(encryptedBytes);
		return new String(result, "UTF-8");
	}

}
