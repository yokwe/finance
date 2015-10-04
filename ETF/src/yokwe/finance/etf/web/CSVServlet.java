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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
		private static String SQL = "select date, symbol, close from yahoo_daily where symbol = '%s' and '%s' <= date and date <= '%s' order by date";
		public static String getSQL(String symbol, String fromDate, String toDate) {
			return String.format(SQL, symbol, fromDate, toDate);
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
		private static String SQL = "select date, symbol, volume from yahoo_daily where symbol = '%s' and '%s' <= date and date <= '%s' order by date";
		public static String getSQL(String symbol, String fromDate, String toDate) {
			return String.format(SQL, symbol, fromDate, toDate);
		}
		
		public String date;
		public String symbol;
		public int    volume;
	}

	public static class Dividend {
		private static String SQL = "select date, symbol, dividend from yahoo_dividend where symbol = '%s' and '%s' <= date and date <= '%s' order by date";
		public static String getSQL(String symbol, String fromDate, String toDate) {
			return String.format(SQL, symbol, fromDate, toDate);
		}
		
		public String date;
		public String symbol;
		public double dividend;
	}
	
	private static String CRLF = "\r\n";
		
	private static class DailyProcessRequest extends ServletProcessor {
		public void process(Statement statement, BufferedWriter output, List<String> symbolList, Period period) throws IOException {
			// Build rawDataMap
			Map<String, List<DailyClose>> rawDataMap = new TreeMap<>();
			for(String symbol: symbolList) {
				List<DailyClose> data = JDBCUtil.getResultAll(statement, DailyClose.getSQL(symbol, period.dateStart, period.dateEnd), DailyClose.class).stream().collect(Collectors.toList());
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

	private static class VolumeProcessor extends ServletProcessor {
		public void process(Statement statement, BufferedWriter output, List<String> symbolList, Period period) throws IOException {
			Map<String, Map<String, Double>> dateMap = new TreeMap<>();
			for(String symbol: symbolList) {
				for(DailyVolume data: JDBCUtil.getResultAll(statement, DailyVolume.getSQL(symbol, period.dateStart, period.dateEnd), DailyVolume.class)) {
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

	private static class DividendProcessor extends ServletProcessor {
		public void process(Statement statement, BufferedWriter output, List<String> symbolList, Period period) throws IOException {
			Map<String, Map<String, Double>> dateMap = new TreeMap<>();
			for(String symbol: symbolList) {
				for(Dividend data: JDBCUtil.getResultAll(statement, Dividend.getSQL(symbol, period.dateStart, period.dateEnd), Dividend.class)) {
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
	
	private static class Period {
		private static Matcher matcherPeriod     = Pattern.compile("([0-9]+)([ymd])").matcher(""); // [0-9]+[ymd]
		private static Matcher matcherDatePeriod = Pattern.compile("([0-9]{6})\\-([0-9]+)([ymd])").matcher(""); // yyyymm-[0-9]+[ymd]
		
		final String dateStart;
		final String dateEnd;
		
		public Period(String value) {
			Calendar today = Calendar.getInstance();
			Calendar calFrom = Calendar.getInstance();
			Calendar calTo   = Calendar.getInstance();
			
			calFrom.setTimeInMillis(today.getTimeInMillis());
			calTo.setTimeInMillis(today.getTimeInMillis());
			
			matcherPeriod.reset(value); // [0-9]+[ymd]
			matcherDatePeriod.reset(value); // yyyymm-[0-9]+[ymd]
			if (matcherPeriod.matches()) {
				// [0-9]+[ymd]
				final int groupCount = matcherPeriod.groupCount();
				if (groupCount != 2) {
					logger.error("groupCount = {}", groupCount);
					throw new ETFException("groupCount");
				}
				String number = matcherPeriod.group(1);
				String mode   = matcherPeriod.group(2);
				final int duration;
				{
					try {
						duration = Integer.parseInt(number);
					} catch (NumberFormatException e) {
						logger.error("number = {}", number);
						throw new ETFException("number");
					}
				}
				
				switch(mode) {
				case "y":
					calFrom.add(Calendar.YEAR, -duration);
					break;
				case "m":
					calFrom.add(Calendar.MONTH, -duration);
					break;
				case "d":
					calFrom.add(Calendar.DATE, -duration);
					break;
				default:
					logger.error("mode = {}", mode);
					throw new ETFException("mode");
				}
			} else if (matcherDatePeriod.matches()) {
				// yyyymm-[0-9]+[ymd]
				final int groupCount = matcherDatePeriod.groupCount();
				if (groupCount != 3) {
					logger.error("groupCount = {}", groupCount);
					throw new ETFException("groupCount");
				}
				String yyyymm = matcherDatePeriod.group(1);
				String number = matcherDatePeriod.group(2);
				String mode   = matcherDatePeriod.group(3);
				{
					String yyyy = yyyymm.substring(0, 4);
					String mm = yyyymm.substring(4, 6);
					
					int year = Integer.parseInt(yyyy);
					int month = Integer.parseInt(mm);
					
					if (year < 1900 || 2100 < year) {
						logger.error("year = {}", year);
						throw new ETFException("year");
					}
					if (month < 1 || 12 < month) {
						logger.error("month = {}", month);
						throw new ETFException("month");
					}
					
					calFrom.set(Calendar.YEAR, year);
					calFrom.set(Calendar.MONTH, month - 1); // 0 base
					calFrom.set(Calendar.DATE, 1); // 1 base
				}

				calTo.setTimeInMillis(calFrom.getTimeInMillis());

				// [0-9]+[ymd]
				{
					int duration;
					try {
						duration = Integer.parseInt(number);
					} catch (NumberFormatException e) {
						logger.error("number = {}", number);
						throw new ETFException("number");
					}
					switch(mode) {
					case "y":
						calTo.add(Calendar.YEAR, duration);
						break;
					case "m":
						calTo.add(Calendar.MONTH, duration);
						break;
					case "d":
						calTo.add(Calendar.DATE, duration);
						break;
					default:
						logger.error("mode = {}", mode);
						throw new ETFException("mode");
					}
				}
			} else {
				logger.error("value = {}", value);
				throw new ETFException("period");
			}
			
			dateStart = dateString(calFrom);
			dateEnd   = dateString(calTo);
			logger.info("period = {}  {} - {}", value, dateStart, dateEnd);
		}
	}

	private static abstract class ServletProcessor {
		private static Map<String, ServletProcessor> processorMap = new TreeMap<>();
		static {
			processorMap.put("daily",    new DailyProcessRequest());
			processorMap.put("volume",   new VolumeProcessor());
			processorMap.put("dividend", new DividendProcessor());
		}
		public static ServletProcessor getInstance(String type) {
			logger.info("type = {}", type);
			ServletProcessor ret = processorMap.get(type);
			if (ret == null) {
				logger.error("Unknown type = {}", type);
				throw new ETFException();
			}
			return ret;
		}

		public abstract void process(Statement statement, BufferedWriter output, List<String> symbolList, Period period) throws IOException;
	}
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		logger.info("doGet START");
		
		final List<String> symbolList = new ArrayList<>();
		final String type;
		final Period period;
		final ServletProcessor processor;
		{
			Map<String, String[]> paramMap = req.getParameterMap();
			
			// s - symbol
			if (paramMap.containsKey("s")) {
				for(String symbol: paramMap.get("s")) {
					symbolList.add(symbol);
				}
			}
			if (symbolList.size() == 0) symbolList.add("SPY");
			logger.info("symbolList = {}", symbolList);
			
			// p - period
			//     date-range := date+period | period
			//     period := [0-9]+[ymd]
			//     date := yyyymm
			period = new Period(paramMap.containsKey("p") ? paramMap.get("p")[0] : "12m");
			
			// t - type (daily, dividend or volume)
			type = paramMap.containsKey("t") ? paramMap.get("t")[0] : "daily";
			processor = ServletProcessor.getInstance(type);
			
			// TODO implement filter for moving average, sd, kurt or skew.  should be ma20 or ma200
			// f - filter
			//     mavg[0-9]+  msd[0-9]+ mskew[0-9]+ mkurt[0-9]+
			// Filter filter = new Filter(paramMap.containsKey("f") ? paramMap.get("f")[0] : "mavg1");
		}

		resp.setContentType("text/csv; charset=UTF-8");
		
		try (
				Statement statement = DriverManager.getConnection(JDBC_CONNECTION_URL).createStatement();
				BufferedWriter output = new BufferedWriter(resp.getWriter(), 65536);) {
			processor.process(statement, output, symbolList, period);
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
