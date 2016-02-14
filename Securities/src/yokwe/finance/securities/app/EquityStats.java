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
import yokwe.finance.securities.stats.FinStats;
import yokwe.finance.securities.stats.HV;
import yokwe.finance.securities.stats.MA;
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
		
		final UniStats market;
		{
			double priceArray[] = PriceTable.getAllBySymbolDateRange(connection, "SPY", dateFrom, dateTo).stream().mapToDouble(o -> o.close).toArray();
			double logReturn[]  = DoubleArray.logReturn(priceArray);
			market = new UniStats(logReturn);
		}


		w.write("symbol,name,price,sd,div,freq,changepct,count,hv,change,var95,var99,rsi,price200,divYield,beta,r2,vol5\n");
		for(String symbol: candidateList) {
			List<PriceTable> priceTable = PriceTable.getAllBySymbolDateRange(connection, symbol, dateFrom, dateTo);
			double priceArray[]  = priceTable.stream().mapToDouble(o -> o.close).toArray();
			double volumeArray[] = priceTable.stream().mapToDouble(o -> o.volume).toArray();
			double divArray[]    = DividendTable.getAllBySymbolDateRange(connection, symbol, dateFrom, dateTo).stream().mapToDouble(o -> o.dividend).toArray();
			
			double logReturn[]  = DoubleArray.logReturn(priceArray);

			// Skip symbol that has small number of price data (1 month)
			if (priceArray.length <= 21) {
				logger.warn("SKIP {}", String.format("%-6s  %3d %2d", symbol, priceArray.length, divArray.length));
				continue;
			}
			//logger.info("{}", String.format("%-6s  %3d %2d", symbol, priceArray.length, divArray.length));

			UniStats stock = new UniStats(logReturn);			
			HV hv = new HV();
			Arrays.stream(priceArray).forEach(hv);
			RSI rsi = new RSI();
			Arrays.stream(priceArray).forEach(rsi);
			MA price200 = MA.sma(200);
			Arrays.stream(priceArray).forEach(price200);
			MA vol5 = MA.sma(5);
			Arrays.stream(volumeArray).forEach(vol5);
			
			String name = nasdaqMap.get(symbol).name;
			if (name.contains("\"")) name = name.replace("\"", "\"\"");
			if (name.contains(","))  name = "\"" + name + "\"";
			
			double lastPrice = priceArray[priceArray.length - 2];
			double price     = priceArray[priceArray.length - 1];
			double changepct = price / lastPrice - 1.0;
			double change    = price - lastPrice;
			double div       = DoubleArray.sum(divArray);
			int    freq      = divArray.length;
			int    count     = priceArray.length;
			double divYield  = div / price;
			
			double beta;
			double r2;
			{
				if (market.size == stock.size) {
					FinStats finStats = new FinStats(market, stock, 0.0);
					beta = finStats.beta;
					r2   = finStats.r2;
				} else {
					beta = 0;
					r2   = 0;
				}
			}
			
			w.write(
				String.format("%s,%s,%.2f,%.5f,%.2f,%d,%.4f,%d,%5f,%.5f,%.5f,%.5f,%.1f,%.3f,%.3f,%.3f,%.3f,%d\n",
					symbol, name, price, stock.sd, div, freq, changepct, count, hv.getValue(), change, hv.getVaR95(1), hv.getVaR99(1),
					rsi.getValue(), price200.getValue(), divYield, beta, r2, Math.round(vol5.getValue())));
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
