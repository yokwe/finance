package yokwe.finance.securities.app;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
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
import yokwe.finance.securities.util.NasdaqUtil;

public class ScreenSecurities {
	private static final Logger logger = LoggerFactory.getLogger(ScreenSecurities.class);
	
	static void calculate(Connection connection, Writer w) throws IOException {
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
			final int y0Number = LocalDate.now().getYear();
			final String y0 = String.format("%d", y0Number);
			final String y1 = String.format("%d", y0Number - 1);
			final String y2 = String.format("%d", y0Number - 2);
			final String y3 = String.format("%d", y0Number - 3);
			final String y4 = String.format("%d", y0Number - 4);
			final String y5 = String.format("%d", y0Number - 5);
			
			int count = 0;
			for(String symbol: candidateList) {
				Map<String, List<Double>> yearMap = dividendMap.get(symbol);
				if (!yearMap.containsKey(y0)) continue;
				if (!yearMap.containsKey(y1)) continue;
				if (!yearMap.containsKey(y2)) continue;
				if (!yearMap.containsKey(y3)) continue;
				if (!yearMap.containsKey(y4)) continue;
				if (!yearMap.containsKey(y5)) continue;
				
				NasdaqTable nasdaqTable = NasdaqUtil.get(symbol);
				
				// TODO google-getprices return no data for some symbol (AMT)
				// TODO create symbol list that should take data from yahoo-daily. Because google-getprices does'nt have data.
				// TODO find symbol that has dividend but doesn't have daily quote. try yahoo-daily to get price not google-getprices.
				
				if (!priceMap.containsKey(symbol)) {
//					logger.info("XXXX {} {}", symbol, yearMap.get(y0));
					continue;
				}
				
				final double priceRatio = 1000.0 / priceMap.get(symbol);
				
				double[] y0Array = yearMap.get(y0).stream().mapToDouble(o -> o).toArray();
				double[] y1Array = yearMap.get(y1).stream().mapToDouble(o -> o).toArray();
				
				double y0Profit = Arrays.stream(y0Array).sum();
				for(int i = y0Array.length; i < y1Array.length; i++) y0Profit += y1Array[i];
				y0Profit *= priceRatio;

				double y1Profit = priceRatio * Arrays.stream(y1Array).sum();
				double y2Profit = priceRatio * yearMap.get(y2).stream().mapToDouble(o -> o).sum();
				double y3Profit = priceRatio * yearMap.get(y3).stream().mapToDouble(o -> o).sum();
				double y4Profit = priceRatio * yearMap.get(y4).stream().mapToDouble(o -> o).sum();
				double y5Profit = priceRatio * yearMap.get(y5).stream().mapToDouble(o -> o).sum();

//				logger.info("{}", String.format("%s %2d %-6s  %8.3f  %8.3f  %8.3f  %8.3f  %8.3f  %8.3f  %s",
//					nasdaqTable.etf, y1Array.length, symbol, y5Profit, y4Profit, y3Profit, y2Profit, y1Profit, y0Profit, nasdaqTable.name));

				String name = nasdaqTable.name;
				if (name.contains("\"")) name = name.replace("\"", "\"\"");
				if (name.contains(",")) name = "\"" + name + "\"";
				w.append(String.format("%s,%s,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%s\n", nasdaqTable.etf, y1Array.length, y5Profit, y4Profit, y3Profit, y2Profit, y1Profit, y0Profit, name));
				count++;
			}
			
			logger.info("count = {}", count);
		}
	}
	
	private static final String OUTPUT_PATH = "tmp/screen.csv";
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
