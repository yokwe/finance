package yokwe.finance.securities.app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.database.DividendTable;
import yokwe.finance.securities.database.PriceTable;

public class CheckEmptyPrice {
	private static final Logger logger = LoggerFactory.getLogger(CheckEmptyPrice.class);
	
	static void checkPrice(Connection connection, String year, Set<String> symbolSet) {
		String likeString = year + "%";
		Map<String, Integer> dividendCountMap = DividendTable.getSymbolCount(connection, likeString);
		Map<String, Integer> priceCountMap    = PriceTable.getSymbolCount(connection, likeString);
		
		for(String symbol: dividendCountMap.keySet()) {
			if (priceCountMap.containsKey(symbol)) {
				int countPrice = priceCountMap.get(symbol);
				if (0 < countPrice) continue;
			}
			logger.info("{} {}", year, symbol);
			symbolSet.add(symbol);
		}
	}
	
	static void calculate(Connection connection) {
		final String lastTradeDate = PriceTable.getLastTradeDate(connection);
		logger.info("lastTradeDate = {}", lastTradeDate);
		
		final int thisYearNumber = LocalDate.now().getYear();
		
		Set<String> symbolSet = new TreeSet<>();
		
		for(int i = 0; i < 5; i++) {
			final String yearString = String.format("%d", thisYearNumber - i);
			checkPrice(connection, yearString, symbolSet);
			logger.info("{}  symbolSet = {}", yearString, symbolSet.size());			
		}
		
//		for(String symbol: symbolSet) {
//			logger.info("{}", symbol);
//		}
	}
	
	public static void main(String[] args) {
		logger.info("START");
		try {
			Class.forName("org.sqlite.JDBC");
			try (Connection connection = DriverManager.getConnection("jdbc:sqlite:tmp/sqlite/securities.sqlite3")) {
				calculate(connection);
			}
		} catch (ClassNotFoundException | SQLException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		logger.info("STOP");
	}
}
