package yokwe.finance.etf.web;

import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
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
	
	static {
		logger.info("load csv");
	}
	
	@Override
	public void init(ServletConfig config) {
		logger.info("init csv");
	}
	
	@Override
	public void destroy() {
		logger.info("destroy csv");
	}
	
	public static class Data {
		private static String SQL = "select date, symbol, close from yahoo_daily where symbol = '%s' order by date";
		public static String getSQL(String symbol) {
			
			return String.format(SQL, symbol);
		}
		public String date;
		public String symbol;
		public double close;
	}
	
	private static String CRLF = "\r\n";
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		logger.info("doGet START");
		
		logger.info("parameterMap = {}", req.getParameterMap());
		
		resp.setContentType("text/csv; charset=UTF-8");

		try {
			Class.forName("org.sqlite.JDBC");
			try (Connection connection = DriverManager.getConnection("jdbc:sqlite:/data1/home/hasegawa/git/finance/ETF/tmp/sqlite/etf.sqlite3")) {
				Statement statement = connection.createStatement();
				List<Data> listQQQ  = JDBCUtil.getResultAll(statement, Data.getSQL("QQQ"), Data.class);
				List<Data> listSPY  = JDBCUtil.getResultAll(statement, Data.getSQL("SPY"), Data.class);
				
				logger.info("listQQQ = {}", listQQQ.size());
				logger.info("listSPY = {}", listSPY.size());
				
				List<String> fieldNameList = new ArrayList<>();
				fieldNameList.add("SPY");
				fieldNameList.add("QQQ");
				Collections.sort(fieldNameList);

				Map<String, Map<String, Double>> dateMap = new TreeMap<>();
				for(Data data: listQQQ) {
					if (!dateMap.containsKey(data.date)) {
						dateMap.put(data.date, new TreeMap<>());
					}
					dateMap.get(data.date).put(data.symbol, data.close);
				}
				for(Data data: listSPY) {
					if (!dateMap.containsKey(data.date)) {
						dateMap.put(data.date, new TreeMap<>());
					}
					dateMap.get(data.date).put(data.symbol, data.close);
				}
				
				logger.info("dateMap  = {}", dateMap.size());

				
				try (BufferedWriter bw = new BufferedWriter(resp.getWriter(), 65536)) {					
					StringBuilder line = new StringBuilder();
					
					// Output header
					line.setLength(0);
					line.append("date");
					for(String fieldName: fieldNameList) {
						if (0 < line.length()) line.append(",");
						line.append(fieldName);
					}
					bw.append(line.toString()).append(CRLF);
					logger.info("fieldNameList = {}", line.toString());

					
					for(String date: dateMap.keySet()) {
						Map<String, Double> record = dateMap.get(date);
						line.setLength(0);
						line.append(date);
						for(String symbol: fieldNameList) {
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
			}
		} catch (ClassNotFoundException | SQLException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
		} catch (RuntimeException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			int n = 0;
			for(StackTraceElement stackTrace: e.getStackTrace()) {
				n++;
				if (n == 5) break;
				logger.info("stack {}", stackTrace);
			}
		}
		logger.info("doGet STOP");
	}
}
