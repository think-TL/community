package models;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import play.db.jpa.GenericModel;

/**
* 订单表
* 
* @author bd
*
*/
@Entity
@Table(name = "t_order")
public class Order extends GenericModel{
	@Id
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
	// 删除状态(1：正常 0：删除)
	public String deleteflag;
}
