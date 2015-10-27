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
import java.util.OptionalDouble;
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
		
		final int LAST_N_YEARS = 5;  // Y4 Y3 Y2 Y1 Y0
		
		// Calculate dividend of last year and this year
		{
			final int y0Number = LocalDate.now().getYear();
			final String y0 = String.format("%d", y0Number);
			final String y1 = String.format("%d", y0Number - 1);
			
			// Output title line
			w.append("etf,freq,price,symbol");
			for(int i = LAST_N_YEARS - 1; 0 <= i; i--) w.append(String.format(",y%d", i));
			for(int i = LAST_N_YEARS - 1; 0 <= i; i--) w.append(String.format(",a%d", i));
			w.append(",name\n");

			int count = 0;
			for(String symbol: candidateList) {
				if (!priceMap.containsKey(symbol)) continue;
				
				final double price = priceMap.get(symbol);
				final double priceRatio = 1000.0 / price;

				Map<String, List<Double>> yearMap = dividendMap.get(symbol);
				if (!yearMap.containsKey(y0)) continue;
				if (!yearMap.containsKey(y1)) continue;
				
				final String name;
				final String etf;
				final int    freq;
				{
					NasdaqTable nasdaqTable = NasdaqUtil.get(symbol);
					etf  = nasdaqTable.etf;
					freq = yearMap.get(y1).size();
					
					String nasdaqName = nasdaqTable.name;
					if (nasdaqName.contains("\"")) nasdaqName = nasdaqName.replace("\"", "\"\"");
					if (nasdaqName.contains(",")) nasdaqName = "\"" + nasdaqName + "\"";
					name = nasdaqName;
				}
				
				double[] average = new double[LAST_N_YEARS];
				double[] profit  = new double[LAST_N_YEARS];
				for(int i = 0; i < profit.length; i++) profit[i] = average[i] = -1;
				
				// special for Y0
				{
					double[] y0Array = yearMap.get(y0).stream().mapToDouble(o -> o).toArray();
					double[] y1Array = yearMap.get(y1).stream().mapToDouble(o -> o).toArray();
					
					double y0Profit = Arrays.stream(y0Array).sum();
					for(int i = y0Array.length; i < y1Array.length; i++) y0Profit += y1Array[i];
					y0Profit *= priceRatio;
					profit[0] = y0Profit;
				}
				
				for(int i = 1; i < LAST_N_YEARS; i++) {
					String yyyy = String.format("%d", y0Number - i);
					if (yearMap.containsKey(yyyy)) {
						profit[i] = priceRatio * yearMap.get(yyyy).stream().mapToDouble(o -> o).sum();
					}
				}
				
				for(int i = 0; i < LAST_N_YEARS; i++) {
					String yyyy = String.format("%d", y0Number - i);
					List<PriceTable> result = PriceTable.getAllBySymbolDateLike(connection, symbol, yyyy + "%");
					if (result.size() == 0) {
						average[i] = -1;
					} else {
						OptionalDouble od = result.stream().mapToDouble(o -> o.close).average();
						average[i] = od.getAsDouble();
					}
				}

				w.append(String.format("%s,%d,%.2f,%s", etf, freq, price, symbol));
				for(int i = LAST_N_YEARS - 1; 0 <= i; i--) {
					final double p = profit[i];
					w.append((0 <= p) ? String.format(",%.3f", p) : ",");
				}
				for(int i = LAST_N_YEARS - 1; 0 <= i; i--) {
					final double a = average[i];
					w.append((0 <= a) ? String.format(",%.3f", a) : ",");
				}
				w.append(String.format(",%s\n", name));
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
