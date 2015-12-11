package yokwe.finance.securities.stats;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.database.PriceTable;
import yokwe.finance.securities.stats.DoubleArray.BiStats;
import yokwe.finance.securities.stats.DoubleArray.UniStats;

public class HV {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(HV.class);

	public static class Asset {
		public final int    amount;
		public final Data   data;
		public Asset(int amount, Data data) {
			this.amount = amount;
			this.data   = data;
		}
	}

	// calculate composite Historical Volatility
	public static double calculate(List<Asset> assetList) {
		final Asset assets[] = assetList.toArray(new Asset[0]);
		final int size = assets.length;
		
		// TODO take log return of Data.Daily
		
		double ratioArray[] = new double[size];
		{
			double sum = Arrays.stream(assets).mapToDouble(o -> o.amount).sum();
			for(int i = 0; i < size; i++) ratioArray[i] = sum / assets[i].amount;
		}
		
		UniStats statsArray[] = new UniStats[size];
		for(int i = 0; i < size; i++) {
			statsArray[i] = new DoubleArray.UniStats(assets[i].data.toDoubleArray());
			logger.info("{}", String.format("%-6s  %s", assets[i].data.symbol, statsArray[i]));
		}
		BiStats statsMatrix[][] = DoubleArray.getMatrix(statsArray);
		
		double hv = 0;
		for(int i = 0; i < size; i++) {
			for(int j = 0; j < size; j++) {
				hv += ratioArray[i] * ratioArray[j] * statsMatrix[i][j].correlation * statsArray[i].sd * statsArray[j].sd;
			}
		}
		
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
			
			String symbolArray[] = {"VCLT", "PGX", "ARR", "VYM", };
			
			logger.info("");
			logger.info("{}", String.format("Date range  %s - %s", dateFrom, dateTo));
			
			List<Asset> assetList = new ArrayList<>();
			for(String symbol: symbolArray) {
				Data data = getData(connection, symbol, dateFrom, dateTo);
				assetList.add(new Asset(100, data));
			}
			
			logger.info("{}", String.format("hv = %8.4f", calculate(assetList)));
			
		} catch (SQLException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new SecuritiesException();
		}
	}
}
