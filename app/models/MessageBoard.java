package models;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import play.db.jpa.GenericModel;

@Entity
@Table(name = "t_message_board")
public class MessageBoard  extends GenericModel{
	@Id
	// 主键
	public String id;
	// 留言板内容
	public String content;
	//评论文章id
	public String notice_id;
	// 留言板id
	public String user_id;
	// 添加时间
	public String add_time;
	// 修改时间
	public String upd_time;
	// 管理员阅读状态
	public String status;
	// 操作人
	public String bizuser_id;
	// 删除状态(1：正常 0：删除)
	public String deleteflag;
}
