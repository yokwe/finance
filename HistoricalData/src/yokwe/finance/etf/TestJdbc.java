package yokwe.finance.etf;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestJdbc {
	static final Logger logger = LoggerFactory.getLogger(TestJdbc.class);
	
	public static class YahooProfileData {
		public String symbol;
		public String name;
		public int net_assets;
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
				String sql = "select * from etf_yahoo_profile";
				List<YahooProfileData> resutlList = JdbcHelper.getResultAll(statement, sql, YahooProfileData.class);

				int count = 0;
				for(YahooProfileData record: resutlList) {
					count++;
					logger.debug("{} {}", count, record);
				}
			}
		} catch (ClassNotFoundException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
		} catch (SQLException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
		}
		
		logger.info("STOP");
	}

}
