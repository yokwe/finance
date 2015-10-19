package yokwe.finance.securities.app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.util.JDBCUtil;
import yokwe.finance.securities.util.DoubleStreamUtil;

public class EstimateProfit {
	static final Logger logger = LoggerFactory.getLogger(EstimateProfit.class);

	public static class SymbolCount {
		private static String SQL_STATEMENT = "select symbol, count(*) as count from dividend where '%s' < date and date <= '%s' group by symbol having count in (%d, %d, %d, %d)";

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
		private static String SQL_STATEMENT = "select symbol, date, dividend from dividend where '%s' < date and date <= '%s'";

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
		private static String SQL_STATEMENT = "select symbol, name, mkt_cap from finance";
		
		public static String genSQL() {
			return SQL_STATEMENT;
		}

		public String symbol;
		public String name;
		public int    mkt_cap;

		@Override
		public String toString() {
			return String.format("%-6s %8d", symbol, mkt_cap);
		}

		@Override
		public int compareTo(SymbolInfo that) {
			return this.symbol.compareTo(that.symbol);
		}
	}

	public static class LastDailyDay {
		private static String SQL_STATEMENT = "select max(date) as last_day from price";
		
		public static String genSQL() {
			return SQL_STATEMENT;
		}

		public String last_day;

		public String toString() {
			return last_day;
		}
	}

	public static class SymbolClose {
		private static String SQL_STATEMENT = "select symbol, close from price where date = '%s'";
		
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
		public String name;
		public int    dividendFrequency;
		public double close;
		
		public double unitsPer1000;
		public double dividendPerYear;
		public double profitPerYearPer1000;
		
		public SymbolProfit(String symbol, String name, int dividendFrequency, double close, double unitsPer1000, double dividendPerYear, double profitPerYearPer1000) {
			this.symbol               = symbol;
			this.name                 = name;
			this.dividendFrequency    = dividendFrequency;
			this.close                = close;
			this.unitsPer1000         = unitsPer1000;
			this.dividendPerYear      = dividendPerYear;
			this.profitPerYearPer1000 = profitPerYearPer1000;
		}
		
		@Override
		public String toString() {
			return String.format("%-6s %2d %7.2f %7.2f %8.2f %9.3f  %s", symbol, dividendFrequency, close, unitsPer1000, dividendPerYear, profitPerYearPer1000, name);
		}
		
		@Override
		public int compareTo(SymbolProfit that) {
			return (int)Math.signum(this.profitPerYearPer1000 - that.profitPerYearPer1000);
		}
	}

	public static String dateString(Calendar calendar) {
		return String.format("%4d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
				calendar.get(Calendar.DAY_OF_MONTH));
	}
	
	public static void calculate(final Connection connection, final int years) {
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
			String sql = SymbolCount.genSQL(years);
			JDBCUtil.getResultAll(connection, sql, SymbolCount.class).stream().forEach(o -> candidateMap.put(o.symbol, o));
			logger.info("candidateMap = {}", candidateMap.keySet().size());
		}
		// candidateList.stream().forEach(o -> logger.info("{}", o));

		{
			String sql = SymbolDividend.genSQL(years);
			dividendlist = JDBCUtil.getResultAll(connection, sql, SymbolDividend.class);
			logger.info("dividendlist = {}", dividendlist.size());
		}
		
		List<SymbolProfit> profitList = new ArrayList<>();
//		for(String symbol: candidateMap.keySet()) {
		for(String symbol: infoMap.keySet()) {
			List<SymbolDividend> rawList = dividendlist.stream().filter(o -> o.symbol.equals(symbol)).collect(Collectors.toList());
			final SymbolInfo info = infoMap.get(symbol);
			// Too few number of dividend
			if ((rawList.size() / (double)years) < 3) continue;
			
			final String name = info.name;
			
			// No close data
			if (!closeMap.containsKey(symbol)) {
				//logger.info("closeMap null  {}", symbol);
				continue;
			}
			final double close = closeMap.get(symbol).close;
			
			final int    rawCount = (int)rawList.stream().count();
			final DoubleStreamUtil.Stats rawStats = new DoubleStreamUtil.Stats();
			rawList.stream().mapToDouble(o -> o.dividend).forEach(rawStats);
			final double rawAVG = rawStats.getMean();
			final double rawSD  = rawStats.getStandardDeviation();
			
			final double lowLimit  = rawAVG - rawSD - rawSD;
			final double highLimit = rawAVG + rawSD + rawSD;
			
			// Handle special case properly: must be <= for the case rawSD == 0
			List<SymbolDividend> adjList = rawList.stream().filter(o -> lowLimit <= o.dividend && o.dividend <= highLimit).collect(Collectors.toList());
			if (adjList.size() == 0) continue;
			
			final DoubleStreamUtil.Stats adjStats = new DoubleStreamUtil.Stats();
			adjList.stream().mapToDouble(o -> o.dividend).forEach(adjStats);
			final double adjAVG   = adjStats.getMean();

			final double profitPerYear = adjAVG * rawCount / years;
			
			// If we buy $1000.00, how many units?
			final double unitsPer1000 = 1000.0 / close;
			
			final int    dividendFrequency = rawCount / years;
			
			final double profitPerYearPer1000 = (profitPerYear * unitsPer1000);
			
			profitList.add(new SymbolProfit(symbol, name, dividendFrequency, close, unitsPer1000, profitPerYear, profitPerYearPer1000));
		}
		Collections.sort(profitList);
		profitList.stream().forEach(o -> logger.info("{}", o));
	}

	public static void main(String[] args) {
		logger.info("START");
		
		try {
			Class.forName("org.sqlite.JDBC");
			try (Connection connection = DriverManager.getConnection("jdbc:sqlite:tmp/sqlite/securities.sqlite3")) {
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
