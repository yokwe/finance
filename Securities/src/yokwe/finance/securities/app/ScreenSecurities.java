package yokwe.finance.securities.app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
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

public class ScreenSecurities {
	private static final Logger logger = LoggerFactory.getLogger(ScreenSecurities.class);
	
	static void calculate(Connection connection) {
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
				logger.info("{}", String.format("%-6s  %4d  %2d", symbol, commonCount, yearMap.size()));
			}
			candidateList = newCandidateList;
		}
		
		
		logger.info("candidateList = {}", candidateList.size());
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
