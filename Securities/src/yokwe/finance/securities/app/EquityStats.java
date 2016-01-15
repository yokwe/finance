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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.database.CompanyTable;
import yokwe.finance.securities.database.DividendTable;
import yokwe.finance.securities.database.NasdaqTable;
import yokwe.finance.securities.database.PriceTable;
import yokwe.finance.securities.stats.DoubleArray;
import yokwe.finance.securities.stats.UniStats;
import yokwe.finance.securities.util.NasdaqUtil;

public class EquityStats {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(EquityStats.class);

	static void stats(Connection connection, Writer w) throws IOException {
		final LocalDate lastTradeDate;
		{
			String dateString = PriceTable.getLastTradeDate(connection);
			lastTradeDate = LocalDate.parse(dateString);
		}
		logger.info("lastTradeDate = {}", lastTradeDate);
		
		Map<String, NasdaqTable>  nasdaqMap  = NasdaqTable.getMap(connection);
		logger.info("nasdaqMap     = {}", nasdaqMap.size());
		
		Map<String, CompanyTable> companyMap = CompanyTable.getMap(connection);
		logger.info("companyMap    = {}", companyMap.size());
		
		// candidateList has all
		List<String> candidateList = nasdaqMap.keySet().stream().collect(Collectors.toList());
		
		// Build priceMap
		Map<String, Double> priceMap = new TreeMap<>();
		PriceTable.getAllByDate(connection, lastTradeDate).stream().forEach(o -> priceMap.put(o.symbol, o.close));

		// Filter out securities that has no price in lastTradedDate
		{
			List<String> newCandidateList = new ArrayList<>();
			
			for(String symbol: candidateList) {
				if (priceMap.containsKey(symbol)) {
					newCandidateList.add(symbol);
				}
			}
			candidateList = newCandidateList;
		}
		logger.info("candidateList = {}", candidateList.size());
		
		LocalDate dateTo   = lastTradeDate;
		LocalDate dateFrom = dateTo.minusYears(1);

		// symbol, price, sd, div, freq, changepct, name
		w.write("symbol,price,sd,div,freq,changepct,name\n");
		for(String symbol: candidateList) {
			double priceArray[] = PriceTable.getAllBySymbolDateRange(connection, symbol, dateFrom, dateTo).stream().mapToDouble(o -> o.close).toArray();
			double divArray[]   = DividendTable.getAllBySymbolDateRange(connection, symbol, dateFrom, dateTo).stream().mapToDouble(o -> o.dividend).toArray();
			
			// Skip symbol that has small number of price data (1 month)
			if (priceArray.length <= 21) {
				logger.warn("SKIP {}", String.format("%-6s  %3d %2d", symbol, priceArray.length, divArray.length));
				continue;
			}
			//logger.info("{}", String.format("%-6s  %3d %2d", symbol, priceArray.length, divArray.length));
			
			String name = NasdaqUtil.get(symbol).name;
			if (name.contains("\"")) name = name.replace("\"", "\"\"");
			if (name.contains(","))  name = "\"" + name + "\"";
			
			double price     = priceArray[priceArray.length - 1];
			double sd        = new UniStats(DoubleArray.logReturn(priceArray)).sd;
			double div       = DoubleArray.sum(divArray);
			int    freq      = divArray.length;
			double changepct = priceArray[priceArray.length - 1] / priceArray[priceArray.length - 2];

			w.write(String.format("%s,%.2f,%.5f,%.2f,%d,%.2f,%s\n", symbol, price, sd, div, freq, changepct, name));
		}
	}
	
	public static void main(String[] args) {
		logger.info("START");
//		String outputPath = "tmp/equityStats.csv";
		String outputPath = args[0];
		logger.info("outputPath    = {}", outputPath);
		
		try {
			Class.forName("org.sqlite.JDBC");
			try (Connection connection = DriverManager.getConnection("jdbc:sqlite:tmp/sqlite/securities.sqlite3");
				BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath))) {
				stats(connection, bw);
			}
		} catch (ClassNotFoundException | SQLException | IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		logger.info("STOP");
	}
}
