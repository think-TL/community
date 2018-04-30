package models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;

import play.db.jpa.Blob;
import play.db.jpa.GenericModel;

/**
 * 资源表
 * 
 * @author tl
 *
 */
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
	public String imgurl;
	@Transient
	public Blob img1;
	// 图片描述2
	public String imgurl2;
	@Transient
	public Blob img2;
	// 图片描述3
	public String imgurl3;
	@Transient
	public Blob img3;
	// 发布价格  界面显示
	public double price;
	// 发布者id   详细信息 下单之后显示
	public String user_id;
	// 所需信用值 显示
	public String credit_number;
	// 添加时间
	public String add_time;
	// 修改时间
	public String upd_time;
	// 操作人
	public String bizuser_id;
	// 删除状态(1：正常 0：删除)
	public String deleteflag;
}
