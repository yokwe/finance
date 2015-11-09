package yokwe.finance.securities.app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.database.NasdaqTable;
import yokwe.finance.securities.database.PriceTable;
import yokwe.finance.securities.update.GoogleHistorical;
import yokwe.finance.securities.update.YahooDaily;
import yokwe.finance.securities.util.NasdaqUtil;

public final class UpdatePrice {
	private static final Logger logger = LoggerFactory.getLogger(UpdatePrice.class);
	
	static final int BUFFER_SIZE = 64 * 1024;
	
	static enum Provider {
		YAHOO, GOOGLE,
	}
	// 2015-VCLT-yahoo.csv
	//   YEAR-SYMBOL-PROVIDER.csv
	private static String HTML_FILE_PATH_FORMAT = "tmp/update/price/%d-%s-%s.csv";
	static String getFilePath(final int year, final String symbol, final Provider provider) {
		return String.format(HTML_FILE_PATH_FORMAT, year, symbol, provider.name());
	}
	
	static List<String> getDateList(Map<String, Map<String, PriceTable>> map) {
		Map<String, PriceTable> ibmMap = map.get("IBM");
		Map<String, PriceTable> nytMap = map.get("NYT");
		Map<String, PriceTable> pepMap = map.get("PEP");
		
		if (ibmMap.size() != nytMap.size()) {
			logger.error("ibm = {}  nyt = {}", ibmMap.size(), nytMap.size());
			throw new SecuritiesException("getDateList");
		}
		if (nytMap.size() != pepMap.size()) {
			logger.error("nyt = {}  pep = {}", nytMap.size(), pepMap.size());
			throw new SecuritiesException("getDateList");
		}
		if (ibmMap.size() != pepMap.size()) {
			logger.error("ibm = {}  pep = {}", ibmMap.size(), pepMap.size());
			throw new SecuritiesException("getDateList");
		}
		
		for(String date: ibmMap.keySet()) {
			if (nytMap.containsKey(date)) continue;
			logger.error("nyt missing date = {}", date);
			throw new SecuritiesException("getDateList");
		}
		for(String date: ibmMap.keySet()) {
			if (pepMap.containsKey(date)) continue;
			logger.error("nyt missing date = {}", date);
			throw new SecuritiesException("getDateList");
		}
		
		List<String> ret = new ArrayList<>(ibmMap.keySet());
		Collections.sort(ret);
		return ret;
	}
	
	static class PathURL {
		public final String path;
		public final String url;
		public PathURL(String path, String url) {
			this.path = path;
			this.url  = url;
		}
	}
	static void update(final int year) throws IOException {
		final Map<String, NasdaqTable> nasdaqMap = NasdaqUtil.getMap();
		
		final LocalDate dateFrom = LocalDate.of(year, 1, 1);
		final LocalDate dateTo   = dateFrom.plusYears(1).minusDays(1);
		
		// Download price file from yahoo and google		
		logger.info("Build pathURLMap");
		Map<String, PathURL> yahooPathURLMap  = new TreeMap<>();
		Map<String, PathURL> googlePathURLMap = new TreeMap<>();
		for(String symbol: nasdaqMap.keySet()) {
			NasdaqTable nasdaq = nasdaqMap.get(symbol);
			// yahoo
			{
				final String path = getFilePath(year, symbol, Provider.YAHOO);
				
				// http://real-chart.finance.yahoo.com/table.csv?s=SPY&a=00&b=01&c=2015&d=12&e=30&f=2015&ignore=.csv
				final String s = nasdaq.yahoo;
				final int a = dateFrom.getMonthValue() - 1;
				final int b = dateFrom.getDayOfMonth();
				final int c = dateFrom.getYear();
				final int d = dateTo.getMonthValue() - 1;
				final int e = dateTo.getDayOfMonth();
				final int f = dateTo.getYear();
				
				final String url = String.format("http://real-chart.finance.yahoo.com/table.csv?s=%s&a=%02d&b=%02d&c=%d&d=%02d&e=%02d&f=%d&g=d&ignore=.csv", s, a, b, c, d, e, f);

				yahooPathURLMap.put(symbol, new PathURL(path, url));
			}
			// google
			{
				final String path = getFilePath(year, symbol, Provider.GOOGLE);
				
				// http://www.google.com/finance/historical?q=NYSE:IBM&startdate=Jan%201,%202000&enddate=Dec%2031,%202050&output=csv
				final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);

				final String startdate = dateFrom.format(dateFormatter).replace(" ", "%20");
				final String enddate   = dateTo.format(dateFormatter).replace(" ", "%20");
				final String url = String.format("http://www.google.com/finance/historical?q=%s:%s&startdate=%s&enddate=%s&output=csv", nasdaq.exchange, nasdaq.google, startdate, enddate);
				
				googlePathURLMap.put(symbol, new PathURL(path, url));
			}
		}
		
		// Fetch
		logger.info("Fetch");
		{
			final int total = nasdaqMap.keySet().size();
			int count = 0;
			for(String symbol: nasdaqMap.keySet()) {
				if ((count++ % 100) == 0) {
					logger.info("{}", String.format("%4d / %4d  %s", count, total, symbol));
				}
				{
					PathURL pathURL = yahooPathURLMap.get(symbol);
					File file = new File(pathURL.path);
					if (!file.exists()) {
						Fetch.download(pathURL.url, pathURL.path, new ArrayList<>());
					}
				}
				{
					PathURL pathURL = googlePathURLMap.get(symbol);
					File file = new File(pathURL.path);
					if (!file.exists()) {
						Fetch.download(pathURL.url, pathURL.path, new ArrayList<>());
					}
				}
			}
		}
		
		// Build map   Map<Symbol, Map<Date, PriceTable>>
		logger.info("Build map");
		Map<String, Map<String, PriceTable>> yahooMap  = new HashMap<>();
		Map<String, Map<String, PriceTable>> googleMap = new HashMap<>();
		{
			final int total = nasdaqMap.keySet().size();
			int count = 0;
			for(String symbol: nasdaqMap.keySet()) {
				if ((count++ % 100) == 0) {
					logger.info("{}", String.format("%4d / %4d  %s", count, total, symbol));
				}

				{
					final String path = yahooPathURLMap.get(symbol).path;
					final File file = new File(path);
					if (!file.isFile()) {
						logger.error("no file  {}", path);
						throw new SecuritiesException("no file");
					}
					final Map<String, PriceTable> dateMap = new HashMap<>();
					yahooMap.put(symbol, dateMap);
					try (BufferedReader br = new BufferedReader(new FileReader(file), BUFFER_SIZE)) {
						final String header = br.readLine();
						if (header != null) {
							YahooDaily.CSVRecord.checkHeader(header);
							for(;;) {
								final String line = br.readLine();
								if (line == null) break;
								final PriceTable priceTable = YahooDaily.CSVRecord.toPriceTable(symbol, line);
								dateMap.put(priceTable.date, priceTable);
							}
						}
					}
				}
				{
					final String path = googlePathURLMap.get(symbol).path;
					final File file = new File(path);
					if (!file.isFile()) {
						logger.error("not file  {}", path);
						throw new SecuritiesException("no file");
					}
					final Map<String, PriceTable> dateMap = new HashMap<>();
					googleMap.put(symbol, dateMap);
					try (BufferedReader br = new BufferedReader(new FileReader(file), BUFFER_SIZE)) {
						final String header = br.readLine();
						if (header != null) {
							GoogleHistorical.CSVRecord.checkHeader(header);
							for(;;) {
								final String line = br.readLine();
								if (line == null) break;
								final PriceTable priceTable = GoogleHistorical.CSVRecord.toPriceTable(symbol, line);
								dateMap.put(priceTable.date, priceTable);
							}
						}
					}
				}
			}
		}

		final List<String> dateList = getDateList(yahooMap);
		logger.info("Build dateList  {}", dateList.size());
		

		logger.info("Build data");
		List<String>     badSymbols = new ArrayList<>();
		List<PriceTable> data = new ArrayList<>();
		{
			final int total = nasdaqMap.keySet().size();
			int count = 0;

			for(String symbol: nasdaqMap.keySet()) {			
				if ((count++ % 100) == 0) {
					logger.info("{}", String.format("%4d / %4d  %s", count, total, symbol));
				}

				final Map<String, PriceTable> yahoo  = yahooMap.get(symbol);
				final Map<String, PriceTable> google = googleMap.get(symbol);

				int countMissing = 0;
				for(String date: dateList) {
					if (yahoo.containsKey(date)) {
						data.add(yahoo.get(date));
					} else {
						if (google.containsKey(date)) {
							data.add(google.get(date));
						} else {
							countMissing++;
						}
					}
				}
				if (0 < countMissing) {
					// repair failed with yahoo and google (some data is still missing)
					badSymbols.add(symbol);
				}
			}

		}
		
		logger.info("Save data");
		{
			final String saveFilePathFormat = (badSymbols.size() == 0) ? "tmp/database/price-%d.csv" : "tmp/database/price-%d.BAD";
			final String saveFilePath = String.format(saveFilePathFormat, dateFrom.getYear());
			final File saveFile = new File(saveFilePath);
			if (!saveFile.exists()) saveFile.createNewFile();
			
			logger.info("Save {}", saveFilePath);
			saveFile.createNewFile();
			try (BufferedWriter save = new BufferedWriter(new FileWriter(saveFile), BUFFER_SIZE)) {
				for(PriceTable table: data) {
					save.append(table.toCSV()).append("\n");
				}
			}
		}
	}

	public static void main(String[] args) {
		logger.info("START");
		final int year  = LocalDate.now().getYear();
		logger.info("year {}", year);
		
		try {
			update(year);
		} catch (RuntimeException | IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		
		logger.info("STOP");
	}
}
