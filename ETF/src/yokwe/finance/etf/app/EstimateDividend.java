package yokwe.finance.etf.app;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.etf.ETFException;
import yokwe.finance.etf.util.JDBCUtil;
import yokwe.finance.etf.web.DailyData;
import yokwe.finance.etf.web.DividendData;

public class EstimateDividend {
	private static final Logger logger = LoggerFactory.getLogger(EstimateDividend.class);
	
	private static final String SQL_SYMBOL_LIST = "select * from yahoo_dividend where date like '%d%%' group by symbol having count(*) = %d order by symbol, date";
	private static final String SQL_BY_YEAR     = "select * from yahoo_dividend where date like '%d%%' and symbol = '%s' order by date";
	private static final String SQL_DAILY       = "select date, symbol, close as value from yahoo_daily where symbol = '%s' and '%s' < date and date <= '%s'";
	
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
	
	public static class Stats {
		public final int    n;
		public final double avg;
		public final double sum;
		public final double sd;
		public final double cv;
		
		public Stats(double[] data) {
			SummaryStatistics stats = new SummaryStatistics();
			Arrays.stream(data).forEach(o -> stats.addValue(o));
			n   = data.length;
			avg = stats.getMean();
			sum = stats.getSum();
			sd  = stats.getStandardDeviation();
			cv  = sd / avg;
		}
		
		@Override
		public String toString() {
			return String.format("%d  %6.2f  %6.2f  %6.2f  %6.2f", n, avg, sum, sd, cv);
		}
	}
	
	static String dateString(Calendar calendar) {
		return String.format("%4d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
				calendar.get(Calendar.DAY_OF_MONTH));
	}

	static void estimate(final Statement statement, final int freq) {
		final String lastDailyDate;
		final String lastDailyDateOneWeekBefore;
		final Calendar now = Calendar.getInstance();
		final int thisYearNo = now.get(Calendar.YEAR);
		final int lastYearNo = thisYearNo - 1;
		
		{
			String sql = LastDailyDay.genSQL();
			lastDailyDate = JDBCUtil.getResultAll(statement, sql, LastDailyDay.class).get(0).last_day;
			logger.info("lastDailyDate = {}", lastDailyDate);
			
			int year  = Integer.parseInt(lastDailyDate.substring(0, 4));
			int month = Integer.parseInt(lastDailyDate.substring(5, 7));
			int day   = Integer.parseInt(lastDailyDate.substring(8, 10));
			
			Calendar oneWeekBefore = Calendar.getInstance();
			oneWeekBefore.set(Calendar.YEAR,  year);
			oneWeekBefore.set(Calendar.MONTH, month - 1); // 0 base
			oneWeekBefore.set(Calendar.DATE,  day);
			oneWeekBefore.add(Calendar.DATE, -7);
			
			lastDailyDateOneWeekBefore = dateString(oneWeekBefore);
			
			logger.info("lastDailyDateOneWeekBefore = {}", lastDailyDateOneWeekBefore);
		}

		final List<String> symbolList;
		{
			String sql = String.format(SQL_SYMBOL_LIST, lastYearNo, freq);
			symbolList = JDBCUtil.getResultAll(statement, sql, DividendData.class).stream().map(o -> o.symbol).collect(Collectors.toList());
		}
		logger.info("symbolList = {}", symbolList.size());
		
		for(String symbol: symbolList) {
			// data of last year
			String sqlLastYear = String.format(SQL_BY_YEAR, lastYearNo, symbol);
			List<DividendData> lastYear = JDBCUtil.getResultAll(statement, sqlLastYear, DividendData.class).stream().collect(Collectors.toList());
			// data of this year
			String sqlThisYear = String.format(SQL_BY_YEAR, thisYearNo, symbol);
			List<DividendData> thisYear = JDBCUtil.getResultAll(statement, sqlThisYear, DividendData.class).stream().collect(Collectors.toList());
			
			// sanity check
			if (lastYear.size() != freq) {
				logger.error("lastYear = {}  freq = {}", lastYear.size(), freq);
				throw new ETFException("size");
			}
			// There can be a exceptional case when ETF inception date is last year
			if (freq < thisYear.size()) continue;
			
			Stats statsLastYear = new Stats(lastYear.stream().mapToDouble(o -> o.dividend).toArray());

			lastYear.subList(0, thisYear.size()).clear();
			thisYear.addAll(lastYear);
			thisYear.sort((o1, o2) -> o1.date.compareTo(o2.date));
			
			Stats statsThisYear = new Stats(thisYear.stream().mapToDouble(o -> o.dividend).toArray());
			
			String sqlDaily = String.format(SQL_DAILY, symbol, lastDailyDateOneWeekBefore, lastDailyDate);
			double[] dailyData = JDBCUtil.getResultAll(statement, sqlDaily, DailyData.class).stream().mapToDouble(o -> o.value).toArray();
			if (dailyData.length < 5) {
				logger.info("sql = {}", sqlDaily);
				throw new ETFException("dailyData");
			}
			
			Stats statsDaily = new Stats(dailyData);
			
			final double price = statsDaily.avg;
			final double divLastYear = statsLastYear.sum / price * 100;
			final double divThisYear = statsThisYear.sum / price * 100;
			logger.info("{}", String.format("%-6s  %6.2f  %6.2f  -  %6.2f  %6.2f  %6.2f  %6.2f", symbol, divLastYear, divThisYear, price, statsThisYear.avg, statsThisYear.sd, statsThisYear.cv));			
		}
	}

	public static void main(String[] args) {
		logger.info("START");
		try {
			Class.forName("org.sqlite.JDBC");
			try (Statement statement = DriverManager.getConnection("jdbc:sqlite:tmp/sqlite/etf.sqlite3").createStatement()) {
				estimate(statement, 12);
			}
		} catch (ClassNotFoundException | SQLException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			e.printStackTrace();
		}

		logger.info("STOP");
	}
}
