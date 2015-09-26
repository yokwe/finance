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

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
	
	public static class Daily {
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
		
		public String date;
		public String symbol;
		public double close;
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
	
	private static String CRLF = "\r\n";
	
	private void doDaily(Statement statement, BufferedWriter output, List<String> symbolList, int lastNMonth) throws IOException {
		Map<String, Map<String, Double>> dateMap = new TreeMap<>();
		for(String symbol: symbolList) {
			for(Daily data: JDBCUtil.getResultAll(statement, Daily.getSQL(symbol, lastNMonth), Daily.class)) {
				if (!dateMap.containsKey(data.date)) {
					dateMap.put(data.date, new TreeMap<>());
				}
				dateMap.get(data.date).put(data.symbol, data.close);
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
				if (!dateMap.containsKey(data.date)) {
					dateMap.put(data.date, new TreeMap<>());
				}
				dateMap.get(data.date).put(data.symbol, data.dividend);
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
			case "daily":
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
			case "daily":
				doDaily(statement, output, symbolList, lastNMonth);
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
