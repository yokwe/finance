package yokwe.finance.etf.app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.etf.util.JDBCUtil;

public class EstimateProfit {
	static final Logger logger = LoggerFactory.getLogger(EstimateProfit.class);

	public static class SymbolCount {
		private static String SQL_STATEMENT = "select symbol, count(*) as count from yahoo_dividend where '%s' < date and date <= '%s' group by symbol having count in (%d, %d, %d, %d)";

		public static String genSQL(int delta) {
			return genSQL(0, delta);
		}

		public static String genSQL(int offset, int delta) {
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.YEAR, -offset);
			String toDate = dateString(calendar);
			calendar.add(Calendar.YEAR, -delta);
			String fromDate = dateString(calendar);
			int c1 = delta * 1;
			int c2 = delta * 2;
			int c4 = delta * 4;
			int c12 = delta * 12;

			return String.format(SQL_STATEMENT, fromDate, toDate, c1, c2, c4, c12);
		}

		public String symbol;
		public int count;

		public String toString() {
			return String.format("%-6s %3d", symbol, count);
		}
	}

	public static class SymbolDividend  implements Comparable<SymbolDividend> {
		private static String SQL_STATEMENT = "select symbol, (sum(dividend) / %d.0) as dividend from yahoo_dividend where '%s' < date and date <= '%s' group by symbol";

		public static String genSQL(int delta) {
			return genSQL(0, delta);
		}

		public static String genSQL(int offset, int delta) {
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.YEAR, -offset);
			String toDate = dateString(calendar);
			calendar.add(Calendar.YEAR, -delta);
			String fromDate = dateString(calendar);

			return String.format(SQL_STATEMENT, delta, fromDate, toDate);
		}

		public String symbol;
		public double dividend;

		public String toString() {
			return String.format("%-6s %2.2f", symbol, dividend);
		}

		@Override
		public int compareTo(SymbolDividend that) {
			return (int) Math.signum(this.dividend - that.dividend);
		}
	}

	public static String dateString(Calendar calendar) {
		return String.format("%4d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
				calendar.get(Calendar.DAY_OF_MONTH));
	}

	public static void main(String[] args) {
		logger.info("START");

		final int delta = 3;
		try {
			Class.forName("org.sqlite.JDBC");
			try (Connection connection = DriverManager.getConnection("jdbc:sqlite:tmp/sqlite/etf.sqlite3")) {

				Map<String, SymbolCount> candidateMap = new TreeMap<>();
				Map<String, SymbolDividend> dividendMap = new TreeMap<>();

				try (Statement statement = connection.createStatement()) {
					String sql = SymbolCount.genSQL(delta);
					JDBCUtil.getResultAll(statement, sql, SymbolCount.class).stream().forEach(o -> candidateMap.put(o.symbol, o));
					logger.info("candidateMap = {}", candidateMap.keySet().size());
				}
				// candidateList.stream().forEach(o -> logger.info("{}", o));

				try (Statement statement = connection.createStatement()) {
					String sql = SymbolDividend.genSQL(delta);
					JDBCUtil.getResultAll(statement, sql, SymbolDividend.class).stream().forEach(o -> dividendMap.put(o.symbol, o));
					logger.info("dividendMap = {}", dividendMap.keySet().size());
				}
				
				{
					List<SymbolDividend> dividendList = new ArrayList<>(dividendMap.values());
					Collections.sort(dividendList);
					//dividendList.stream().forEach(o -> logger.info("{}", o));
				}
				
				// TODO something goes wrong about handling of *NA*
				//   expense_ratio of ACWF shourd be -1.0

			}
		} catch (ClassNotFoundException | SQLException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
		}

		logger.info("STOP");
	}
}
