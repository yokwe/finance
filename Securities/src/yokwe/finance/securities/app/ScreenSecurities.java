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
		
		{
			List<String> newCandidateList = new ArrayList<>();
			for(String symbol: candidateList) {
				List<DividendTable> dividendList = DividendTable.getAllBySymbol(connection, symbol);
				
				Map<String, Integer> yearMap = new TreeMap<>();
				for(DividendTable table: dividendList) {
					String year = table.date.substring(0, 4);
					if (yearMap.containsKey(year)) {
						Integer count = yearMap.get(year);
						yearMap.put(year, count + 1);
					} else {
						yearMap.put(year, 1);
					}
				}
				
				// TODO create more accurate countList
				//   first create from all data
				//   find common number of data count
				//   omit first and last year data count if that is not equals to common data count.
				int[] countList = yearMap.values().stream().mapToInt(o -> o).toArray();
				if (countList.length <= 1) continue;
				
				int notEqualCount = 0;
				int firstCount = countList[0];
				for(int count : countList) {
					if (count != firstCount) notEqualCount++;
				}
				if (2 < notEqualCount) continue;
				
				newCandidateList.add(symbol);
				logger.info("{}", String.format("%-6s  %4d  %4d", symbol, dividendList.size(), countList.length));
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
