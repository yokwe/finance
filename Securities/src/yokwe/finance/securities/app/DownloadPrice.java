package yokwe.finance.securities.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.database.NasdaqTable;
import yokwe.finance.securities.database.PriceTable;
import yokwe.finance.securities.update.GoogleHistorical;
import yokwe.finance.securities.update.YahooDaily;
import yokwe.finance.securities.util.HttpUtil;

public class DownloadPrice {
	private static final Logger logger = LoggerFactory.getLogger(DownloadPrice.class);
	
	// Use yahoo-table
	static Map<String, PriceTable> getFromYahoo(final String exch, final String symbol, final String yahooSymbol, final LocalDate dateFrom, final LocalDate dateTo) {
		// http://real-chart.finance.yahoo.com/table.csv?s=SPY&a=00&b=01&c=2015&d=12&e=30&f=2015&ignore=.csv
		final String s = yahooSymbol;
		final int a = dateFrom.getMonthValue() - 1;
		final int b = dateFrom.getDayOfMonth();
		final int c = dateFrom.getYear();
		final int d = dateTo.getMonthValue() - 1;
		final int e = dateTo.getDayOfMonth();
		final int f = dateTo.getYear();
		
		final String url = String.format("http://real-chart.finance.yahoo.com/table.csv?s=%s&a=%02d&b=%02d&c=%d&d=%02d&e=%02d&f=%d&g=d&ignore=.csv", s, a, b, c, d, e, f);
		final List<String> lines = HttpUtil.download(url);
		
		final Map<String, PriceTable> ret = new HashMap<>();
		if (lines == null) return ret;
		
		int count = 0;
		for(String line: lines) {
			if (count++ == 0) {
				YahooDaily.CSVRecord.checkHeader(line);
			} else {
				final PriceTable priceTable = YahooDaily.CSVRecord.toPriceTable(symbol, line);
				ret.put(priceTable.date, priceTable);
			}
		}
		return ret;
	}
	
	static Map<String, PriceTable> getFromGoogle(final String exch, final String symbol, final String googleSymbol, final LocalDate dateFrom, final LocalDate dateTo) {
		// http://www.google.com/finance/historical?q=NYSE:IBM&startdate=Jan%201,%202000&enddate=Dec%2031,%202050&output=csv
		final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);

		final String startdate = dateFrom.format(dateFormatter).replace(" ", "%20");
		final String enddate   = dateTo.format(dateFormatter).replace(" ", "%20");
		final String url = String.format("http://www.google.com/finance/historical?q=%s:%s&startdate=%s&enddate=%s&output=csv", exch, googleSymbol, startdate, enddate);
		List<String> lines = HttpUtil.download(url);

		final Map<String, PriceTable> ret = new HashMap<>();
		if (lines == null) return ret;
		
		int count = 0;
		for(String line: lines) {
			if (count++ == 0) {
				GoogleHistorical.CSVRecord.checkHeader(line);
			} else {
				final PriceTable priceTable = GoogleHistorical.CSVRecord.toPriceTable(symbol, line);
				ret.put(priceTable.date, priceTable);
			}
		}
		return ret;
	}
	
	
	static List<String> getDateList(Connection connection, final LocalDate dateFrom, final LocalDate dateTo) {
		Map<String, PriceTable> ibmMap = getFromYahoo("NYSE", "IBM", "IBM", dateFrom, dateTo);
		Map<String, PriceTable> nytMap = getFromYahoo("NYSE", "NYT", "NYT", dateFrom, dateTo);
		Map<String, PriceTable> pepMap = getFromYahoo("NYSE", "PEP", "PEP", dateFrom, dateTo);
		
		if (ibmMap.size() != nytMap.size()) {
			logger.error("{} ibm = {}  nyt = {}", dateFrom.getYear(), ibmMap.size(), nytMap.size());
			throw new SecuritiesException("getDateList");
		}
		if (nytMap.size() != pepMap.size()) {
			logger.error("{} nyt = {}  pep = {}", dateFrom.getYear(), nytMap.size(), pepMap.size());
			throw new SecuritiesException("getDateList");
		}
		if (ibmMap.size() != pepMap.size()) {
			logger.error("{} ibm = {}  pep = {}", dateFrom.getYear(), ibmMap.size(), pepMap.size());
			throw new SecuritiesException("getDateList");
		}
		
		for(String symbol: nytMap.keySet()) {
			if (ibmMap.containsKey(symbol)) continue;
			logger.error("{} nyt missing date = {}", dateFrom.getYear(), symbol);
			throw new SecuritiesException("getDateList");
		}
		for(String symbol: pepMap.keySet()) {
			if (ibmMap.containsKey(symbol)) continue;
			logger.error("{} pep missing date = {}", dateFrom.getYear(), symbol);
			throw new SecuritiesException("getDateList");
		}
		
		List<String> ret = new ArrayList<>(ibmMap.keySet());
		Collections.sort(ret);
		return ret;
	}
	
	static void check(Connection connection, final BufferedWriter wr, final Map<String, NasdaqTable>  nasdaqMap, final LocalDate dateFrom, final LocalDate dateTo) throws IOException {
		final String saveFilePath = String.format("tmp/database/price-%d.csv", dateFrom.getYear());
		final File saveFile = new File(saveFilePath);
		if (saveFile.isFile()) {
			logger.info("# skip {}", saveFilePath);
			return;
		}
		
		List<PriceTable> data = PriceTable.getAllByDateRange(connection, dateFrom, dateTo);
		logger.info("data {}  {}  {}", dateFrom, dateTo, data.size());
		wr.append(String.format("# data  %s  %s  %d\n", dateFrom, dateTo, data.size()));

		List<String> dateList = getDateList(connection, dateFrom, dateTo);
		Collections.sort(dateList);
		
		List<PriceTable> goodData   = new ArrayList<>();
		List<String>     badSymbols = new ArrayList<>();
		for(String symbol: nasdaqMap.keySet()) {
			final List<PriceTable> data2 = data.stream().filter(o -> o.symbol.equals(symbol)).collect(Collectors.toList());
			
			// Skip symbol that has no data
			if (data2.size() == 0) continue;
			
			logger.info("XXX {}", symbol);
			
			// create map
			Map<String, PriceTable> map = new HashMap<>();
			{
				boolean foundError = false;
				for(PriceTable table: data2) {
					if (map.containsKey(table.date)) {
						// duplicate date
						logger.info("dup      {} {}", table.date, symbol);
						badSymbols.add(symbol);
						foundError = true;
						break;
					}
					map.put(table.date, table);
				}
				// skip to next symbol if found error
				if (foundError) continue;
			}
			
			final String dateFirst2 = data2.get(0).date;
			final List<String> modifiedDateList = new ArrayList<>();
			for(String date: dateList) {
				// Skip data before dateFirst2 -- before inception date
				if (date.compareTo(dateFirst2) < 0) continue;
				modifiedDateList.add(date);
			}
			
			{
				boolean foundError = false;
				for(String date: modifiedDateList) {
					if (!map.containsKey(date)) {
						// missing date
//						logger.info("missing  {} {}", date, symbol);
						badSymbols.add(symbol);
						foundError = true;
						break;
					}
				}
				// skip to next symbol if found error
				if (foundError) continue;
			}

			// build goodData to remove surplus date
			for(String date: modifiedDateList) {
				goodData.add(map.get(date));
			}
		}
		if (0 < badSymbols.size()) logger.info("badSymbols     {} {}", badSymbols.size(), badSymbols);

		List<String> missingSymbols = new ArrayList<>();
		for(String symbol: badSymbols) {
			final NasdaqTable nasdaq = nasdaqMap.get(symbol);
			
			logger.info("YYY {}", symbol);

			final Map<String, PriceTable> yahooMap  = getFromYahoo(nasdaq.exchange, symbol, nasdaq.yahoo, dateFrom, dateTo);
			final Map<String, PriceTable> googleMap = getFromGoogle(nasdaq.exchange, symbol, nasdaq.google, dateFrom, dateTo);

			final List<String>     missingDate = new ArrayList<>();
			final List<PriceTable> myGoodData  = new ArrayList<>();
			for(String date: dateList) {
				if (yahooMap.containsKey(date)) {
					myGoodData.add(yahooMap.get(date));
				} else {
					if (googleMap.containsKey(date)) {
						myGoodData.add(googleMap.get(date));
					} else {
						missingDate.add(date);
					}
				}
			}
			goodData.addAll(myGoodData);
			if (0 < missingDate.size()) {
				// repair failed with yahoo and google (some data is still missing)
				missingSymbols.add(symbol);
			}
		}
		
		if (missingSymbols.size() == 0) {
			// Save content of goodData as saveFile
			logger.info("# save {}", saveFilePath);
			saveFile.createNewFile();
			try (BufferedWriter save = new BufferedWriter(new FileWriter(saveFile))) {
				for(PriceTable table: goodData) {
					save.append(table.toCSV()).append("\n");
				}
			}
		} else {
			logger.info("# missing data {} {}", missingSymbols.size(), missingSymbols);
			// Save content of goodData as bad saveFile
			final String badSaveFilePath = String.format("tmp/database/price-%d.BAD", dateFrom.getYear());
			File badSaveFile = new File(badSaveFilePath);
			if (!badSaveFile.exists()) badSaveFile.createNewFile();
			try (BufferedWriter save = new BufferedWriter(new FileWriter(badSaveFile))) {
				for(PriceTable table: goodData) {
					save.append(table.toCSV()).append("\n");
				}
			}
		}
	}

	private static final String OUTPUT_PATH = "tmp/downloadPrice.log";
	
	public static void main(String[] args) {
		logger.info("START");
		final int years     = Integer.valueOf(args[0]);
		final int lastYear  = LocalDate.now().getYear();
		final int firstYear = lastYear - years + 1;
		logger.info("year {} - {}", firstYear, lastYear);
		
		try {
			Class.forName("org.sqlite.JDBC");
			try (Connection connection = DriverManager.getConnection("jdbc:sqlite:tmp/sqlite/securities.sqlite3");
				BufferedWriter bw = new BufferedWriter(new FileWriter(OUTPUT_PATH))) {
				Map<String, NasdaqTable> nasdaqMap = NasdaqTable.getMap(connection);
				logger.info("nasdaqMap     = {}", nasdaqMap.size());
				
				for(int year = firstYear; year <= lastYear; year++) {
					LocalDate dateFrom = LocalDate.of(year, 1, 1);
					LocalDate dateTo   = dateFrom.plusYears(1).minusDays(1);

					check(connection, bw, nasdaqMap, dateFrom, dateTo);
				}
			}
		} catch (ClassNotFoundException | SQLException | IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		logger.info("STOP");
	}
}
