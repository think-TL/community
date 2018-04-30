package models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

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
}
