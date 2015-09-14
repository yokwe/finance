package yokwe.finance.etf.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.etf.util.JDBCUtil;

public class TestJDBC {
	static final Logger logger = LoggerFactory.getLogger(TestJDBC.class);
	
	public static class Data {
		public String symbol;
		public String name;
		public int    net_assets;
		public double expense_ratio;
		
		public String toString() {
			return String.format("[%s|%s|%d|%.2f]", symbol, name, net_assets, expense_ratio);
		}
	}

	public static void main(String[] args) {
		logger.info("START");
		try {
			Class.forName("org.sqlite.JDBC");
			try (Connection connection = DriverManager.getConnection("jdbc:sqlite:tmp/sqlite/etf.sqlite3")) {
				Statement statement = connection.createStatement();
				String sql = "select * from yahoo_profile";
				List<Data> resultList = JDBCUtil.getResultAll(statement, sql, Data.class);

				int count = 0;
				for(Data record: resultList) {
					count++;
					logger.debug("{} {}", count, record);
				}
			}
		} catch (ClassNotFoundException | SQLException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
		}
		
		logger.info("STOP");
	}
}
