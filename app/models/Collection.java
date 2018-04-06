package models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import play.db.jpa.GenericModel;

/**
 * 收藏表
 * 
 * @author tl
 *
 */
@Entity
@Table(name = "t_collection")
public class Collection extends GenericModel{
	@Id
	@GeneratedValue(generator = "system-uuid")
	@GenericGenerator(name = "system-uuid", strategy = "uuid")
	// 主键
	public String id;
	// 资源id
	public String resources_id;
	// 收藏者id
	public String user_id;
	// 添加时间
	public String add_time;
	// 修改时间
	public String upd_time;
	// 删除状态(1：正常 0：删除)
	public String deleteflag;
}
