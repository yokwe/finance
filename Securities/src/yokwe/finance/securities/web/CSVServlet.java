package yokwe.finance.securities.web;

import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.DoubleUnaryOperator;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class CSVServlet extends HttpServlet {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(CSVServlet.class);
	
	private static final long serialVersionUID = 1L;
	
	private static final String JDBC_CONNECTION_URL = "jdbc:sqlite:/data1/home/hasegawa/git/finance/Securities/tmp/sqlite/securities.sqlite3";
	
	private static final String CRLF = "\r\n";

	@Override
	public void init(ServletConfig config) {
		logger.info("init csv");
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new SecuritiesException();
		}
	}
	
	@Override
	public void destroy() {
		logger.info("destroy csv");
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		logger.info("doGet START");
		
		final List<String>                     symbolList = new ArrayList<>();
		final Data                             data;
		final Period                           period;
		final Map<String, DoubleUnaryOperator> filterMap = new TreeMap<>();
		final boolean                          zero;
		final boolean                          relative;
		final boolean                          logReturn;
		
		final String  baseSymbol;
		
		{
			final Map<String, String[]> paramMap = req.getParameterMap();
			
			// n - normalize
			if (paramMap.containsKey("n")) {
				baseSymbol = paramMap.get("n")[0];
			} else {
				baseSymbol = "";
			}
			
			// s - symbol
			for(String symbol: paramMap.get("s")) {
				if (symbol.contains(",")) {
					for(String s: symbol.split(",")) {
						symbolList.add(s);
					}
				} else {
					symbolList.add(symbol);
				}
			}
			if (symbolList.size() == 0) {
				symbolList.add("SPY");
			}
			// Add baseSymbol to symbolList for normalization
			if (!baseSymbol.equals("") && !symbolList.contains(baseSymbol)) symbolList.add(baseSymbol);
			logger.info("symbolList = {}", symbolList);
			
			// p - period
			//     date-range := date+period | period
			//     period := [0-9]+[ymd]
			//     date := yyyymm
			period = new Period(paramMap.containsKey("p") ? paramMap.get("p")[0] : "12m");
			
			// d - data (price, div or vol)
			data = Data.getInstance(paramMap.containsKey("d") ? paramMap.get("d")[0] : "price");
			
			// f - filter
			//     (avg|sd|skew|kurt)([0-9]+)
			for(String symbol: symbolList) {
				DoubleUnaryOperator op = Filter.getInstance(paramMap.containsKey("f") ? paramMap.get("f")[0] : "avg1");
				filterMap.put(symbol, op);
			}
			
			// z - add zero data
			zero = paramMap.containsKey("z");
			
			// r - show relative to first data
			relative = paramMap.containsKey("r");
			
			// lr - log return
			if (paramMap.containsKey("f")) {
				String filter = paramMap.get("f")[0];
				if (filter.startsWith("ema")) {
					logReturn = true;
				} else if (filter.startsWith("var")) {
					logReturn = true;
				} else {
					logReturn = paramMap.containsKey("lr");
				}
			} else {
				logReturn = paramMap.containsKey("lr");
			}
		}

		resp.setContentType("text/csv; charset=UTF-8");
		
		try {
			// Build dailyDataMap
			Map<String, List<Data.Daily>> dailyDataMap = new TreeMap<>();
			
			try (Connection connection = DriverManager.getConnection(JDBC_CONNECTION_URL)) {
				for(String symbol: symbolList) {
					List<Data.Daily> dailyDataList = data.generate(connection, symbol, period);
					dailyDataMap.put(symbol, dailyDataList);
					logger.info("dailyDataMap {} {}", symbol, dailyDataList.size());
				}
			} catch (SQLException e) {
				logger.error(e.getClass().getName());
				logger.error(e.getMessage());
				throw new SecuritiesException();
			}
			
			// logReturn
			if (logReturn) {
				for(String key: dailyDataMap.keySet()) {
					List<Data.Daily> oldDailyList = dailyDataMap.get(key);
					List<Data.Daily> newDailyList = Data.logReturn(oldDailyList);
					// replace oldDailyData with newDailyData
					oldDailyList.clear();
					oldDailyList.addAll(newDailyList);
				}
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
			Map<String, double[]> doubleDataMap = new TreeMap<>();
			for(String symbol: symbolList) {
				doubleDataMap.put(symbol,  dailyDataMap.get(symbol).stream().mapToDouble(o -> o.value).toArray());
			}
			
			// make relative to first entry
			if (relative) {
				for(double[] values: doubleDataMap.values()) {
					final double base = values[0] * 0.01;
					for(int i = 0; i < values.length; i++) values[i] /= base;
				}
			}
			
			// normalize to baseSymbol
			if (!baseSymbol.equals("")){
				double[] baseArray = doubleDataMap.get(baseSymbol);
				double   baseAvg   = Arrays.stream(baseArray).sum() / baseArray.length;
				for(int i = 0; i < baseArray.length; i++) baseArray[i] = baseArray[i] / baseAvg; // ratio to average
				
				if (baseArray.length != dateList.size()) {
					logger.error("baseArray {}  dateList {}", baseArray.length, dateList.size());
					throw new SecuritiesException("size");
				}
				
				for(String symbol: symbolList) {
					double[] targetArray = doubleDataMap.get(symbol);
					
					if (targetArray.length != dateList.size()) {
						logger.error("targetArray {}  dateList {}", targetArray.length, dateList.size());
						throw new SecuritiesException("size");
					}

					// normalize target with ratio of baseSymbol
					for(int i = 0; i < targetArray.length; i++) targetArray[i] /=  baseArray[i];
				}
				// remove baseSymbol, because value is always one
				doubleDataMap.remove(baseSymbol);
				symbolList.remove(baseSymbol);
			}
			
			// Apply filter with doubleDataMap
			for(String symbol: symbolList) {
				DoubleUnaryOperator op = filterMap.get(symbol);
				// variable data is reference, so no need to copy back to doubleDataMap.
				double[] doubleArray = doubleDataMap.get(symbol);
				for(int i = 0; i < doubleArray.length; i++) {
					doubleArray[i] = op.applyAsDouble(doubleArray[i]);
				}
			}
			
			// Add zero data if requested
			if (zero){
				double[] zeroData = new double[dateList.size()];
				for(int i = 0; i < dateList.size(); i++) zeroData[i] = 0;
				doubleDataMap.put("0", zeroData);
				
				List<Data.Daily> zeroList = new ArrayList<>();
				for(String date: dateList) {
					zeroList.add(new Data.Daily(date, "0", 0.0));
				}
				dailyDataMap.put("0", zeroList);
				symbolList.add("0");
			}

			//
			// End of data processing
			//
			
			// Build dateMap from doubleDataMap
			Map<String, Map<String, Double>> dateMap = new TreeMap<>(); // date symbol value
			for(String symbol: symbolList) {
				// Sanity check
				if (doubleDataMap.get(symbol).length != dailyDataMap.get(symbol).size()) {
					logger.error("size {}  {}  {}", symbol, doubleDataMap.get(symbol).length, dailyDataMap.get(symbol).size());
					throw new SecuritiesException("size");
				}

				int index = 0;
				double[] doubleData = doubleDataMap.get(symbol);
				for(Data.Daily dailyData: dailyDataMap.get(symbol)) {
					final String date = dailyData.date;
					Map<String, Double> map = dateMap.get(date);
					if (map == null) {
						map = new TreeMap<>();
						dateMap.put(date, map);
					}
					map.put(symbol, doubleData[index++]);
				}
			}
			logger.info("dateMap  = {}", dateMap.size());

			try (BufferedWriter output = new BufferedWriter(resp.getWriter(), 65536)) {
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
			} catch (IOException e) {
				logger.error(e.getClass().getName());
				logger.error(e.getMessage());
				throw new SecuritiesException();
			}
		} catch (RuntimeException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			int n = 0;
			for(StackTraceElement stackTrace: e.getStackTrace()) {
				n++;
				if (n == 5) break;
				logger.info("stack {}", stackTrace);
			}
			throw new SecuritiesException();
		}
		logger.info("doGet STOP");
	}
}
