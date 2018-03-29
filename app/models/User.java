package models;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;

import play.db.DB;
import play.db.jpa.GenericModel;
import play.db.jpa.JPA;
import play.db.jpa.Model;

@Entity
@Table(name = "t_user")
public class User extends GenericModel {
	@Id
	// id
	public String user_id;
	// 用户名
	public String user_name;
	// 用户登录密码
	public String user_passowrd;
	// 用户认证标识 0 未认证 /1 认证
	public int user_status;
	// 信用级别 0 未认证 1 信用一般 2 信用良好 3 信用优秀
	public int credit_cmt;
	// 信用值
	public String credit;
	//真实姓名
	public String realname;
	// 身份证
	public String id_card;
	// 小区具体位置
	public String address;
	// 出生年月日
	public String birth;
	// 性别 0 男 1 女
	public int sex;
	// 头像
	public String head_img;
	// 邮箱
	public String emali;
	// 添加时间
	public String add_time;
	// 修改时间
	public String upd_time;
	// 操作人
	public String bizuser_id;
	// 删除状态(1：正常 0：删除)
	public String deleteflag;
}
