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
 * 权限表
 * 
 * @author bd
 *
 */
@Entity
@Table(name = "t_permission")
public class Permission extends GenericModel {
	@Id
	@GeneratedValue(generator = "system-uuid")
	@GenericGenerator(name = "system-uuid", strategy = "uuid")
	// id
	public String id;
	// 权限名
	public String name;
	// 跳转链接
	public String url;
	// 图标
	public String img;
	// 父级权限id
	public String permission_id;
	// 界面选中标识
	public String activeflag;
	// 父级权限排序
	public String sequence;
	
}
