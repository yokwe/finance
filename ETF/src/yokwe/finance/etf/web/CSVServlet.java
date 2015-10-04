package yokwe.finance.etf.web;

import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;

import yokwe.finance.etf.ETFException;
import yokwe.finance.etf.util.JDBCUtil;
import yokwe.finance.etf.util.StreamUtil.MovingStats;

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
		private static String SQL_1 = "select date, symbol, close from yahoo_daily where symbol = '%s' and '%s' <= date order by date";
		public static String getSQL(String symbol, Calendar fromDate) {
			return String.format(SQL_1, symbol, dateString(fromDate));
		}
		
		private static String SQL_2 = "select date, symbol, close from yahoo_daily where symbol = '%s' and date < '%s' order by date desc limit %s";
		public static String getSQL(String symbol, Calendar fromDate, int count) {
			return String.format(SQL_2, symbol, dateString(fromDate), count);
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
	
	public static class SampledData implements Comparator<SampledData> {
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
		
		@Override
		public int compare(SampledData o1, SampledData o2) {
			int ret = o1.date.compareTo(o2.date);
			if (ret == 0) {
				ret = o1.symbol.compareTo(o2.symbol);
			}
			return ret;
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
			// Build rawDataMap
			Map<String, List<DailyClose>> rawDataMap = new TreeMap<>();
			for(String symbol: symbolList) {
				List<DailyClose> data = JDBCUtil.getResultAll(statement, DailyClose.getSQL(symbol, fromDate), DailyClose.class).stream().collect(Collectors.toList());
				rawDataMap.put(symbol, data);
				logger.info("rawDataMap {} {}", symbol, data.size());
			}
			
			// Build dateList from rawDataMap
			List<String> dateList = rawDataMap.get(symbolList.get(0)).stream().map(o -> o.date).collect(Collectors.toList());
			logger.info("dateList {}", dateList.size());
			
			// Build doubleDataMap from rawDataMap
			Map<String, List<Double>> doubleDataMap = new TreeMap<>();
			for(String symbol: symbolList) {
				List<Double> doubleList = rawDataMap.get(symbol).stream().map(o -> o.close).collect(Collectors.toList());
				doubleDataMap.put(symbol,  doubleList);
			}
			
			// Build statsMap from doubleDataMap
			Map<String, List<MovingStats>> statsMap = new TreeMap<>();
			for(String symbol: symbolList) {
				List<MovingStats> result = doubleDataMap.get(symbol).stream().collect(MovingStats.getInstance(1));
				statsMap.put(symbol, result);
			}

			// Apply statsMap result to doubleDataMap
			for(String symbol: symbolList) {
				doubleDataMap.put(symbol, statsMap.get(symbol).stream().map(o -> o.mean).collect(Collectors.toList()));
			}
			
			//
			// End of data processing
			//
			
			// Build dateMap from doubleDataMap
			Map<String, Map<String, Double>> dateMap = new TreeMap<>();
			{
				int index = 0;
				for(String date: dateList) {
					Map<String, Double> doubleMap = new TreeMap<>();
					for(String symbol: symbolList) {
						List<Double> doubleList = doubleDataMap.get(symbol);
						doubleMap.put(symbol, doubleList.get(index));
					}
					dateMap.put(date, doubleMap);
					//
					index++;
				}
			}			
			logger.info("dateMap  = {}", dateMap.size());

			//
		    // Start output data
			//
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
