package models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import play.db.jpa.GenericModel;

/**
 * 公告表
 * 
 * @author tl
 *
 */
@Entity
@Table(name = "t_notice")
public class Notice extends GenericModel{
	@Id
	@GeneratedValue(generator = "system-uuid")
	@GenericGenerator(name = "system-uuid", strategy = "uuid")
	// 主键
	public String id;
	// 公告标题
	public String title;
	// 通告内容
	public String content;
	// 添加时间
	public String add_time;
	// 修改时间
	public String upd_time;
	// 操作人
	public String bizuser_id;
	// 公告删除状态(1：正常 0：删除)
	public String deleteflag;
}
