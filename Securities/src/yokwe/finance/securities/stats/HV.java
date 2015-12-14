package yokwe.finance.securities.stats;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.database.PriceTable;
import yokwe.finance.securities.stats.DoubleArray.BiStats;
import yokwe.finance.securities.stats.DoubleArray.UniStats;

public class HV {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(HV.class);

	public static final double CONFIDENCE_95_PERCENT = 1.65;
	public static final double CONFIDENCE_99_PERCENT = 2.33;

	// calculate composite Historical Volatility
	public static double calculate(Data data, Map<String, Double>assetMap) {
		final int size = assetMap.size();
		
		Map<Integer, String> nameMap = new TreeMap<>();
		{
			int i = 0;
			for(String key: assetMap.keySet()) {
				nameMap.put(i, key);
				i++;
			}
		}
		
		double ratioArray[] = new double[size];
		{
			double sum = assetMap.values().stream().mapToDouble(o -> o).sum();
			logger.info("SUM          {}", String.format("%5.0f", sum));
			int i = 0;
			for(String key: assetMap.keySet()) {
				double allocation = assetMap.get(key);
				double ratio = allocation / sum;;
				ratioArray[i] = ratio;
				logger.info("RATIO {}", String.format("%-6s %5.0f  %8.4f", key, allocation, ratio));
				i++;
			}
		}
		
		UniStats statsArray[] = new UniStats[size];
		{
			logger.info("HV                    SD      VAR 1d VAR 1m");
			int i = 0;
			for(String symbol: assetMap.keySet()) {
				statsArray[i]  = new DoubleArray.UniStats(DoubleArray.logReturn(data.toDoubleArray(symbol)));
				double amount = assetMap.get(symbol);
				double sd     = statsArray[i].sd;
				double var1d  = sd * CONFIDENCE_95_PERCENT * amount;
				double var1m  = sd * CONFIDENCE_95_PERCENT * amount * Math.sqrt(21);
				logger.info("HV    {}", String.format("%-6s        %8.4f%8.2f%8.2f", symbol, sd, var1d, var1m));
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
		hv = Math.sqrt(hv);
		logger.info("HV    {}", String.format("              %8.4f", hv));
		return hv;
	}
	public static double calculateTerse(Data data, Map<String, Double>assetMap) {
		final int size = assetMap.size();
		
		Map<Integer, String> nameMap = new TreeMap<>();
		{
			int i = 0;
			for(String key: assetMap.keySet()) {
				nameMap.put(i, key);
				i++;
			}
		}
		
		double ratioArray[] = new double[size];
		{
			double sum = assetMap.values().stream().mapToDouble(o -> o).sum();
			int i = 0;
			for(String key: assetMap.keySet()) {
				double allocation = assetMap.get(key);
				double ratio = allocation / sum;;
				ratioArray[i] = ratio;
				i++;
			}
		}
		
		UniStats statsArray[] = new UniStats[size];
		{
			int i = 0;
			for(String symbol: assetMap.keySet()) {
				statsArray[i]  = new DoubleArray.UniStats(DoubleArray.logReturn(data.toDoubleArray(symbol)));
				i++;
			}
		}
		BiStats statsMatrix[][] = DoubleArray.getMatrix(statsArray);		
		
		double hv = 0;
		for(int i = 0; i < size; i++) {
			for(int j = 0; j < size; j++) {
				hv += ratioArray[i] * ratioArray[j] * statsMatrix[i][j].correlation * statsArray[i].sd * statsArray[j].sd;
			}
		}
		hv = Math.sqrt(hv);
		return hv;
	}
	public static double calculate(UniStats statsArray[], double ratioArray[]) {
		if (statsArray.length != ratioArray.length) {
			logger.error("statsArray.length = {}  ratioArray.length = {}", statsArray.length, ratioArray.length);
			throw new SecuritiesException("statsArray.length != ratioArray.length");
		}

		final int size = statsArray.length;
		BiStats statsMatrix[][] = DoubleArray.getMatrix(statsArray);		
		
		double hv = 0;
		for(int i = 0; i < size; i++) {
			for(int j = 0; j < size; j++) {
				hv += ratioArray[i] * ratioArray[j] * statsMatrix[i][j].correlation * statsArray[i].sd * statsArray[j].sd;
			}
		}
		hv = Math.sqrt(hv);
		return hv;
	}

	public static double calculate(Allocation allocations[]) {
		final int size = allocations.length;
		
		double ratioArray[] = new double[size];
		{
			double amountTotal = Arrays.stream(allocations).mapToInt(o -> o.amount).sum();
			logger.info("SUM          {}", String.format("%5d", (int)amountTotal));
			for(int i = 0; i < size; i++) {
				Allocation allocation = allocations[i];
				double ratio = allocation.amount / amountTotal;;
				ratioArray[i] = ratio;
				logger.info("RATIO {}", String.format("%-6s %5d  %8.4f", allocation.asset.symbol, allocation.amount, ratio));
			}
		}
		
		UniStats statsArray[] = new UniStats[size];
		{
			logger.info("HV                    SD      VAR 1d  VAR 1m");
			int i = 0;
			for(Allocation allocation: allocations) {
				String symbol = allocation.asset.symbol;
				double amount = allocation.amount;
				statsArray[i]  = new DoubleArray.UniStats(DoubleArray.logReturn(allocation.asset.price));
				double sd     = statsArray[i].sd;
				double var1d  = sd * CONFIDENCE_95_PERCENT * amount;
				double var1m  = sd * CONFIDENCE_95_PERCENT * amount * Math.sqrt(21);
				logger.info("HV    {}", String.format("%-6s        %8.4f%8.2f%8.2f", symbol, sd, var1d, var1m));
				i++;
			}
		}
		BiStats statsMatrix[][] = DoubleArray.getMatrix(statsArray);
		{
			StringBuffer sb = new StringBuffer();
			sb.append("CORR        ");
			for(int i = 0; i < size; i++) {
				sb.append(String.format("  %-6s", allocations[i].asset.symbol));
			}
			logger.info(sb.toString());
		}
		for(int i = 0; i < size; i++) {
			StringBuffer sb = new StringBuffer();
			sb.append(String.format("CORR  %-6s", allocations[i].asset.symbol));
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
		hv = Math.sqrt(hv);
		logger.info("HV    {}", String.format("              %8.4f", hv));
		return hv;
	}
	public static double calculateTerse(Allocation allocations[]) {
		final int size = allocations.length;
		
		double ratioArray[] = new double[size];
		{
			double amountTotal = Arrays.stream(allocations).mapToInt(o -> o.amount).sum();
//			logger.info("SUM          {}", String.format("%5d", (int)amountTotal));
			for(int i = 0; i < size; i++) {
				Allocation allocation = allocations[i];
				double ratio = allocation.amount / amountTotal;;
				ratioArray[i] = ratio;
//				logger.info("RATIO {}", String.format("%-6s %5d  %8.4f", allocation.asset.symbol, allocation.amount, ratio));
			}
		}
		
		UniStats statsArray[] = new UniStats[size];
		{
//			logger.info("HV                    SD      VAR 1d  VAR 1m");
			int i = 0;
			for(Allocation allocation: allocations) {
				statsArray[i]  = new DoubleArray.UniStats(DoubleArray.logReturn(allocation.asset.price));
//				String symbol = allocation.asset.symbol;
//				double amount = allocation.amount;
//				double sd     = statsArray[i].sd;
//				double var1d  = sd * CONFIDENCE_95_PERCENT * amount;
//				double var1m  = sd * CONFIDENCE_95_PERCENT * amount * Math.sqrt(21);
//				logger.info("HV    {}", String.format("%-6s        %8.4f%8.2f%8.2f", symbol, sd, var1d, var1m));
				i++;
			}
		}
		BiStats statsMatrix[][] = DoubleArray.getMatrix(statsArray);
//		{
//			StringBuffer sb = new StringBuffer();
//			sb.append("CORR        ");
//			for(int i = 0; i < size; i++) {
//				sb.append(String.format("  %-6s", allocations[i].asset.symbol));
//			}
//			logger.info(sb.toString());
//		}
//		for(int i = 0; i < size; i++) {
//			StringBuffer sb = new StringBuffer();
//			sb.append(String.format("CORR  %-6s", allocations[i].asset.symbol));
//			for(int j = 0; j < size; j++) {
//				sb.append(String.format("%8.4f", statsMatrix[i][j].correlation));
//			}
//			logger.info("{}", sb.toString());
//		}
		
		double hv = 0;
		for(int i = 0; i < size; i++) {
			for(int j = 0; j < size; j++) {
				hv += ratioArray[i] * ratioArray[j] * statsMatrix[i][j].correlation * statsArray[i].sd * statsArray[j].sd;
			}
		}
		hv = Math.sqrt(hv);
//		logger.info("HV    {}", String.format("              %8.4f", hv));
		return hv;
	}

	
	public static Data getData(Connection connection, String symbol, LocalDate dateFrom, LocalDate dateTo) {
		List<PriceTable> priceList = PriceTable.getAllBySymbolDateRange(connection, symbol, dateFrom, dateTo);
		return new Data(priceList);
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
			LocalDate dateTo   = LocalDate.now();
			LocalDate dateFrom = dateTo.minusYears(1);
//			LocalDate dateFrom = dateTo.minusMonths(1);
			
			Map<String, Double> assetMap = new TreeMap<>();
			assetMap.put("VCLT", 8600.0);
			assetMap.put("PGX",  4400.0);
			assetMap.put("VYM",  3400.0);
//			assetMap.put("ARR",  2100.0);
			double assetSum = assetMap.values().stream().mapToDouble(o -> o).sum();
			
			logger.info("");
			logger.info("{}", String.format("Date range  %s - %s", dateFrom, dateTo));
			
			Data data;
			{
				List<PriceTable> all = new ArrayList<>();
				for(String symbol: assetMap.keySet()) {
					all.addAll(PriceTable.getAllBySymbolDateRange(connection, symbol, dateFrom, dateTo));
				}
				data = new Data(all);
			}			
			double hv = calculate(data, assetMap);
			logger.info("");
			logger.info("{}", String.format("SUM         %8.2f", assetSum));
			logger.info("{}", String.format("VAR 1d      %8.2f", hv * assetSum * CONFIDENCE_95_PERCENT));
			logger.info("{}", String.format("VAR 1m      %8.2f", hv * assetSum * CONFIDENCE_95_PERCENT  * Math.sqrt(21)));
			
			{
				final double assetUnit = 10;
				Random random = new Random(System.currentTimeMillis());
				final int numberOfAsset = assetMap.size();
				double ratio[] = new double[numberOfAsset];
				Map<String, Double> randomAssetMap = new TreeMap<>();

				double minHV = hv;
				for(int i = 0; i < 10000; i++) {
					double ratioSum = 0.0;
					for(int j = 0; j < ratio.length; j++) ratio[j] = random.nextDouble();
					for(int j = 0; j < ratio.length; j++) ratioSum += ratio[j];
					for(int j = 0; j < ratio.length; j++) ratio[j] = Math.round(assetSum * (ratio[j] / ratioSum) / assetUnit) * assetUnit;
					{
						int j = numberOfAsset;
						double remain = assetSum;
						for(String symbol: assetMap.keySet()) {
							j--;
							if (j == 0) {
								randomAssetMap.put(symbol, remain);
								break;
							}
							double value = Math.round(ratio[j]);
							remain -= value;
							randomAssetMap.put(symbol, value);
						}
					}
					double tempHV = calculateTerse(data, randomAssetMap);
					if (tempHV < minHV) {
						minHV = tempHV;

						logger.info("");
						logger.info("minHV {}", String.format("              %8.4f", minHV));
//						for(int j = 0; j < ratio.length; j++) logger.info("ratio {}", String.format("%6.0f", ratio[j]));
						{
							double sum = randomAssetMap.values().stream().mapToDouble(o -> o).sum();
							logger.info("SUM          {}", String.format("%5.0f", sum));
							for(String key: randomAssetMap.keySet()) {
								double allocation = randomAssetMap.get(key);
								logger.info("RATIO {}", String.format("%-6s %5.0f  %8.4f", key, allocation, allocation / sum));
							}
						}


//						{
//							double assetSum = randomAssetMap.values().stream().mapToDouble(o -> o).sum();
//							logger.info("XX SUM          {}", String.format("%5.0f", assetSum));
//							for(String key: assetMap.keySet()) {
//								double allocation = assetMap.get(key);
//								logger.info("RATIO {}", String.format("%-6s %5.0f  %8.4f", key, allocation, allocation / assetSum));
//							}
//						}
//						logger.info("XX {}", String.format("VAR 1d      %8.2f", hv * sum * CONFIDENCE_95_PERCENT));
//						logger.info("XX {}", String.format("VAR 1m      %8.2f", hv * sum * CONFIDENCE_95_PERCENT  * Math.sqrt(21)));
					}
				}
			}
		} catch (SQLException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new SecuritiesException();
		}
	}
}
