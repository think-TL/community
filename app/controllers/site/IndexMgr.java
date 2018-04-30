package controllers.site;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;

import play.db.DB;
import play.mvc.Controller;

public class IndexMgr extends Controller {
	private static DataSource ds = DB.getDataSource();
	private static QueryRunner run = new QueryRunner(ds);
	
	
	/**
	 * 首页资源展示
	 */
	public void index() {
		
	      try { 
	    	    String sql = "select * from t_resources t where t.deleteflag =1";
	    	    
			List resour_list = run.query(sql, new MapListHandler());
			renderTemplate("/site/indexMgr/index.html",resour_list);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
//		renderTemplate("/public/webSite/index.html");  
	}
}
