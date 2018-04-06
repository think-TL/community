package models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import play.db.jpa.GenericModel;

@Entity
@Table(name = "t_resources")
public class Resources  extends GenericModel{
	@Id
	@GeneratedValue(generator = "system-uuid")
	@GenericGenerator(name = "system-uuid", strategy = "uuid")
	// 主键
	public String id;
	// 资源名称
	public String name;
	// 资源类型
	public String resources_type_id;
	// 资源描述
	public String cmt;
	// 图片描述1
	public String img1;
	// 图片描述2
	public String img2;
	// 图片描述3
	public String img3;
	// 成交价格
	public double price;
	// 发布者id
	public String user_id;
	// 接受者id
	public String receive_ser_id;
	// 添加时间
	public String add_time;
	// 修改时间
	public String upd_time;
	// 操作人
	public String bizuser_id;
	// 删除状态(1：正常 0：删除)
	public String deleteflag;
}
