package yokwe.finance.etf.app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.etf.Util;
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
		public int    count;

		public String toString() {
			return String.format("%-6s %3d", symbol, count);
		}
	}

	public static class SymbolDividend  implements Comparable<SymbolDividend> {
		private static String SQL_STATEMENT = "select symbol, date, dividend from yahoo_dividend where '%s' < date and date <= '%s'";

		public static String genSQL(int delta) {
			return genSQL(0, delta);
		}

		public static String genSQL(int offset, int delta) {
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.YEAR, -offset);
			String toDate = dateString(calendar);
			calendar.add(Calendar.YEAR, -delta);
			String fromDate = dateString(calendar);

			return String.format(SQL_STATEMENT, fromDate, toDate);
		}

		public String symbol;
		public String date;
		public double dividend;

		public String toString() {
			return String.format("%-6s %2.2f", symbol, dividend);
		}

		@Override
		public int compareTo(SymbolDividend that) {
			return (int) Math.signum(this.dividend - that.dividend);
		}
	}

	public static class SymbolInfo implements Comparable<SymbolInfo> {
		private static String SQL_STATEMENT = "select symbol, name, inception_date, expense_ratio, aum, index_tracked from etf";
		
		public static String genSQL() {
			return SQL_STATEMENT;
		}

		public String symbol;
		public String name;
		public String inception_date;
		public double expense_ratio;
		public int    aum;
		public String index_tracked;

		@Override
		public String toString() {
			return String.format("%-6s %2.2f", symbol, expense_ratio);
		}

		@Override
		public int compareTo(SymbolInfo that) {
			return this.symbol.compareTo(that.symbol);
		}
	}

	public static class LastDailyDay {
		private static String SQL_STATEMENT = "select max(date) as last_day from yahoo_daily";
		
		public static String genSQL() {
			return SQL_STATEMENT;
		}

		public String last_day;

		public String toString() {
			return last_day;
		}
	}

	public static class SymbolClose {
		private static String SQL_STATEMENT = "select symbol, close from yahoo_daily where date = '%s'";
		
		public static String genSQL(String date) {
			return String.format(SQL_STATEMENT, date);
		}

		public String symbol;
		public double close;

		@Override
		public String toString() {
			return String.format("%-6s %3.2f", symbol, close);
		}
	}
	
	public static class SymbolProfit implements Comparable<SymbolProfit> {
		public String symbol;
		public double profit;
		public int    count;
		public double close;
		
		public SymbolProfit(String symbol, double profit, int count, double close) {
			this.symbol = symbol;
			this.profit = profit;
			this.count  = count;
			this.close  = close;
		}
		
		@Override
		public String toString() {
			return String.format("%-6s %3.2f %2d %6.2f", symbol, profit, count, close);
		}
		
		@Override
		public int compareTo(SymbolProfit that) {
			return (int)Math.signum(this.profit - that.profit);
		}
	}

	public static String dateString(Calendar calendar) {
		return String.format("%4d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
				calendar.get(Calendar.DAY_OF_MONTH));
	}
	
	public static void calculate(final Connection connection, final int delta) {
		String                      lastDailyDay;
		Map<String, SymbolInfo>     infoMap      = new TreeMap<>();
		Map<String, SymbolCount>    candidateMap = new TreeMap<>();
		List<SymbolDividend>        dividendlist = new ArrayList<>();
		Map<String, SymbolClose>    closeMap     = new TreeMap<>();

		{
			String sql = LastDailyDay.genSQL();
			lastDailyDay = JDBCUtil.getResultAll(connection, sql, LastDailyDay.class).get(0).last_day;
			logger.info("lastDailyDay = {}", lastDailyDay);
		}

		{
			String sql = SymbolClose.genSQL(lastDailyDay);
			JDBCUtil.getResultAll(connection, sql, SymbolClose.class).stream().forEach(o -> closeMap.put(o.symbol, o));
			logger.info("closeMap = {}", closeMap.keySet().size());
		}
		
		{
			String sql = SymbolInfo.genSQL();
			JDBCUtil.getResultAll(connection, sql, SymbolInfo.class).stream().forEach(o -> infoMap.put(o.symbol, o));
			logger.info("infoMap = {}", infoMap.keySet().size());
		}

		{
			String sql = SymbolCount.genSQL(delta);
			JDBCUtil.getResultAll(connection, sql, SymbolCount.class).stream().forEach(o -> candidateMap.put(o.symbol, o));
			logger.info("candidateMap = {}", candidateMap.keySet().size());
		}
		// candidateList.stream().forEach(o -> logger.info("{}", o));

		{
			String sql = SymbolDividend.genSQL(delta);
			dividendlist = JDBCUtil.getResultAll(connection, sql, SymbolDividend.class);
			logger.info("dividendlist = {}", dividendlist.size());
		}
		
		
		for(String symbol: candidateMap.keySet()) {
			List<SymbolDividend> baseList = dividendlist.stream().filter(o -> o.symbol.equals(symbol)).collect(Collectors.toList());
			final int baseCount = (int)baseList.stream().count();
			final double baseAVG = baseList.stream().mapToDouble(o -> o.dividend).average().getAsDouble();
			final double baseSD  = Util.StandardDeviationSample(baseList.stream().mapToDouble(o -> o.dividend).boxed().collect(Collectors.toList()));
			
			final double lowLimit  = baseAVG - baseSD - baseSD;
			final double highLimit = baseAVG + baseSD + baseSD;
			
			// Handle special case properly: must be <= for the case baseSD == 0
			List<SymbolDividend> adjustedList = baseList.stream().filter(o -> lowLimit <= o.dividend && o.dividend <= highLimit).collect(Collectors.toList());
			final int adjustedCount = (int)adjustedList.stream().count();
			final double adjustedAVG = adjustedList.stream().mapToDouble(o -> o.dividend).average().getAsDouble();
			final double adjustedSD  = Util.StandardDeviationSample(adjustedList.stream().mapToDouble(o -> o.dividend).boxed().collect(Collectors.toList()));

			if (baseCount != adjustedCount)
				logger.info("{}", String.format("%-6s  %2d-%2d  %5.3f-%5.3f  %5.3f-%5.3f", symbol, baseCount, adjustedCount, baseAVG, adjustedAVG, baseSD, adjustedSD));			
		}
		
		// TODO need to remove irregular value
//				List<SymbolProfit> symbolProfitList = new ArrayList<>();
//				for(String symbol: candidateMap.keySet()) {
//					double close    = closeMap.get(symbol).close;
//					double dividend = dividendMap.get(symbol).dividend;
//					int    count    = dividendMap.get(symbol).count;
//					
//					symbolProfitList.add(new SymbolProfit(symbol, dividend, count, close));
//				}
//				Collections.sort(symbolProfitList);
//				symbolProfitList.stream().forEach(o -> logger.info("{}", o));
	}

	public static void main(String[] args) {
		logger.info("START");
		
		try {
			Class.forName("org.sqlite.JDBC");
			try (Connection connection = DriverManager.getConnection("jdbc:sqlite:tmp/sqlite/etf.sqlite3")) {
				calculate(connection, 3);
			}
		} catch (ClassNotFoundException | SQLException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			e.printStackTrace();
		}

		logger.info("STOP");
	}
}
