package models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;

import play.db.jpa.GenericModel;

/**
* 订单表
* 
* @author TL
*
*/
@Entity
@Table(name = "t_order")
public class Order extends GenericModel{
	@Id
	@GeneratedValue(generator = "system-uuid")
	@GenericGenerator(name = "system-uuid", strategy = "uuid")
	public String id;
	// 下单用户id
	public String user_id;
	// 资源id
	public String resources_id;
	// 资源名字
	public String resources_name;
	// 资源类型
	public String resources_type_id;
	// 资源描述
	public String resources_cmt;
	// 资源图片
	public String resources_img1;
	// 资源图片
	public String resources_img2;
	// 资源图片
	public String resources_img3;
	// 资源价格
	public double resources_price;
	// 资源信用值
	public String resources_creditnumber;
	// 新增时间
	public String itime;
	// 修改时间
	public String utime;
	// 操作人
	public String bizuser_id;
	//订单状态  0未确认  1 为进行中  2  取消  3完成   
	public String order_status;
	//配送状态  0 未发货  1 已发货   //这两个字段，我也不是很确定 你看看 有没有撒子问题  我觉得应该是这样的 用户下单后有 卖家那边选择发货 或者取消订单  
	public String shipping_status;
	// 删除状态(1：正常 0：删除)
	public String deleteflag;
	//卖家id
	public String maiuser_id;
	//卖家电话
	public String phone;
	//卖家名字
	public String name;
	//消费者名字
	public String consumename;
	//卖家地址
	public String address;
	//订单单号
	public String orderno;
	
	//翻页页码,不存数据库
	@Transient
	public Integer page;
	//不存数据库
	@Transient
	public String startTime;
	//不存数据库
	@Transient
	public String endTime;
}
