package yokwe.finance.securities.app;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.JDBCUtil;

public class EstimateDividend {
	private static final Logger logger = LoggerFactory.getLogger(EstimateDividend.class);
	
	private static final String OUTPUT_PATH = "tmp/estimateDividend.csv";
	private static final String CRLF = "\r\n";
		
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
	
	public static class Stats {
		public final int    n;
		public final double avg;
		public final double sum;
		public final double sd;
		
		public Stats(double[] data) {
			SummaryStatistics stats = new SummaryStatistics();
			Arrays.stream(data).forEach(o -> stats.addValue(o));
			n   = data.length;
			avg = stats.getMean();
			sum = stats.getSum();
			sd  = stats.getStandardDeviation();
		}
		
		@Override
		public String toString() {
			return String.format("%d  %6.2f  %6.2f  %6.2f", n, avg, sum, sd);
		}
	}
	
	static String dateString(Calendar calendar) {
		return String.format("%4d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
				calendar.get(Calendar.DAY_OF_MONTH));
	}
	
	static Calendar getLastTradedDay(final Statement statement) {
		String sql = LastDailyDay.genSQL();
		String lastTradedDate = JDBCUtil.getResultAll(statement, sql, LastDailyDay.class).get(0).last_day;
		
		int year  = Integer.parseInt(lastTradedDate.substring(0, 4));
		int month = Integer.parseInt(lastTradedDate.substring(5, 7));
		int day   = Integer.parseInt(lastTradedDate.substring(8, 10));
		
		Calendar ret = Calendar.getInstance();
		ret.set(Calendar.YEAR,  year);
		ret.set(Calendar.MONTH, month - 1); // 0 base
		ret.set(Calendar.DATE,  day);
		
		return ret;
	}
	
	static Calendar add(final Calendar origin, final int field, final int amount) {
		Calendar ret = Calendar.getInstance();
		ret.setTimeInMillis(origin.getTimeInMillis());
		ret.add(field, amount);
		return ret;
	}
	
	static List<String> getSymbol(final Statement statement, final int year, final int freq) {
		final String sql_template = "select * from dividend where date like '%d%%' group by symbol having count(*) = %d order by symbol, date";
		final String sql = String.format(sql_template, year, freq);
		List<String> ret = JDBCUtil.getResultAll(statement, sql, DividendData.class).stream().map(o -> o.symbol).collect(Collectors.toList());
		return ret;
	}
	
	static List<DividendData> getDividendData(final Statement statement, final String symbol, final int year) {
		final String sql_template = "select * from dividend where date like '%d%%' and symbol = '%s' order by date";
		final String sql = String.format(sql_template, year, symbol);
		List<DividendData> ret = JDBCUtil.getResultAll(statement, sql, DividendData.class).stream().collect(Collectors.toList());
		return ret;
	}
	
	static double[] getDailyData(final Statement statement, final String symbol, final Calendar dateBegin, final Calendar dateEnd) {
		String sql_template = "select date, symbol, close as value from price where symbol = '%s' and '%s' < date and date <= '%s'";
		String sql = String.format(sql_template, symbol, dateString(dateBegin), dateString(dateEnd));
		double[] ret = JDBCUtil.getResultAll(statement, sql, PriceData.class).stream().mapToDouble(o -> o.value).toArray();
		return ret;
	}
	
	static Map<String, FinanceData> getFinanceMap(final Statement statement) {
		String sql = "select * from finance";
		Map<String, FinanceData> ret = new TreeMap<>();
		JDBCUtil.getResultAll(statement, sql, FinanceData.class).stream().forEach(o -> ret.put(o.symbol, o));
		return ret;
	}
	
	static Map<String, NasdaqData> getNasdaqMap(final Statement statement) {
		String sql = "select * from nasdaq";
		Map<String, NasdaqData> ret = new TreeMap<>();
		JDBCUtil.getResultAll(statement, sql, NasdaqData.class).stream().forEach(o -> ret.put(o.symbol, o));
		return ret;
	}
	
	static void estimate(final Writer writer, final Statement statement, final int freq) throws IOException {
		final Calendar origin = getLastTradedDay(statement);
		
		final int thisYearNo   = origin.get(Calendar.YEAR);
		final int lastYearNo   = thisYearNo - 1;
		final Calendar oneWeek = add(origin, Calendar.DATE, -7);
		final Calendar oneYear = add(origin, Calendar.YEAR, -1);
		
//		Map<String, FinanceData> financeMap = getFinanceMap(statement);
		Map<String, NasdaqData>  nasdaqMap  = getNasdaqMap(statement);
		
		final List<String> symbolList = getSymbol(statement, lastYearNo, freq);
		logger.info("symbolList = {}", symbolList.size());
		
		StringBuffer line = new StringBuffer();

		for(final String symbol: symbolList) {
//			FinanceData financeData = financeMap.get(symbol);
//			if (financeData == null) {
//			    throw new SecuritiesException("financeData");
//		    }
			NasdaqData nasdaqData = nasdaqMap.get(symbol);
			if (nasdaqData == null) {
				logger.error("NO DATA  symbol = {}", symbol);
				throw new RuntimeException();
//				continue;
			}
			
			// data of last year
			List<DividendData> lastYear = getDividendData(statement, symbol, lastYearNo);
			// data of this year
			List<DividendData> thisYear = getDividendData(statement, symbol, thisYearNo);
			
			// sanity check
			if (lastYear.size() != freq) {
				logger.error("lastYear = {}  freq = {}", lastYear.size(), freq);
				throw new SecuritiesException("size");
			}
			// There can be a exceptional case when ETF inception date is last year
			if (freq < thisYear.size()) continue;
			
			Stats statsLastYear = new Stats(lastYear.stream().mapToDouble(o -> o.dividend).toArray());

			// build annual data of this year from data of last year
			lastYear.subList(0, thisYear.size()).clear();
			thisYear.addAll(lastYear);
			thisYear.sort((o1, o2) -> o1.date.compareTo(o2.date));
			
			Stats statsThisYear = new Stats(thisYear.stream().mapToDouble(o -> o.dividend).toArray());
			
			double[] weekData  = getDailyData(statement, symbol, oneWeek, origin);
			Stats    statsWeek = new Stats(weekData);
			if (weekData.length < 5) {
				// Must be this symbol start this week
				logger.warn("symbol = {} {}", symbol, weekData.length);
				continue;
			}
			
			double[] yearData  = getDailyData(statement, symbol, oneYear, origin);
			Stats    statsYear = new Stats(yearData);
			
			final double price = statsWeek.avg;
			final double divLastYear = statsLastYear.sum / price * 100;
			final double divThisYear = statsThisYear.sum / price * 100;
			logger.info("{}", String.format("%-6s  %6.2f  %6.3f  %6.3f  -  %6.3f  %6.3f  %6.3f  -  %6.2f  %6.3f  %6.3f",
					symbol, price, divLastYear, divThisYear,
					statsThisYear.avg, statsThisYear.sd, (statsThisYear.sd / statsThisYear.avg),
					statsYear.avg,     statsYear.sd,     (statsYear.sd     / statsYear.avg)));
			
			// 

			line.setLength(0);
			line.append(symbol);
			line.append(String.format(",%d",   freq));
			line.append(String.format(",%.2f", price));
			line.append(String.format(",%.3f", divLastYear));
			line.append(String.format(",%.3f", divThisYear));
			
//			line.append(String.format(",%d",   financeData.avg_vol));
//			line.append(String.format(",%d",   financeData.mkt_cap));

			line.append(String.format(",%.3f", statsThisYear.avg));
			line.append(String.format(",%.3f", statsThisYear.sd));
			line.append(String.format(",%.3f", statsThisYear.sd / statsThisYear.avg));
			
			line.append(String.format(",%.3f", statsYear.avg));
			line.append(String.format(",%.3f", statsYear.sd));
			line.append(String.format(",%.3f", statsYear.sd / statsYear.avg));
			
			line.append(String.format(",\"%s\"", nasdaqData.name));
			line.append(CRLF);
			
			writer.write(line.toString());
		}
	}

	public static void main(String[] args) {
		logger.info("START");
		try {
			Class.forName("org.sqlite.JDBC");
			try (Statement statement = DriverManager.getConnection("jdbc:sqlite:tmp/sqlite/securities.sqlite3").createStatement();
				Writer writer = new BufferedWriter(new FileWriter(OUTPUT_PATH))) {
				StringBuffer line = new StringBuffer();
				line.append("symbol,freq,price,score,d last,d this,avg_vol,mkt_cap,d avg,d sd,d cv,p avg,p sd,p cv,name,segment").append(CRLF);
				writer.write(line.toString());

				estimate(writer, statement, 12);
				estimate(writer, statement,  6);
				estimate(writer, statement,  4);
			}
		} catch (ClassNotFoundException | SQLException | IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			e.printStackTrace();
		}

		logger.info("STOP");
	}
}
