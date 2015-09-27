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
import java.util.stream.Collectors;

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
	
	private static String dateString(Calendar calendar) {
		return String.format("%4d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
				calendar.get(Calendar.DAY_OF_MONTH));
	}

	
	public static class DailyClose {
		private static String SQL = "select date, symbol, close from yahoo_daily where symbol = '%s' and '%s' <= date order by date";
		public static String getSQL(String symbol, Calendar fromDate) {
			return String.format(SQL, symbol, dateString(fromDate));
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
	
	public static class SampledData {
		public final String date;
		public final String symbol;
		public final double value;

		public SampledData(String date, String symbol, double value) {
			this.date   = date;
			this.symbol = symbol;
			this.value  = value;
		}
		public SampledData(DailyClose that) {
			this(that.date, that.symbol, that.close);
		}
		
		@Override
		public String toString() {
			return String.format("[%s %s %6.3f", date, symbol, value);
		}
	}
	
	public static class DailyVolume {
		private static String SQL = "select date, symbol, volume from yahoo_daily where symbol = '%s' and '%s' <= date order by date";
		public static String getSQL(String symbol, Calendar fromDate) {
			return String.format(SQL, symbol, dateString(fromDate));
		}
		
		public String date;
		public String symbol;
		public int    volume;
	}

	public static class Dividend {
		private static String SQL = "select date, symbol, dividend from yahoo_dividend where symbol = '%s' and '%s' <= date order by date";
		public static String getSQL(String symbol, Calendar fromDate) {
			return String.format(SQL, symbol, dateString(fromDate));
		}
		
		public String date;
		public String symbol;
		public double dividend;
	}
	
	public static class MovingAverage {
		private static final class Accumlator {
			final int                   interval;
			final DescriptiveStatistics stats;
			List<SampledData>           result;
			
			Accumlator(int interval) {
				this.interval = interval;
				this.stats    = new DescriptiveStatistics(interval);
				this.result   = new ArrayList<>();
			}
			
			void apply(SampledData x) {
				stats.addValue(x.value);
				if (interval <= stats.getN()) {
					result.add(new SampledData(x.date, x.symbol, stats.getMean()));
				}
			}
			List<SampledData> finish() {
				return result;
			}
		}
		
		public static Collector<SampledData, Accumlator, List<SampledData>> getInstance(int interval) {
			final Supplier<Accumlator>               supplier      = () -> new Accumlator(interval);
			final BiConsumer<Accumlator, SampledData> accumulator  = (a, e) -> a.apply(e);
			final BinaryOperator<Accumlator> combiner = (a1, a2) -> {
				logger.error("combiner  {}  {}", a1.toString(), a2.toString());
				throw new ETFException("Not expected");
			};
			final Function<Accumlator, List<SampledData>> finisher = (a) -> a.finish();
			return Collector.of(supplier, accumulator, combiner, finisher);
		}
	}

	
	private static String CRLF = "\r\n";
	
	private interface ServletProcessor {
		public void process(Statement statement, BufferedWriter output, List<String> symbolList, Calendar fromDate) throws IOException;
	}
	private static Map<String, ServletProcessor> processorMap = new TreeMap<>();
	static {
		processorMap.put("daily",    new DailyProcessRequest());
		processorMap.put("volume",   new VolumeProcessor());
		processorMap.put("dividend", new DividendProcessor());
	}
	
	private static class DailyProcessRequest implements ServletProcessor {
		public void process(Statement statement, BufferedWriter output, List<String> symbolList, Calendar fromDate) throws IOException {
			Map<String, List<SampledData>> rawData = new TreeMap<>();
			for(String symbol: symbolList) {
				List<SampledData> data = JDBCUtil.getResultAll(statement, DailyClose.getSQL(symbol, fromDate), DailyClose.class).stream().map(o -> new SampledData(o)).collect(Collectors.toList());
				rawData.put(symbol, data);
			}
			
			Map<String, Map<String, Double>> dateMap = new TreeMap<>();
			for(String symbol: symbolList) {
				for(SampledData data: rawData.get(symbol)) {
					if (!dateMap.containsKey(data.date)) {
						dateMap.put(data.date, new TreeMap<>());
					}
					dateMap.get(data.date).put(symbol, data.value);
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
	}

	private static class VolumeProcessor implements ServletProcessor {
		public void process(Statement statement, BufferedWriter output, List<String> symbolList, Calendar fromDate) throws IOException {
			Map<String, Map<String, Double>> dateMap = new TreeMap<>();
			for(String symbol: symbolList) {
				for(DailyVolume data: JDBCUtil.getResultAll(statement, DailyVolume.getSQL(symbol, fromDate), DailyVolume.class)) {
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
	}

	private static class DividendProcessor implements ServletProcessor {
		public void process(Statement statement, BufferedWriter output, List<String> symbolList, Calendar fromDate) throws IOException {
			Map<String, Map<String, Double>> dateMap = new TreeMap<>();
			for(String symbol: symbolList) {
				for(Dividend data: JDBCUtil.getResultAll(statement, Dividend.getSQL(symbol, fromDate), Dividend.class)) {
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

	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		logger.info("doGet START");
		
		final List<String> symbolList = new ArrayList<>();
		int lastNMonth;
		String type = "daily";

		final ServletProcessor processRequest;
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
			logger.info("t = {}", type);
			
			processRequest = processorMap.get(type);
			if (processRequest == null) {
				logger.error("Unknown type = {}", type);
				throw new ETFException();
			}
		}

		resp.setContentType("text/csv; charset=UTF-8");
		
		try (
				Statement statement = DriverManager.getConnection(JDBC_CONNECTION_URL).createStatement();
				BufferedWriter output = new BufferedWriter(resp.getWriter(), 65536);) {
			Calendar fromDate = Calendar.getInstance();
			fromDate.add(Calendar.MONTH, -lastNMonth);
			
			processRequest.process(statement, output, symbolList, fromDate);
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
