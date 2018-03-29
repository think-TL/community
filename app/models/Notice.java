package models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import play.db.jpa.GenericModel;

@Entity
@Table(name = "t_notice")
public class Notice extends GenericModel{
	@GeneratedValue(generator = "system-uuid")
	@GenericGenerator(name = "system-uuid", strategy = "uuid")
	@Id
	// 主键
	public String id;
	// 标题
	public String title;
	// 管理员id
	public String bizuer_id;
	// 通告内容
	public String content;
	// 发布状态 0 未发布 1 发布
	public int publish_flag;
	// 通告分类
	public String type_id;
	// 添加时间
	public String add_time;
	// 修改时间
	public String upd_time;
	// 操作人
	public String bizuser_id;
	// 删除状态(1：正常 0：删除)
	public String deleteflag;
}
