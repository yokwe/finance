package yokwe.finance.etf.web;

import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.LoggerFactory;

import yokwe.finance.etf.ETFException;
import yokwe.finance.etf.util.JDBCUtil;

public class CSVServlet extends HttpServlet {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(CSVServlet.class);
	
	private static final long serialVersionUID = 1L;
	
	private static final String JDBC_CONNECTION_URL = "jdbc:sqlite:/data1/home/hasegawa/git/finance/ETF/tmp/sqlite/etf.sqlite3";
	
	@Override
	public void init(ServletConfig config) {
		logger.info("init csv");
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new ETFException();
		}
	}
	
	@Override
	public void destroy() {
		logger.info("destroy csv");
	}
	
	public static class DailyClose {
		private static String dateString(Calendar calendar) {
			return String.format("%4d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
					calendar.get(Calendar.DAY_OF_MONTH));
		}

		public static String getSQL(String symbol) {
			return getSQL(symbol, 20 * 12);
		}
		
		private static String SQL = "select date, symbol, close from yahoo_daily where symbol = '%s' and '%s' <= date order by date";
		public static String getSQL(String symbol, int lastNMonth) {
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.MONTH, -lastNMonth);
			String fromDate = dateString(calendar);

			return String.format(SQL, symbol, fromDate);
		}
		
		public DailyClose(String date, String symbol, double close) {
			this.date   = date;
			this.symbol = symbol;
			this.close  = close;
		}
		public DailyClose() {
			this("", "", 0);
		}
		public String date;
		public String symbol;
		public double close;
		
		@Override
		public String toString() {
			return String.format("[%s %s %6.3f", date, symbol, close);
		}
	}
	
	public static class DailyVolume {
		private static String dateString(Calendar calendar) {
			return String.format("%4d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
					calendar.get(Calendar.DAY_OF_MONTH));
		}

		public static String getSQL(String symbol) {
			return getSQL(symbol, 20 * 12);
		}
		
		private static String SQL = "select date, symbol, volume from yahoo_daily where symbol = '%s' and '%s' <= date order by date";
		public static String getSQL(String symbol, int lastNMonth) {
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.MONTH, -lastNMonth);
			String fromDate = dateString(calendar);

			return String.format(SQL, symbol, fromDate);
		}
		
		public String date;
		public String symbol;
		public int    volume;
	}

	public static class Dividend {
		private static String dateString(Calendar calendar) {
			return String.format("%4d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
					calendar.get(Calendar.DAY_OF_MONTH));
		}

		public static String getSQL(String symbol) {
			return getSQL(symbol, 20 * 12);
		}
		
		private static String SQL = "select date, symbol, dividend from yahoo_dividend where symbol = '%s' and '%s' <= date order by date";
		public static String getSQL(String symbol, int lastNMonth) {
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.MONTH, -lastNMonth);
			String fromDate = dateString(calendar);

			return String.format(SQL, symbol, fromDate);
		}
		
		public String date;
		public String symbol;
		public double dividend;
	}
	
	public static class MovingAverage {
		private static final class Accumlator {
			final int                   interval;
			final DescriptiveStatistics stats;
			List<DailyClose>            result;
			
			Accumlator(int interval) {
				this.interval = interval;
				this.stats    = new DescriptiveStatistics(interval);
				this.result   = new ArrayList<>();
			}
			
			void apply(DailyClose x) {
				stats.addValue(x.close);
				if (interval <= stats.getN()) {
					result.add(new DailyClose(x.date, x.symbol, stats.getMean()));
				}
			}
			List<DailyClose> finish() {
				return result;
			}
		}
		
		public static Collector<DailyClose, Accumlator, List<DailyClose>> getInstance(int interval) {
			Supplier<Accumlator>               supplier    = () -> new Accumlator(interval);
			BiConsumer<Accumlator, DailyClose> accumulator = (a, e) -> a.apply(e);
			BinaryOperator<Accumlator> combiner = (a1, a2) -> {
				logger.error("combiner  {}  {}", a1.toString(), a2.toString());
				throw new ETFException("Not expected");
			};
			Function<Accumlator, List<DailyClose>> finisher    = (a) -> a.finish();
			return Collector.of(supplier, accumulator, combiner, finisher);
		}
	}

	
	private static String CRLF = "\r\n";
	
	private void doDailyClose(Statement statement, BufferedWriter output, List<String> symbolList, int lastNMonth) throws IOException {
		final int movingAverageDays = 200;
		
		Map<String, List<DailyClose>> rawData = new TreeMap<>();
		{
			List<String> newSymbolList = new ArrayList<>();
			for(String symbol: symbolList) {
				List<DailyClose> data = JDBCUtil.getResultAll(statement, DailyClose.getSQL(symbol, lastNMonth), DailyClose.class);
				List<DailyClose> data_ma = data.stream().collect(MovingAverage.getInstance(200));
				
				logger.info("{}  data = {}  data_ma = {}", symbol, data.size(), data_ma.size());
				
				String symbol_ma = symbol + "-" + movingAverageDays;
				
				rawData.put(symbol,    data);
				rawData.put(symbol_ma, data_ma);
				
				newSymbolList.add(symbol);
				newSymbolList.add(symbol_ma);
				
				logger.info("data    = {}");
				logger.info("data_ma = {}", data_ma);
			}
			
			symbolList = newSymbolList;
		}
		
		Map<String, Map<String, Double>> dateMap = new TreeMap<>();
		{
			for(String symbol: symbolList) {
				for(DailyClose data: rawData.get(symbol)) {
					if (!dateMap.containsKey(data.date)) {
						dateMap.put(data.date, new TreeMap<>());
					}
					dateMap.get(data.date).put(symbol, data.close);
				}
			}
		}
		
		logger.info("dateMap  = {}", dateMap.size());

		StringBuilder line = new StringBuilder();
		
		// Output header
		line.setLength(0);
		line.append("date");
		for(String fieldName: symbolList) {
			if (0 < line.length()) line.append(",");
			line.append(fieldName);
		}
		output.append(line.toString()).append(CRLF);
		logger.info("fieldNameList = {}", line.toString());

		
		for(String date: dateMap.keySet()) {
			Map<String, Double> record = dateMap.get(date);
			line.setLength(0);
			line.append(date);
			for(String symbol: symbolList) {
				line.append(",");
				if (record.containsKey(symbol)) {
					line.append(record.get(symbol));
				}
			}
			output.append(line.toString()).append(CRLF);
		}
	}

	private void doDailyVolume(Statement statement, BufferedWriter output, List<String> symbolList, int lastNMonth) throws IOException {
		Map<String, Map<String, Double>> dateMap = new TreeMap<>();
		for(String symbol: symbolList) {
			for(DailyVolume data: JDBCUtil.getResultAll(statement, DailyVolume.getSQL(symbol, lastNMonth), DailyVolume.class)) {
				if (!dateMap.containsKey(data.date)) {
					dateMap.put(data.date, new TreeMap<>());
				}
				dateMap.get(data.date).put(data.symbol, (double)data.volume);
			}
		}
		
		logger.info("dateMap  = {}", dateMap.size());

		StringBuilder line = new StringBuilder();
		
		// Output header
		line.setLength(0);
		line.append("date");
		for(String fieldName: symbolList) {
			if (0 < line.length()) line.append(",");
			line.append(fieldName);
		}
		output.append(line.toString()).append(CRLF);
		logger.info("fieldNameList = {}", line.toString());

		
		for(String date: dateMap.keySet()) {
			Map<String, Double> record = dateMap.get(date);
			line.setLength(0);
			line.append(date);
			for(String symbol: symbolList) {
				line.append(",");
				if (record.containsKey(symbol)) {
					line.append(record.get(symbol));
				}
			}
			output.append(line.toString()).append(CRLF);
		}
	}

	private void doDividend(Statement statement, BufferedWriter output, List<String> symbolList, int lastNMonth) throws IOException {
		Map<String, Map<String, Double>> dateMap = new TreeMap<>();
		for(String symbol: symbolList) {
			for(Dividend data: JDBCUtil.getResultAll(statement, Dividend.getSQL(symbol, lastNMonth), Dividend.class)) {
				// Use pseudo date for dataMap
				String pseudoDate = data.date.substring(0, 8) + "15";
				if (!dateMap.containsKey(pseudoDate)) {
					dateMap.put(pseudoDate, new TreeMap<>());
				}
				double oldValue = 0;
				if (dateMap.get(pseudoDate).containsKey(data.symbol)) {
					oldValue = dateMap.get(pseudoDate).get(data.symbol);
				}
				dateMap.get(pseudoDate).put(data.symbol, data.dividend + oldValue);
			}
		}		
		
		logger.info("dateMap  = {}", dateMap.size());

		StringBuilder line = new StringBuilder();
		
		// Output header
		line.setLength(0);
		line.append("date");
		for(String fieldName: symbolList) {
			if (0 < line.length()) line.append(",");
			line.append(fieldName);
		}
		output.append(line.toString()).append(CRLF);
		logger.info("fieldNameList = {}", line.toString());

		
		for(String date: dateMap.keySet()) {
			Map<String, Double> record = dateMap.get(date);
			line.setLength(0);
			line.append(date);
			for(String symbol: symbolList) {
				line.append(",");
				if (record.containsKey(symbol)) {
					line.append(record.get(symbol));
				}
			}
			output.append(line.toString()).append(CRLF);
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		logger.info("doGet START");
		
		final List<String> symbolList = new ArrayList<>();
		int lastNMonth;
		String type = "daily";

		{
			Map<String, String[]> paramMap = req.getParameterMap();
			
			if (paramMap.containsKey("s")) {
				for(String symbol: paramMap.get("s")) {
					symbolList.add(symbol);
				}
			}
			if (symbolList.size() == 0) symbolList.add("SPY");
			logger.info("symbolList = {}", symbolList);
			
			if (paramMap.containsKey("m")) {
				String s = paramMap.get("m")[0];
				try {
					lastNMonth = Integer.parseInt(s);
				} catch (NumberFormatException e) {
					logger.error(e.getClass().getName());
					logger.error(e.getMessage());
					lastNMonth = 12;
				}
			} else {
				lastNMonth = 12;
			}
			logger.info("m = {}", lastNMonth);
			
			if (paramMap.containsKey("t")) {
				type = paramMap.get("t")[0];
			} else {
				type = "daily";
			}
			switch(type) {
			case "close":
			case "volume":
			case "dividend":
				break;
			default:
				logger.info("Unknown type = {}", type);
				type = "daily";
			}
			logger.info("t = {}", type);
		}

		resp.setContentType("text/csv; charset=UTF-8");
		
		try (
				Statement statement = DriverManager.getConnection(JDBC_CONNECTION_URL).createStatement();
				BufferedWriter output = new BufferedWriter(resp.getWriter(), 65536);) {
			
			switch(type) {
			case "close":
				doDailyClose(statement, output, symbolList, lastNMonth);
				break;
			case "volume":
				doDailyVolume(statement, output, symbolList, lastNMonth);
				break;
			case "dividend":
				doDividend(statement, output, symbolList, lastNMonth);
				break;
			default:
				logger.error("Unknown type = {}", type);
				break;
			}
			
		} catch (SQLException | IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new ETFException();
		} catch (RuntimeException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			int n = 0;
			for(StackTraceElement stackTrace: e.getStackTrace()) {
				n++;
				if (n == 5) break;
				logger.info("stack {}", stackTrace);
			}
			throw new ETFException();
		}
		logger.info("doGet STOP");
	}
}
