package yokwe.finance.etf.web;

import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;

import yokwe.finance.etf.ETFException;

public class CSVServlet extends HttpServlet {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(CSVServlet.class);
	
	private static final long serialVersionUID = 1L;
	
	private static final String JDBC_CONNECTION_URL = "jdbc:sqlite:/data1/home/hasegawa/git/finance/ETF/tmp/sqlite/etf.sqlite3";
	
	private static final String CRLF = "\r\n";

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
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		logger.info("doGet START");
		
		final List<String> symbolList = new ArrayList<>();
		final Generator    generator;
		final Period       period;
		final Filter       filter;
		
		{
			final Map<String, String[]> paramMap = req.getParameterMap();
			
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
			
			// t - type (price, div or vol)
			generator = Generator.getInstance(paramMap.containsKey("t") ? paramMap.get("t")[0] : "price");
			
			// f - filter
			//     (avg|sd|skew|kurt)([0-9]+)
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
