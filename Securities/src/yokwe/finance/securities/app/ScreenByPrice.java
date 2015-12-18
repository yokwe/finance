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
import yokwe.finance.securities.database.CompanyTable;
import yokwe.finance.securities.database.NasdaqTable;
import yokwe.finance.securities.database.PriceTable;
import yokwe.finance.securities.stats.Data;
import yokwe.finance.securities.stats.DoubleArray;
import yokwe.finance.securities.stats.UniStats;

public class ScreenByPrice {
	private static final Logger logger = LoggerFactory.getLogger(ScreenByPrice.class);
		
	public static LocalDate getTradingDate(Connection connection, final LocalDate date) {
		LocalDate tradingDate = date.minusDays(0);
		if (PriceTable.isTradingDay(connection, tradingDate)) return tradingDate;
		tradingDate = tradingDate.minusDays(1);
		if (PriceTable.isTradingDay(connection, tradingDate)) return tradingDate;
		tradingDate = tradingDate.minusDays(1);
		if (PriceTable.isTradingDay(connection, tradingDate)) return tradingDate;
		tradingDate = tradingDate.minusDays(1);
		if (PriceTable.isTradingDay(connection, tradingDate)) return tradingDate;
		
		logger.error("date = {}  tradingDate = {}", date, tradingDate);
		throw new SecuritiesException("tradingDate");
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
		@Override
		public String toString() {
			return String.format("{%s %s %s}", name, date, map);
		}
	}
	// Remove out of bound value from values data
	static void calculate(Connection connection, Writer w) throws IOException {
		Map<String, NasdaqTable> nasdaqMap = NasdaqTable.getMap(connection);
		logger.info("nasdaqMap     = {}", nasdaqMap.size());
		
		Map<String, CompanyTable> companyMap = CompanyTable.getMap(connection);
		logger.info("companyMap    = {}", companyMap.size());

		// symbolList has all symbols
		List<String> symbolList = nasdaqMap.keySet().stream().collect(Collectors.toList());
		logger.info("symbolList    = {}", symbolList.size());

		LocalDate origin = LocalDate.parse(PriceTable.getLastTradeDate(connection));
		logger.info("origin        = {}", origin);

		PriceMap originMap = new PriceMap(connection, "origin", origin);
		logger.info("originMap     = {}", originMap.map.size());

		Map<String, PriceMap> priceMapMap = new TreeMap<>();
		priceMapMap.put("A 180", new PriceMap(connection, "A 180", origin.minusDays(180)));
		priceMapMap.put("B 150", new PriceMap(connection, "B 150", origin.minusDays(150)));
		priceMapMap.put("C 120", new PriceMap(connection, "C 120", origin.minusDays(120)));
		priceMapMap.put("D  90", new PriceMap(connection, "D  90", origin.minusDays(90)));
		priceMapMap.put("E  60", new PriceMap(connection, "E  60", origin.minusDays(60)));
		priceMapMap.put("F  30", new PriceMap(connection, "F  30", origin.minusDays(30)));
		
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
					ratio = String.format("%.2f", (originClose / targetClose) * 100.0);
//					ratio = String.format("%.2f", (originClose - targetClose) / targetClose);
				} else {
					ratio = "";
				}
				
				if (!ratioMap.containsKey(symbol)) ratioMap.put(symbol, new TreeMap<>());
				Map<String, String> map = ratioMap.get(symbol);
				map.put(name, ratio);
			}
		}
		
		// Build sdMap
		Map<String, String> sdMap = new TreeMap<>();
		{
			LocalDate dateTo   = origin;
			LocalDate dateFrom = dateTo.minusYears(1);
			
			for(String symbol: ratioMap.keySet()) {
				Data data = new Data(PriceTable.getAllBySymbolDateRange(connection, symbol, dateFrom, dateTo));
				UniStats stats = new UniStats(DoubleArray.logReturn(data.toDoubleArray(symbol)));
				double valueAtRisk = stats.sd;
				sdMap.put(symbol, String.format("%.4f", valueAtRisk));
			}
		}
		
		// Output header
		w.append("symbol");
		w.append(",sector");
		w.append(",industry");
		w.append(",name");
		w.append(",sd");
		for(String name: priceMapMap.keySet()) {
			w.append(",").append(name);
		}
		w.append(",price");
		for(String name: priceMapMap.keySet()) {
			w.append(",").append(name);
		}
		w.append("\n");
		
		// Output data of each symbol
		for(String symbol: ratioMap.keySet()) {
			w.append(symbol);
			
			{
				CompanyTable company = companyMap.get(symbol);
				if (company == null) {
					logger.error("Unknown symbol = {}", symbol);
					throw new SecuritiesException("Unknown symbol");
				}
				String sector   = company.sector;
				if (sector.contains("\"")) sector = sector.replace("\"", "\"\"");
				if (sector.contains(","))  sector = "\"" + sector + "\"";

				String industry = company.industry;
				if (industry.contains("\"")) industry = industry.replace("\"", "\"\"");
				if (industry.contains(","))  industry = "\"" + industry + "\"";
				
				w.append(",").append(sector);
				w.append(",").append(industry);
			}
			
			{
				String name = nasdaqMap.get(symbol).name;
				if (name.contains("\"")) name = name.replace("\"", "\"\"");
				if (name.contains(","))  name = "\"" + name + "\"";
				
				w.append(",").append(name);
			}
			w.append(",").append(sdMap.get(symbol));
			
			Map<String, String> map = ratioMap.get(symbol);
			if (map == null) {
				logger.error("null  {}  {}", symbol, map);
				throw new SecuritiesException("null");
			}

			for(String name: priceMapMap.keySet()) {
				final PriceMap priceMap = priceMapMap.get(name);
				final PriceTable priceTable = priceMap.map.get(symbol);
				final String value = (priceTable != null) ? String.format("%.2f", priceTable.close) : "";
				w.append(",").append(value);
			}
			
			w.append(",").append(String.format("%.2f", originMap.map.get(symbol).close));

			for(String name: priceMapMap.keySet()) {
				w.append(",").append(map.get(name));
			}
						
			w.append("\n");
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
