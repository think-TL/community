package models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import play.db.jpa.GenericModel;

/**
 * 资源类型表
 * 
 * @author tl
 *
 */
@Entity
@Table(name = "t_resources_type")
public class ResourcesType extends GenericModel{
	@Id
	@GeneratedValue(generator = "system-uuid")
	@GenericGenerator(name = "system-uuid", strategy = "uuid")
	// id
	public String id;
	// 资源名称
	public String name;
	// 添加时间
	public String add_time;
	// 修改时间
	public String upd_time;
	// 操作人
	public String bizuser_id;
	// 删除状态(1：正常 0：删除)
	public String deleteflag;
}
