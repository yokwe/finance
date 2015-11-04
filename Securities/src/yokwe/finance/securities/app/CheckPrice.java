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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.database.NasdaqTable;
import yokwe.finance.securities.database.PriceTable;
import yokwe.finance.securities.update.GoogleHistorical;
import yokwe.finance.securities.update.YahooDaily;
import yokwe.finance.securities.util.HttpUtil;

public class CheckPrice {
	private static final Logger logger = LoggerFactory.getLogger(CheckPrice.class);
	
	static final int YEAR_FIRST = 1975;
	static final int YEAR_LAST  = LocalDate.now().getYear();

	// Use yahoo-table
	static List<PriceTable> getFromYahoo(final String exch, final String symbol, final LocalDate dateFrom, final LocalDate dateTo) {
		// http://real-chart.finance.yahoo.com/table.csv?s=SPY&a=00&b=01&c=2015&d=12&e=30&f=2015&ignore=.csv
		final String s = symbol;
		final int a = dateFrom.getMonthValue() - 1;
		final int b = dateFrom.getDayOfMonth();
		final int c = dateFrom.getYear();
		final int d = dateTo.getMonthValue() - 1;
		final int e = dateTo.getDayOfMonth();
		final int f = dateTo.getYear();
		
		final String url = String.format("http://real-chart.finance.yahoo.com/table.csv?s=%s&a=%02d&b=%02d&c=%d&d=%02d&e=%02d&f=%d&g=d&ignore=.csv", s, a, b, c, d, e, f);
		final List<String> lines = HttpUtil.download(url);
		
		final List<PriceTable> ret = new ArrayList<>();
		if (lines == null) return ret;
		
		int count = 0;
		for(String line: lines) {
			if (count++ == 0) {
				YahooDaily.CSVRecord.checkHeader(line);
			} else {
				ret.add(YahooDaily.CSVRecord.toPriceTable(symbol, line));
			}
		}
		return ret;
	}
	
	static List<PriceTable> getFromGoogle(final String exch, final String symbol, final LocalDate dateFrom, final LocalDate dateTo) {
		// http://www.google.com/finance/historical?q=NYSE:IBM&&startdate=Jan%201,%202000&enddate=Dec%2031,%202050&output=csv
		final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);

		final String startdate = dateFrom.format(dateFormatter).replace(" ", "%20");
		final String enddate   = dateTo.format(dateFormatter).replace(" ", "%20");
		final String url = String.format("http://www.google.com/finance/historical?q=%s:%s&&startdate=%s&enddate=%s&output=csv", exch, symbol, startdate, enddate);
		List<String> lines = HttpUtil.download(url);

		final List<PriceTable> ret = new ArrayList<>();
		if (lines == null) return ret;
		
		int count = 0;
		for(String line: lines) {
			if (count++ == 0) {
				GoogleHistorical.CSVRecord.checkHeader(line);
			} else {
				ret.add(GoogleHistorical.CSVRecord.toPriceTable(symbol, line));
			}
		}
		return ret;
	}
	
	static void check(Connection connection, final BufferedWriter wr, final Map<String, NasdaqTable>  nasdaqMap, final LocalDate dateFrom, final LocalDate dateTo) throws IOException {
		String saveFilePath = String.format("tmp/database/price-%d.csv", dateFrom.getYear());
		File saveFile = new File(saveFilePath);
		if (saveFile.isFile()) {
			logger.info("# skip {}", saveFilePath);
			return;
		}
		
		List<PriceTable> data = PriceTable.getAllByDateRange(connection, dateFrom, dateTo);
		logger.info("data {}  {}  {}", dateFrom, dateTo, data.size());
		wr.append(String.format("# data  %s  %s  %d\n", dateFrom, dateTo, data.size()));

		List<String> dateList = data.stream().filter(o -> o.symbol.equals("IBM")).map(o -> o.date).collect(Collectors.toList());
		Collections.sort(dateList);
		Set<String> dateSet = new HashSet<>(dateList);
		
		// check duplicate
		{
			String lastDate = null;
			for(String date: dateList) {
				if (lastDate != null && date.equals(lastDate)) {
					logger.error("duplicate dateList {}", date);
					throw new SecuritiesException("duplicate date");
				}
				lastDate = date;
			}
		}
		
		// check with NYT
		{
			List<String> dateList2 = data.stream().filter(o -> o.symbol.equals("NYT")).map(o -> o.date).collect(Collectors.toList());
			Set<String> dateSet2 = new HashSet<>(dateList2);
			for(String date: dateList) {
				if (dateSet2.contains(date)) continue;
				logger.info("dateSet date is missing  date = {}", date);
				throw new SecurityException("dateSet");
			}
			for(String date: dateList2) {
				if (dateSet.contains(date)) continue;
				logger.info("dateSet2 date is missing  date = {}", date);
				throw new SecurityException("dateSet");
			}
		}
		
		// check with PG
		{
			List<String> dateList2 = data.stream().filter(o -> o.symbol.equals("PG")).map(o -> o.date).collect(Collectors.toList());
			Set<String> dateSet2 = new HashSet<>(dateList2);
			for(String date: dateList) {
				if (dateSet2.contains(date)) continue;
				logger.info("dateSet date is missing  date = {}", date);
				throw new SecurityException("dateSet");
			}
			for(String date: dateList2) {
				if (dateSet.contains(date)) continue;
				logger.info("dateSet2 date is missing  date = {}", date);
				throw new SecurityException("dateSet");
			}
		}
		
		List<PriceTable> goodData   = new ArrayList<>();
		List<String>     badSymbols = new ArrayList<>();
		for(String symbol: nasdaqMap.keySet()) {
			final List<PriceTable> data2 = data.stream().filter(o -> o.symbol.equals(symbol)).collect(Collectors.toList());
			
			// Skip symbol that has no data
			if (data2.size() == 0) continue;
			
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
			
			final List<String> dateList2 = data2.stream().map(o -> o.date).collect(Collectors.toList());
			final String dateFirst2 = dateList2.get(0);
			
			{
				boolean foundError = false;
				for(String date: dateList) {
					// Skip data before dateFirst2 -- before inception date
					if (date.compareTo(dateFirst2) < 0) continue;
					if (!map.containsKey(date)) {
						// missing date
						logger.info("missing  {} {}", date, symbol);
						badSymbols.add(symbol);
						foundError = true;
						break;
					}
				}
				// skip to next symbol if found error
				if (foundError) continue;
			}

			// build goodData to remove surplus date
			for(String date: dateList) {
				// Skip data before dateFirst2 -- before inception date
				if (date.compareTo(dateFirst2) < 0) continue;
				goodData.add(map.get(date));
			}
		}
		if (0 < badSymbols.size()) logger.info("badSymbols     {}", badSymbols);

		List<String> stillBadSymbols = new ArrayList<>();
		for(String symbol: badSymbols) {
			final Map<String, PriceTable> yahooMap = new HashMap<>();
			final Map<String, PriceTable> googleMap = new HashMap<>();

			{
				String exch = nasdaqMap.get(symbol).exchange;
				
				List<PriceTable> yahooData = getFromYahoo(exch, symbol, dateFrom, dateTo);
				yahooData.stream().forEach(o -> yahooMap.put(o.date, o));
				
				List<PriceTable> googleData = getFromGoogle(exch, symbol, dateFrom, dateTo);
				googleData.stream().forEach(o -> googleMap.put(o.date, o));
				
//				logger.info("badSymbols {}  yahoo {}  google {}", symbol, yahooData.size(), googleData.size());
			}
			
			List<String> missingDate = new ArrayList<>();
			List<PriceTable> myGoodData = new ArrayList<>();
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
			if (missingDate.size() == 0) {
				// repaired from yahoo and google
				goodData.addAll(myGoodData);
			} else {
//				logger.info("MISSING  {} {} {}", symbol, missingDate.size(), missingDate);
				stillBadSymbols.add(symbol);
			}
		}
		
		if (stillBadSymbols.size() == 0) {
			// Save content of data to saveFile
			logger.info("# save {}", saveFilePath);
			saveFile.createNewFile();
			try (BufferedWriter save = new BufferedWriter(new FileWriter(saveFile))) {
				for(PriceTable table: goodData) {
					// 1975-10-27,AA,36.25,276800
					save.write(String.format("%s,%s,%.2f,%d\n", table.date, table.symbol, table.close, table.volume));
				}
			}
		} else {
			logger.info("# missing data {}", stillBadSymbols);
		}
	}

	private static final String OUTPUT_PATH = "tmp/checkPrice.log";
	
	public static void main(String[] args) {
		logger.info("START");
		try {
			Class.forName("org.sqlite.JDBC");
			try (Connection connection = DriverManager.getConnection("jdbc:sqlite:tmp/sqlite/securities.sqlite3");
				BufferedWriter bw = new BufferedWriter(new FileWriter(OUTPUT_PATH))) {
				Map<String, NasdaqTable>  nasdaqMap  = NasdaqTable.getMap(connection);
				logger.info("nasdaqMap     = {}", nasdaqMap.size());

				for(int year = YEAR_FIRST; year <= YEAR_LAST; year++) {
					LocalDate dateFrom = LocalDate.of(year, 1, 1);
					LocalDate dateTo = dateFrom.plusYears(1).minusDays(1);

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
