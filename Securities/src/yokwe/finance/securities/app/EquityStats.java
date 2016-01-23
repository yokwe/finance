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

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.database.CompanyTable;
import yokwe.finance.securities.database.DividendTable;
import yokwe.finance.securities.database.NasdaqTable;
import yokwe.finance.securities.database.PriceTable;
import yokwe.finance.securities.stats.DoubleArray;
import yokwe.finance.securities.stats.MA;
import yokwe.finance.securities.stats.Portfolio;
import yokwe.finance.securities.stats.RSI;
import yokwe.finance.securities.stats.UniStats;

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

		w.write("symbol,name,price,sd,div,freq,changepct,count,hv,change,var95,var99,rsi,ma200\n");
		for(String symbol: candidateList) {
			double priceArray[] = PriceTable.getAllBySymbolDateRange(connection, symbol, dateFrom, dateTo).stream().mapToDouble(o -> o.close).toArray();
			double divArray[]   = DividendTable.getAllBySymbolDateRange(connection, symbol, dateFrom, dateTo).stream().mapToDouble(o -> o.dividend).toArray();
			
			double logReturn[]  = DoubleArray.logReturn(priceArray);

			// Skip symbol that has small number of price data (1 month)
			if (priceArray.length <= 21) {
				logger.warn("SKIP {}", String.format("%-6s  %3d %2d", symbol, priceArray.length, divArray.length));
				continue;
			}
			//logger.info("{}", String.format("%-6s  %3d %2d", symbol, priceArray.length, divArray.length));

			UniStats uni = new UniStats(logReturn);
			MA emaHV = MA.ema();
			Arrays.stream(DoubleArray.multiply(logReturn, logReturn)).forEach(emaHV);
			RSI rsi = new RSI();
			Arrays.stream(priceArray).forEach(rsi);
			MA ma200 = MA.sma(200);
			Arrays.stream(priceArray).forEach(ma200);
			
			String name = nasdaqMap.get(symbol).name;
			if (name.contains("\"")) name = name.replace("\"", "\"\"");
			if (name.contains(","))  name = "\"" + name + "\"";
			
			double price     = priceArray[priceArray.length - 1];
			double div       = DoubleArray.sum(divArray);
			int    freq      = divArray.length;
			double changepct = priceArray[priceArray.length - 1] / priceArray[priceArray.length - 2] - 1.0;
			int    count     = priceArray.length;
			double hv        = Math.sqrt(emaHV.getValue());
			double change    = priceArray[priceArray.length - 1] - priceArray[priceArray.length - 2];
			double var95     = hv * Portfolio.CONFIDENCE_95_PERCENT;
			double var99     = hv * Portfolio.CONFIDENCE_99_PERCENT;
			
			w.write(String.format("%s,%s,%.2f,%.5f,%.2f,%d,%.4f,%d,%5f,%.5f,%.5f,%.5f,%.1f,%.3f\n", symbol, name, price, uni.sd, div, freq, changepct, count, hv, change, var95, var99, rsi.getValue() ,ma200.getValue()));
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
