package yokwe.finance.etf.web;

import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.Connection;
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
	
	public static class Data {
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
	
	private static String CRLF = "\r\n";
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		logger.info("doGet START");
		
		final List<String> symbolList = new ArrayList<>();
		int lastNMonth;

		{
			Map<String, String[]> paramMap = req.getParameterMap();
			
			if (paramMap.containsKey("symbol")) {
				for(String symbol: paramMap.get("symbol")) {
					symbolList.add(symbol);
				}
			}
			if (symbolList.size() == 0) symbolList.add("SPY");
			logger.info("symbolList = {}", symbolList);
			
			{
				if (paramMap.containsKey("lastNMonth")) {
					String s = paramMap.get("lastNMonth")[0];
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
			}
			logger.info("lastNMonth = {}", lastNMonth);
		}

		resp.setContentType("text/csv; charset=UTF-8");
		
		try {
			try (Connection connection = DriverManager.getConnection("jdbc:sqlite:/data1/home/hasegawa/git/finance/ETF/tmp/sqlite/etf.sqlite3")) {
				Statement statement = connection.createStatement();
				
				Map<String, Map<String, Double>> dateMap = new TreeMap<>();
				for(String symbol: symbolList) {
					for(Data data: JDBCUtil.getResultAll(statement, Data.getSQL(symbol, lastNMonth), Data.class)) {
						if (!dateMap.containsKey(data.date)) {
							dateMap.put(data.date, new TreeMap<>());
						}
						dateMap.get(data.date).put(data.symbol, data.close);
					}
				}
				
				logger.info("dateMap  = {}", dateMap.size());

				try (BufferedWriter bw = new BufferedWriter(resp.getWriter(), 65536)) {					
					StringBuilder line = new StringBuilder();
					
					// Output header
					line.setLength(0);
					line.append("date");
					for(String fieldName: symbolList) {
						if (0 < line.length()) line.append(",");
						line.append(fieldName);
					}
					bw.append(line.toString()).append(CRLF);
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
						bw.append(line.toString()).append(CRLF);
					}
				} catch (IOException e) {
					logger.error(e.getClass().getName());
					logger.error(e.getMessage());
					throw new ETFException();
				}
			} catch (SQLException e) {
				logger.error(e.getClass().getName());
				logger.error(e.getMessage());
				throw new ETFException();
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
			throw new ETFException();
		}
		logger.info("doGet STOP");
	}
}
