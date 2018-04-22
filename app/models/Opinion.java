package models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import play.db.jpa.GenericModel;

/**
 * 意见反馈表
 *
 */
@Entity
@Table(name = "t_opinion")
public class Opinion extends GenericModel{
	@Id
	@GeneratedValue(generator = "system-uuid")
	@GenericGenerator(name = "system-uuid", strategy = "uuid")
	// id
	public String id;
	// 反馈内容
	public String content;
	// 用户id
	public String user_id;
	// 用户idnum
	public String user_idnum;
	// 用户名
	public String user_nickname;
	// 阅读状态(1:已读 0:未读)
	public String status;
	// 新增时间
	public String itime;
	// 修改时间
	public String utime;
	// 操作人
	public String bizuser_id;
}
