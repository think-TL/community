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
 * 角色权限表
 * 
 * @author bd
 *
 */
@Entity
@Table(name = "t_role_permission")
public class RolePermission extends GenericModel {
	@Id
	@GeneratedValue(generator = "system-uuid")
	@GenericGenerator(name = "system-uuid", strategy = "uuid")
	// id
	public String id;
	// 角色id
	public String role_id;
	// 菜单id
	public String permission_id;
	// 新增时间
	public String itime;
	// 修改时间
	public String utime;
	// 操作人
	public String bizuser_id;
	// 删除状态(1：正常 0：删除)
	public String deleteflag;
	
}
