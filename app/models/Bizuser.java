package models;

import java.io.File;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;
import org.joda.time.DateTime;

import play.data.validation.Required;
import play.db.jpa.Blob;
import play.db.jpa.GenericModel;

/**
 * 管理员用户表
 * 
 * @author bd
 *
 */
@Entity
@Table(name = "t_bizuser")
public class Bizuser extends GenericModel {
	@Id
	@GeneratedValue(generator = "system-uuid")
	@GenericGenerator(name = "system-uuid", strategy = "uuid")
	// id
	public String id;
	// 用户名
	@Required(message = "请填写用户名")
	public String name;
	// 密码
	public String password;
	// 新增时间
	public String itime;
	// 修改时间
	public String utime;
	// 操作人
	public String bizuser_id;
	// 删除状态
	public String deleteflag;
	
}
