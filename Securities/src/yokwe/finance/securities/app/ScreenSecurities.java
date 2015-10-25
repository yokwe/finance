package yokwe.finance.securities.app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.database.DividendTable;
import yokwe.finance.securities.database.FinanceTable;
import yokwe.finance.securities.database.NasdaqTable;
import yokwe.finance.securities.database.PriceTable;

public class ScreenSecurities {
	private static final Logger logger = LoggerFactory.getLogger(ScreenSecurities.class);
	
	static void calculate(Connection connection) {
		final String lastTradeDate = PriceTable.getLastTradeDate(connection);
		logger.info("lastTradeDate = {}", lastTradeDate);
		
		Map<String, Double> priceMap = new TreeMap<>();
		PriceTable.getAllByDate(connection, lastTradeDate).stream().forEach(o -> priceMap.put(o.symbol, o.close));
		
		Map<String, NasdaqTable>  nasdaqMap  = NasdaqTable.getMap(connection);
		Map<String, FinanceTable> financeMap = FinanceTable.getMap(connection);
		
		logger.info("nasdaqMap     = {}", nasdaqMap.size());
		logger.info("financeMap    = {}", financeMap.size());
		
		List<String> candidateList = financeMap.keySet().stream().collect(Collectors.toList());
		logger.info("candidateList = {}", candidateList.size());
		
		// Filter out the securities that has no trade history
		{
			List<String> newCandidateList = new ArrayList<>();
			for(String symbol: candidateList) {
				final FinanceTable finance = financeMap.get(symbol);
				if (finance.vol <= 0 && finance.avg_vol <= 0) continue;
				
				newCandidateList.add(symbol);
			}
			candidateList = newCandidateList;
		}
		logger.info("candidateList = {}", candidateList.size());

		// Build dividendMap
		Map<String, Map<String, List<Double>>> dividendMap = new TreeMap<>();
		for(String symbol: candidateList) {
			List<DividendTable> dividendList = DividendTable.getAllBySymbol(connection, symbol);
			Map<String, List<Double>> yearMap = new TreeMap<>();
			for(DividendTable dividendTable: dividendList) {
				String year = dividendTable.date.substring(0, 4);
				if (!yearMap.containsKey(year)) yearMap.put(year, new ArrayList<>());
				yearMap.get(year).add(dividendTable.dividend);
			}
			dividendMap.put(symbol,  yearMap);
		}

		// Filter out the securities that has irregular dividend
		{
			List<String> newCandidateList = new ArrayList<>();
			
			for(String symbol: candidateList) {
				Map<String, List<Double>> yearMap = dividendMap.get(symbol);
				
				int[] countList = yearMap.values().stream().mapToInt(o -> o.size()).toArray();				
				if (countList.length <= 3) continue;
				int commonCount = countList[1]; // Should be count in common
				int notEqualCount = 0;
				// Skip first and last year
				for(int i = 1; i < (countList.length - 1); i++) {
					if (countList[i] != commonCount) notEqualCount++;
				}
				if (0 < notEqualCount) continue;
				
				newCandidateList.add(symbol);
//				logger.info("{}", String.format("%-6s  %4d  %2d", symbol, commonCount, yearMap.size()));
			}
			candidateList = newCandidateList;
		}
		logger.info("candidateList = {}", candidateList.size());
		
		// Calculate dividend of last year and this year
		{
			final int thisYearNumber = LocalDate.now().getYear();
			final String thisYear = String.format("%d", thisYearNumber);
			final String lastYear = String.format("%d", thisYearNumber - 1);
			
			for(String symbol: candidateList) {
				Map<String, List<Double>> yearMap = dividendMap.get(symbol);
				if (!yearMap.containsKey(thisYear)) continue;
				if (!yearMap.containsKey(lastYear)) continue;
				
				// TODO google-getprices return no data for some symbol (AMT)
				// TODO create symbol list that should take data from yahoo-daily. Because google-getprices does'nt have data.
				// TODO find symbol that has dividend but doesn't have daily quote. try yahoo-daily to get price not google-getprices.
				
				if (!priceMap.containsKey(symbol)) {
					logger.info("XXXX {} {}", symbol, yearMap.get(thisYear));
					continue;
				}
				
				final double priceRatio = 1000.0 / priceMap.get(symbol);
				
				if (yearMap.containsKey(thisYear) && yearMap.containsKey(lastYear)) {
					double[] lastYearArray = yearMap.get(lastYear).stream().mapToDouble(o -> o).toArray();
					double[] thisYearArray = yearMap.get(thisYear).stream().mapToDouble(o -> o).toArray();
					
					double dividendThisYear = 0;				
					for(int i = 0; i < thisYearArray.length; i++) dividendThisYear += thisYearArray[i];
					for(int i = thisYearArray.length; i < lastYearArray.length; i++) dividendThisYear += lastYearArray[i];

					double dividendLastYear = 0;
					for(int i = 0; i < lastYearArray.length; i++) dividendLastYear += lastYearArray[i];

	//				logger.info("{}", String.format("%-6s  %8.3f  %8.3f", symbol, dividendLastYear * priceRatio, dividendThisYear * priceRatio));
				}
			}
		}
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
