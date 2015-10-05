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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;

import yokwe.finance.etf.ETFException;
import yokwe.finance.etf.util.JDBCUtil;
import yokwe.finance.etf.util.StreamUtil.MovingStats;
import yokwe.finance.etf.util.StreamUtil.MovingStats.Accumlator;

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

	
	private static abstract class Generator {
		private static Map<String, Generator> generatorMap = new TreeMap<>();
		static {
			generatorMap.put("price", new PriceData());
			generatorMap.put("vol",   new VolumeData());
			generatorMap.put("div",   new DividendData());
		}
		public static Generator getInstance(String type) {
			logger.info("type = {}", type);
			Generator ret = generatorMap.get(type);
			if (ret == null) {
				logger.error("Unknown type = {}", type);
				throw new ETFException();
			}
			return ret;
		}

		public abstract List<DailyData> generate(Statement statement, String symbol, Period period);
	}
	
	public static class PriceData extends Generator {
		private static String SQL = "select date, symbol, close from yahoo_daily where symbol = '%s' and '%s' <= date and date <= '%s' order by date";
		private static String getSQL(String symbol, String fromDate, String toDate) {
			return String.format(SQL, symbol, fromDate, toDate);
		}
		
		public String date;
		public String symbol;
		public double close;
		
		@Override
		public String toString() {
			return String.format("[%s %s %6.3f", date, symbol, close);
		}
		
		public List<DailyData> generate(Statement statement, String symbol, Period period) {
			List<DailyData> ret = JDBCUtil.getResultAll(statement, getSQL(symbol, period.dateStart, period.dateEnd), PriceData.class).stream().map(o -> o.toDailyData()).collect(Collectors.toList());
			return ret;
		}

		private DailyData toDailyData() {
			return new DailyData(date, symbol, close);
		}
	}

	public static class VolumeData extends Generator {
		private static String SQL = "select date, symbol, volume from yahoo_daily where symbol = '%s' and '%s' <= date and date <= '%s' order by date";
		private static String getSQL(String symbol, String fromDate, String toDate) {
			return String.format(SQL, symbol, fromDate, toDate);
		}
		
		public String date;
		public String symbol;
		public int    volume;
		
		public List<DailyData> generate(Statement statement, String symbol, Period period) {
			List<DailyData> ret = JDBCUtil.getResultAll(statement, VolumeData.getSQL(symbol, period.dateStart, period.dateEnd), VolumeData.class).stream().map(o -> o.toDailyData()).collect(Collectors.toList());
			return ret;
		}

		private DailyData toDailyData() {
			return new DailyData(date, symbol, volume);
		}
	}

	public static class DividendData extends Generator {
		private static String SQL = "select date, symbol, dividend from yahoo_dividend where symbol = '%s' and '%s' <= date and date <= '%s' order by date";
		private static String getSQL(String symbol, String fromDate, String toDate) {
			return String.format(SQL, symbol, fromDate, toDate);
		}
		
		public String date;
		public String symbol;
		public double dividend;
		
		public List<DailyData> generate(Statement statement, String symbol, Period period) {
			List<DailyData> ret = JDBCUtil.getResultAll(statement, getSQL(symbol, period.dateStart, period.dateEnd), DividendData.class).stream().map(o -> o.toDailyData()).collect(Collectors.toList());
			return ret;
		}

		private DailyData toDailyData() {
			return new DailyData(date, symbol, dividend);
		}
	}
	
	public static class DailyData {
		public String date;
		public String symbol;
		public double value;
		
		public DailyData(String date, String symbol, double value) {
			this.date   = date;
			this.symbol = symbol;
			this.value  = value;
		}
	}
	
	private static String CRLF = "\r\n";
	
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
				try {
					duration = Integer.parseInt(number);
				} catch (NumberFormatException e) {
					logger.error("number = {}", number);
					throw new ETFException("number");
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

	private static class Filter {
		private static Matcher matcher = Pattern.compile("(avg|sd|skew|kurt)([0-9]+)").matcher("");

		static Map<String, Function<MovingStats, Double>> mapperMap = new TreeMap<>();
		static {
			mapperMap.put("avg",  o -> o.mean);
			mapperMap.put("sd",   o -> o.standardDeviation);
			mapperMap.put("skew", o -> o.skewness);
			mapperMap.put("kurt", o -> o.kurtosis);
		}
		
		private Function<MovingStats, Double>                    mapper;
		private int                                              interval;
		private Collector<Double, Accumlator, List<MovingStats>> collector;
		public Filter(String value) {
			matcher.reset(value);
			if (matcher.matches()) {
				if (matcher.groupCount() != 2) {
					logger.error("groupCount = {}", matcher.groupCount());
					throw new ETFException("groupCount");
				}
				String type   = matcher.group(1);
				String number = matcher.group(2);
				try {
					interval = Integer.parseInt(number);
				} catch (NumberFormatException e) {
					logger.error("number = {}", number);
					throw new ETFException("number");
				}
				mapper = mapperMap.get(type);
				if (mapper == null) {
					logger.error("type = {}", type);
					throw new ETFException("type");
				}
				logger.info("filter {} {}", type, number);
			} else {
				logger.error("value = {}", value);
				throw new ETFException("fiter");
			}
			collector = MovingStats.getInstance(interval);
		}
		
		public List<Double> apply(List<Double> data) {
			List<MovingStats> stats = data.stream().collect(collector);
			List<Double> ret = stats.stream().map(mapper).collect(Collectors.toList());
			return ret;
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		logger.info("doGet START");
		
		final List<String> symbolList = new ArrayList<>();
		final Generator    generator;
		final Period       period;
		final Filter       filter;
		
		{
			Map<String, String[]> paramMap = req.getParameterMap();
			
			// s - symbol
			if (paramMap.containsKey("s")) {
				for(String symbol: paramMap.get("s")) {
					symbolList.add(symbol);
				}
			} else {
				symbolList.add("SPY");
			}
			logger.info("symbolList = {}", symbolList);
			
			// p - period
			//     date-range := date+period | period
			//     period := [0-9]+[ymd]
			//     date := yyyymm
			period = new Period(paramMap.containsKey("p") ? paramMap.get("p")[0] : "12m");
			
			// t - type (daily, dividend or volume)
			generator = Generator.getInstance(paramMap.containsKey("t") ? paramMap.get("t")[0] : "price");
			
			// f - filter
			//     avg[0-9]+  sd[0-9]+  skew[0-9]+ kurt[0-9]+
			// Filter filter = new Filter(paramMap.containsKey("f") ? paramMap.get("f")[0] : "mavg1");
			filter = new Filter(paramMap.containsKey("f") ? paramMap.get("f")[0] : "avg1");
		}

		resp.setContentType("text/csv; charset=UTF-8");
		
		try (
				Statement statement = DriverManager.getConnection(JDBC_CONNECTION_URL).createStatement();
				BufferedWriter output = new BufferedWriter(resp.getWriter(), 65536);) {
			// Build dailyDataMap
			Map<String, List<DailyData>> dailyDataMap = new TreeMap<>();
			for(String symbol: symbolList) {
				List<DailyData> dailyDataList = generator.generate(statement, symbol, period);
				dailyDataMap.put(symbol, dailyDataList);
				logger.info("dailyDataMap {} {}", symbol, dailyDataList.size());
			}
			
			// Build dateList from dailyDataMap
			final List<String> dateList;
			{
				Set<String> dateSet = new TreeSet<>();
				for(String symbol: symbolList) {
					dailyDataMap.get(symbol).stream().forEach(o -> dateSet.add(o.date));
				}
				dateList = new ArrayList<>(dateSet);
				logger.info("dateList {}", dateList.size());
			}
		
			// Build doubleDataMap from dailyDataMap
			Map<String, List<Double>> doubleDataMap = new TreeMap<>();
			for(String symbol: symbolList) {
				List<Double> doubleList = dailyDataMap.get(symbol).stream().map(o -> o.value).collect(Collectors.toList());
				doubleDataMap.put(symbol,  doubleList);
			}
			
			// Apply filter with doubleDataMap
			for(String symbol: symbolList) {
				List<Double> filtered = filter.apply(doubleDataMap.get(symbol));
				doubleDataMap.put(symbol, filtered);
			}
			
			//
			// End of data processing
			//
			
			// Build dateMap from doubleDataMap
			Map<String, Map<String, Double>> dateMap = new TreeMap<>(); // date symbol value
			for(String symbol: symbolList) {
				// Sanity check
				if (doubleDataMap.get(symbol).size() != dailyDataMap.get(symbol).size()) {
					logger.error("size {}  {}  {}", symbol, doubleDataMap.get(symbol).size(), dailyDataMap.get(symbol).size());
					throw new ETFException("size");
				}

				int index = 0;
				List<Double> doubleList = doubleDataMap.get(symbol);
				for(DailyData dailyData: dailyDataMap.get(symbol)) {
					final String date = dailyData.date;
					Map<String, Double> map = dateMap.get(date);
					if (map == null) {
						map = new TreeMap<>();
						dateMap.put(date, map);
					}
					map.put(symbol, doubleList.get(index++));
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
