package controllers.boss;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import models.Bizuser;
import models.BizuserRole;
import models.Role;
import models.RolePermission;
import play.Logger;
import play.data.validation.Valid;
import play.db.DB;
import play.mvc.Controller;
import play.mvc.With;
import redis.clients.jedis.Jedis;

/**
 * 排行统计管理
 * 
 */
@With(AdminIntercepter.class)
public class AdminMgr extends Controller {
	
	private static DataSource ds = DB.getDataSource();
	private static QueryRunner run = new QueryRunner(ds);
	
	/**
	 *  权限管理
	 * @return
	 */
	public static void permission(String roleId) {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		List permissionList = null;
		try {
			JsonObject jsonObject = new JsonObject();
			
			// 获取一级权限目录
			String sql = "select name,id from t_permission where permission_id = '0' and id != '3' ";
			List listA = run.query(sql, new MapListHandler());
			// 获取二级权限目录
			sql = "select name,id,permission_id from t_permission where permission_id !=  '0'";
			List listB = run.query(sql, new MapListHandler());
			// 获取一般管理员拥有的权限list
			sql = "select permission_id from t_role_permission where role_id = ?";
			List adminP = run.query(sql, new MapListHandler(),roleId);
			String pStr = "";
			for (Object object : adminP) {
				Map m = (Map)object;
				pStr += "," + m.get("permission_id") + ",";
			}
			
			List listAll = new ArrayList<>();
			for (int i = 0; i < listA.size(); i++) {
				Map mapAll = new HashMap<>();

				Map mapA = (Map)listA.get(i);
				mapAll.put("name",mapA.get("name").toString());
				mapAll.put("id",mapA.get("id").toString());
				mapAll.put("open", true);
				
				List permissionB = new ArrayList<>();
				// 遍历二级目录
				for (int j = 0; j < listB.size(); j++) {
					Map mapB = (Map)listB.get(j);
					//找到一级目录对应的二级
					if(mapA.get("id").equals(mapB.get("permission_id"))){
						// 查询一般管理员之前是否有此权限
						int location = pStr.indexOf("," + mapB.get("id") + ",");
						// 拥有此权限则选中checkBox
						if(location > -1){
							mapAll.put("checked", true);
							mapB.put("checked", true);
						}
						permissionB.add(mapB);
					}
				}
				// 把一级目录对应的二级加入children
				mapAll.put("children", new Gson().toJsonTree(permissionB));
				listAll.add(mapAll);
			}
			String jsonStr = new Gson().toJsonTree(listAll).toString();
			// 获取权限列表
			permissionList = IndexMgr.getPermission(jedis);
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			render(permissionList,jsonStr,roleId);
		} catch (Exception e) {
			Logger.error("权限管理错误", e.getMessage());
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
		}
	}
	
	/**
	 *  更改权限
	 * @return
	 */
	public static void updatePermission(String idStr,String roleId) {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			// 删除一般管理员之前权限
			run.update("delete from t_role_permission where role_id = ?",roleId);
			
			// 截取 , 成数组
			String[] idArray = idStr.substring(0,idStr.length() -1).split(",");
			
			//  添加权限
			RolePermission rp ;
			for (int i = 0; i < idArray.length; i++) {
				rp = new RolePermission();
				rp.deleteflag = "1";
				rp.role_id = roleId;
				rp.itime = Utils.getCurrentTime();
				rp.bizuser_id = jedis.hmget(session.getId() + ":bizuser:info", "id").get(0);
				rp.permission_id = idArray[i];
				rp.save();
			}
			
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			roleAdminMgr("修改成功");
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("更改权限错误", e.getMessage());
		}
	}
	
	/**
	 *  查看角色管理
	 * @return
	 */
	public static void roleAdminMgr(String msg) {
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
	 *  查看角色管理数据
	 * @return
	 */
	public static void roleAdminData(int limit, int offset,String name) {
		try {
			JsonObject jsonObject = new JsonObject();
			
			StringBuffer sql = new StringBuffer("SELECT id,rolename FROM t_role r WHERE r.deleteflag = 1 ");
			
			StringBuffer wheres = new StringBuffer();
			wheres.append("AND r.id <> 0");
			
			// 获得数据
			List statisticsList = run.query(sql.toString()  + wheres.toString() + " order by r.itime+0 desc limit ?,?",new MapListHandler(),offset,limit);
			// 获得总条数
			List list = run.query("SELECT COUNT(r.id) as total FROM t_role r WHERE r.deleteflag = 1  "  + wheres.toString() , new MapListHandler());
		
			Map map = (Map) list.get(0);
			jsonObject.addProperty("total", map.get("total").toString());
			jsonObject.add("rows", new Gson().toJsonTree(statisticsList));
			renderJSON(jsonObject);
		} catch (Exception e) {
			Logger.error("查看角色管理数据错误"+ e.getMessage());
		}
	}
	
	/**
	 *  删除角色
	 * @return
	 */
	public static void delRoleAdmin(String roleid) {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			String sql = "select id from t_bizuser_role where role_id = ? and deleteflag = 1 limit 1";
			List list = run.query(sql, new MapListHandler(),roleid);
			if(list.size() > 0){
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				roleAdminMgr("请先删除角色的管理员！");
			}
			
			sql = "update t_role set deleteflag = '0',utime=?,bizuser_id=? where id = ? ";
			run.update(sql, Utils.getCurrentTime(),jedis.hmget(session.getId() + ":bizuser:info", "id").get(0).toString(),roleid);
			
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			roleAdminMgr(null);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("删除角色"+e.getMessage());
		}
	}
	
	/**
	 *  增加角色
	 * @return
	 */
	public static void addRoleAdmin(Role role) {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			role.deleteflag = "1";
			// 加入时间
			role.itime = Utils.getCurrentTime();
			// 获取操作人id
			role.bizuser_id = jedis.hmget(session.getId() + ":bizuser:info", "id").get(0);
			role = role.save();			
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			roleAdminMgr(null);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("增加角色"+e.getMessage());
		}
	}
	
	
	/**
	 *  查看一般管理员管理
	 * @return
	 */
	public static void commonlyAdminMgr(String msg) {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		List permissionList = null;
		List roleList = null;
		try {
			permissionList = IndexMgr.getPermission(jedis);
			String sql = "SELECT id,rolename FROM t_role WHERE deleteflag = 1 AND id <> 0";
			roleList = run.query(sql, new MapListHandler());
			System.out.println("==="+roleList.toString());
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
		}
		render(permissionList,roleList,msg);
	}
	
	/**
	 *  删除一般管理员管理
	 * @return
	 */
	public static void delCommonlyAdmin(String userid) {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			String sql = "update t_bizuser set deleteflag = '0',utime=?,bizuser_id=? where id = ? ";
			run.update(sql, Utils.getCurrentTime(),jedis.hmget(session.getId() + ":bizuser:info", "id").get(0).toString(),userid);
			// 删除一般管理员角色
			sql = "update t_bizuser_role set deleteflag = '0',utime=?,bizuser_id=? where user_id = ? ";
			run.update(sql, Utils.getCurrentTime(),jedis.hmget(session.getId() + ":bizuser:info", "id").get(0).toString(),userid);
			
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			commonlyAdminMgr(null);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("删除一般管理员管理错误", e.getMessage());
		}
	}
	
	/**
	 *  查看一般管理员管理
	 * @return
	 */
	public static void ListCommonlyAdminData(int limit, int offset,String userid) {
		try {
			JsonObject jsonObject = new JsonObject();
			
			StringBuffer sql = new StringBuffer(""
					+ " SELECT "
						+ " b.id,b.name,r.rolename "
					+ " FROM "
						+ " t_bizuser b "
					+ " LEFT JOIN t_bizuser_role br ON b.id = br.user_id "
					+ " LEFT JOIN t_role r ON r.id = br.role_id "
					+ " AND b.deleteflag = '1' ");
			
			StringBuffer wheres = new StringBuffer();
			// 判断是否根据id查找
			if(userid != null && !"".equals(userid)){
				wheres.append(" and b.id = '" + Utils.getSecurityParm(userid) + "'");
			}
			
			// 获得数据
			List statisticsList = run.query(sql.toString() + wheres.toString() + "  limit ?,?",new MapListHandler(),offset,limit);
			// 获得总条数
			List list = run.query("select count(b.id) as total  from t_bizuser b left join t_bizuser_role br on b.id = br.user_id where br.id = '1'  and b.deleteflag = '1' " + wheres.toString() , new MapListHandler());
		
			Map map = (Map) list.get(0);
			jsonObject.addProperty("total", map.get("total").toString());
			jsonObject.add("rows", new Gson().toJsonTree(statisticsList));
			renderJSON(jsonObject);
		} catch (Exception e) {
			Logger.error("查看一般管理员管理错误", e.getMessage());
		}
	}
	
	/**
	 * 增加一般管理员
	 */
	public static void addCommonlyAdmin(Bizuser biz,BizuserRole bizRole) {
		// 获取jedis
		Jedis jedis = RedisUtil.getJedis();
		try {
			// 表单验证
			validation.valid(biz);
			if (validation.hasErrors()) {
				// 关闭链接
				RedisUtil.closeJedisPool(jedis);
				commonlyAdminMgr("请完善信息");
			}
			biz.deleteflag = "1";
			// 加入时间
			biz.itime = Utils.getCurrentTime();
			// 获取操作人id
			biz.bizuser_id = jedis.hmget(session.getId() + ":bizuser:info", "id").get(0);
			biz = biz.save();
			
			BizuserRole br = new BizuserRole();
			br.user_id = biz.id;
			br.deleteflag = "1";
			br.itime = Utils.getCurrentTime();
			br.bizuser_id = jedis.hmget(session.getId() + ":bizuser:info", "id").get(0);
			br.role_id = bizRole.role_id;
			br.save();
			
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			// 操作完成
			commonlyAdminMgr(null);
		} catch (Exception e) {
			// 关闭链接
			RedisUtil.closeJedisPool(jedis);
			Logger.error("增加一般管理员错误", e.getMessage());
		}
	}
	
}
