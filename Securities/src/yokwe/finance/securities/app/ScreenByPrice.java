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

	static boolean isTradingDay(Connection connection, LocalDate date) {
		PriceTable priceTable = PriceTable.getBySymbolDate(connection, "NYT", date);
		return priceTable != null;
	}
	
	static Map<String, PriceTable> getPriceMap(Connection connection, LocalDate date) {
		Map<String, PriceTable> ret = new TreeMap<>();
		PriceTable.getAllByDate(connection, date).stream().forEach(o -> ret.put(o.symbol, o));
		return ret;
	}
	
	static class PriceMap {
		final String                  name;
		final LocalDate               date;
		final Map<String, PriceTable> map;
		PriceMap(Connection connection, String name, LocalDate date) {
			this.name = name;
			this.date = getTradingDate(connection, date);
			this.map  = new TreeMap<>();
			List<PriceTable> result = PriceTable.getAllByDate(connection, this.date);
			if (result == null) {
				logger.error("no data  {}", this.date);
				throw new SecuritiesException("no data");
			} else {
				result.stream().forEach(o -> map.put(o.symbol, o));
			}
		}
	}
	// Remove out of bound value from values data
	static void calculate(Connection connection, Writer w) throws IOException {
		Map<String, NasdaqTable> nasdaqMap = NasdaqTable.getMap(connection);
		logger.info("nasdaqMap     = {}", nasdaqMap.size());

		// symbolList has all symbols
		List<String> symbolList = nasdaqMap.keySet().stream().collect(Collectors.toList());
		logger.info("symbolList    = {}", symbolList.size());

		LocalDate origin = LocalDate.parse(PriceTable.getLastTradeDate(connection));
		logger.info("origin        = {}", origin);

		PriceMap originMap = new PriceMap(connection, "origin", origin);
		logger.info("originMap     = {}", originMap.map.size());

		Map<String, PriceMap> priceMapMap = new TreeMap<>();
		priceMapMap.put("A 365", new PriceMap(connection, "A 365", origin.minusDays(365)));
		priceMapMap.put("B  90", new PriceMap(connection, "B  90", origin.minusDays(90)));
		priceMapMap.put("C  30", new PriceMap(connection, "C  30", origin.minusDays(30)));
		priceMapMap.put("D  10", new PriceMap(connection, "D  10", origin.minusDays(10)));
		
		// Calculate price change of each period
		//  symbol      name    ratio to origin
		Map<String, Map<String, String>> ratioMap = new TreeMap<>();
		for(PriceMap targetMap: priceMapMap.values()) {
			final String name = targetMap.name;
			for(String symbol: originMap.map.keySet()) {
				final String ratio;
				if (targetMap.map.containsKey(symbol)) {
					final double originClose = originMap.map.get(symbol).close;
					final double targetClose = targetMap.map.get(symbol).close;
					ratio = String.format("%.2f", (originClose - targetClose) / originClose);
				} else {
					ratio = "";
				}
				
				if (!ratioMap.containsKey(symbol)) ratioMap.put(symbol, new TreeMap<>());
				Map<String, String> map = ratioMap.get(symbol);
				map.put(name, ratio);
			}
		}
				
		// Output header
		w.append("symbol");
		w.append(",price");
		for(String name: priceMapMap.keySet()) {
			w.append(",").append(name);
		}
		w.append(",name\n");
		
		// Output data of each symbol
		for(String symbol: ratioMap.keySet()) {
			w.append(symbol);
			w.append(",").append(String.format("%.2f", originMap.map.get(symbol).close));
			for(String name: priceMapMap.keySet()) {
				Map<String, String> map = ratioMap.get(name);
				if (map == null) {
					logger.error("null  {}  {}", symbol, name);
					throw new SecuritiesException("null");
				}
				for(String ratio: map.values()) {
					w.append(",").append(ratio);
				}
			}
			
			String name = nasdaqMap.get(symbol).name;
			if (name.contains("\"")) name = name.replace("\"", "\"\"");
			if (name.contains(","))  name = "\"" + name + "\"";
			
			w.append(",").append(name).append("\n");
		}
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
