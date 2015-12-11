package yokwe.finance.securities.stats;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.database.PriceTable;
import yokwe.finance.securities.stats.DoubleArray.BiStats;
import yokwe.finance.securities.stats.DoubleArray.UniStats;

public class HV {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(HV.class);

	// calculate composite Historical Volatility
	public static double calculate(Map<String, Data> dataMap, Map<String, Double>allocationMap) {
		final int size = allocationMap.size();
		
		Map<Integer, String> nameMap = new TreeMap<>();
		{
			int i = 0;
			for(String key: allocationMap.keySet()) {
				nameMap.put(i, key);
				i++;
			}
		}
		
		double ratioArray[] = new double[size];
		{
			double sum = allocationMap.values().stream().mapToDouble(o -> o).sum();
			logger.info("SUM          {}", String.format("%5.0f", sum));
			int i = 0;
			for(String key: allocationMap.keySet()) {
				double allocation = allocationMap.get(key);
				double ratio = allocation / sum;;
				ratioArray[i] = ratio;
				logger.info("RATIO {}", String.format("%-6s %5.0f  %8.4f", key, allocation, ratio));
				i++;
			}
		}
		
		UniStats statsArray[] = new UniStats[size];
		{
			int i = 0;
			for(String key: allocationMap.keySet()) {
				Data data = dataMap.get(key);
				statsArray[i] = new DoubleArray.UniStats(DoubleArray.logReturn(data.toDoubleArray()));
				logger.info("HV    {}", String.format("%-6s        %8.4f", data.symbol, statsArray[i].sd));
				i++;
			}
		}
		BiStats statsMatrix[][] = DoubleArray.getMatrix(statsArray);
		{
			StringBuffer sb = new StringBuffer();
			sb.append("CORR        ");
			for(int i = 0; i < size; i++) {
				sb.append(String.format("  %-6s", nameMap.get(i)));
			}
			logger.info(sb.toString());
		}
		for(int i = 0; i < size; i++) {
			StringBuffer sb = new StringBuffer();
			sb.append(String.format("CORR  %-6s", nameMap.get(i)));
			for(int j = 0; j < size; j++) {
				sb.append(String.format("%8.4f", statsMatrix[i][j].correlation));
			}
			logger.info("{}", sb.toString());
		}
		
		
		double hv = 0;
		for(int i = 0; i < size; i++) {
			for(int j = 0; j < size; j++) {
				hv += ratioArray[i] * ratioArray[j] * statsMatrix[i][j].correlation * statsArray[i].sd * statsArray[j].sd;
			}
		}
		logger.info("HV    {}", String.format("              %8.4f", hv));
		return hv;
	}
	
	public static Data getData(Connection connection, String symbol, LocalDate dateFrom, LocalDate dateTo) {
		Data.Daily daily[] = PriceTable.getAllBySymbolDateRange(connection, symbol, dateFrom, dateTo).stream().map(o -> new Data.Daily(o.date, o.volume)).collect(Collectors.toList()).toArray(new Data.Daily[0]);
		return new Data(symbol, daily);
	}
	public static void main(String args[]) {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new SecuritiesException();
		}
		final String JDBC_CONNECTION_URL = "jdbc:sqlite:/data1/home/hasegawa/git/finance/Securities/tmp/sqlite/securities.sqlite3";

		try (Connection connection = DriverManager.getConnection(JDBC_CONNECTION_URL)) {
			LocalDate dateFrom = LocalDate.now().minusYears(1);
			LocalDate dateTo   = LocalDate.now();
			
			Map<String, Double> allocationMap = new TreeMap<>();
			allocationMap.put("VCLT", 1000.0);
			allocationMap.put("PGX",  1000.0);
			allocationMap.put("ARR",  1000.0);
			allocationMap.put("VYM",  1000.0);
			
			logger.info("");
			logger.info("{}", String.format("Date range  %s - %s", dateFrom, dateTo));
			
			Map<String, Data> dataMap = new TreeMap<>();
			for(String symbol: allocationMap.keySet()) {
				Data data = getData(connection, symbol, dateFrom, dateTo);
				dataMap.put(symbol, data);
			}
			
			calculate(dataMap, allocationMap);			
		} catch (SQLException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new SecuritiesException();
		}
	}
}
