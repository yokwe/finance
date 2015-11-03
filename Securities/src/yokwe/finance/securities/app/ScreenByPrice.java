package yokwe.finance.securities.app;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.database.NasdaqTable;
import yokwe.finance.securities.database.PriceTable;

public class ScreenByPrice {
	private static final Logger logger = LoggerFactory.getLogger(ScreenByPrice.class);
	
	private static class DateRange {
		public static LocalDate getTradingDate(Connection connection, final LocalDate date) {
			LocalDate tradingDate = date.minusDays(0);
			if (isTradingDay(connection, tradingDate)) return tradingDate;
			tradingDate = tradingDate.minusDays(1);
			if (isTradingDay(connection, tradingDate)) return tradingDate;
			tradingDate = tradingDate.minusDays(1);
			if (isTradingDay(connection, tradingDate)) return tradingDate;
			
			logger.error("date = {}  tradingDate = {}", date, tradingDate);
			throw new SecuritiesException("tradingDate");
		}

		public final int       days;
		public final LocalDate fromDate;
		public final LocalDate toDate;
		public DateRange(Connection connection, LocalDate date, int daysToSubtract) {
			days     = daysToSubtract;
			toDate   = getTradingDate(connection, date);
			fromDate = getTradingDate(connection, toDate.minusDays(daysToSubtract));
		}
		@Override
		public String toString() {
			return String.format("{%d  %s  %s}", days, fromDate, toDate);
		}
	}
	
	static boolean isTradingDay(Connection connection, LocalDate date) {
		PriceTable priceTable = PriceTable.getBySymbolDate(connection, "NYT", date);
		return priceTable != null;
	}
	// Remove out of bound value from values data
	static void calculate(Connection connection, Writer w) throws IOException {
		String lastTradedDate = PriceTable.getLastTradeDate(connection);
		
		Map<String, DateRange> dateMap = new TreeMap<>();
		{
			// Sometime yahoo daily data is not complete for last day. So use previous date of last traded day
			LocalDate origin = LocalDate.parse(lastTradedDate).minusDays(1);
			dateMap.put("10", new DateRange(connection, origin, 10));
			dateMap.put("20", new DateRange(connection, origin, 20));
			dateMap.put("30", new DateRange(connection, origin, 30));
			dateMap.put("90", new DateRange(connection, origin, 90));
		}
		
		logger.info("dateMap = {}", dateMap);
		
		Map<String, NasdaqTable>  nasdaqMap  = NasdaqTable.getMap(connection);
		
		logger.info("nasdaqMap     = {}", nasdaqMap.size());
		
		// candidateList has all symbols
		List<String> candidateList = nasdaqMap.keySet().stream().collect(Collectors.toList());
		
		// Calculate price change of each period
		Map<String, Map<String, Double>> priceMap = new TreeMap<>();
		for(String symbol: candidateList) {
			Map<String, Double> map = new TreeMap<>();
			for(String key: dateMap.keySet()) {
				PriceTable fromTable = PriceTable.getBySymbolDate(connection, symbol, dateMap.get(key).fromDate);
				PriceTable toTable   = PriceTable.getBySymbolDate(connection, symbol, dateMap.get(key).toDate);
				
				if (symbol.equals("YYY")) {
					logger.info("{}  {}  {}  {}", symbol, key, fromTable, toTable);
				}
				if (toTable == null)   continue;
				if (fromTable == null) continue;
				
				map.put(key, fromTable.close - toTable.close);
			}
			if (0 < map.size()) {
				priceMap.put(symbol, map);
			} else {
//				logger.info("no price  {}", symbol);
			}
		}
		logger.info("priceMap = {}", priceMap.size());
		
		logger.info("candidateList = {}", candidateList.size());
	}
	
	private static final String OUTPUT_PATH = "tmp/screenByPrice.csv";
	public static void main(String[] args) {
		logger.info("START");
		try {
			Class.forName("org.sqlite.JDBC");
			try (Connection connection = DriverManager.getConnection("jdbc:sqlite:tmp/sqlite/securities.sqlite3");
				BufferedWriter bw = new BufferedWriter(new FileWriter(OUTPUT_PATH))) {
				calculate(connection, bw);
			}
		} catch (ClassNotFoundException | SQLException | IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		logger.info("STOP");
	}
}
