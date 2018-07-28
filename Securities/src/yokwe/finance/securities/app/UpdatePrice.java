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
import yokwe.finance.securities.util.HttpUtil;
import yokwe.finance.securities.util.NasdaqUtil;

public final class UpdatePrice {
	private static final Logger logger = LoggerFactory.getLogger(UpdatePrice.class);
	
	static final int  BUFFER_SIZE = 64 * 1024;
	static final long WAIT_TIME   = 300;

	static enum Provider {
		YAHOO, GOOGLE,
	}
	// 2015-VCLT-yahoo.csv
	//   YEAR-SYMBOL-PROVIDER.csv
	private static String HTML_FILE_PATH_FORMAT = "tmp/update/price/%d/%s-%s.csv";
	static String getFilePath(final int year, final String symbol, final Provider provider) {
		return String.format(HTML_FILE_PATH_FORMAT, year, symbol, provider.name());
	}
	
	static List<String> getDateList(Map<String, Map<String, PriceTable>> map) {
		Map<String, PriceTable> ibmMap = map.get("IBM");
		Map<String, PriceTable> nytMap = map.get("NYT");
		Map<String, PriceTable> pepMap = map.get("PEP");
		
		if (ibmMap == null) {
			logger.error("ibm == null");
		}
		if (nytMap == null) {
			logger.error("nytMap == null");
		}
		if (pepMap == null) {
			logger.error("pepMap == null");
		}
		
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
	static void update(final int year) throws IOException, InterruptedException {
		final Map<String, NasdaqTable> nasdaqMap = NasdaqUtil.getMap();
		
		final LocalDate dateFrom = LocalDate.of(year, 1, 1);
		final LocalDate dateTo   = dateFrom.plusYears(1).minusDays(1);
		
		{
			String dirPath = String.format("tmp/update/price/%d", year);
			File dir = new File(dirPath);
			if (!dir.exists()) dir.mkdirs();
		}
		
		// Download price file from yahoo and google		
		logger.info("Build pathURLMap");
		Map<String, PathURL> googlePathURLMap = new TreeMap<>();
		for(String symbol: nasdaqMap.keySet()) {
			NasdaqTable nasdaq = nasdaqMap.get(symbol);
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
			long lastTime = System.currentTimeMillis();
			for(String symbol: nasdaqMap.keySet()) {
				if ((count++ % 100) == 0) {
					logger.info("{}", String.format("%4d / %4d  %s", count, total, symbol));
				}
				{
					
					PathURL googlePathURL = googlePathURLMap.get(symbol);
					File googleFile = new File(googlePathURL.path);
					
					if (googleFile.exists()) continue;
					
					long thisTime = System.currentTimeMillis();
					long sleepTime = lastTime + WAIT_TIME - thisTime;
					lastTime = thisTime;
					if (0 < sleepTime) Thread.sleep(sleepTime);

					if (!googleFile.exists()) {
						HttpUtil.download(googlePathURL.url, googlePathURL.path);
					}
				}
			}
		}
		
		// Build map   Map<Symbol, Map<Date, PriceTable>>
		logger.info("Build map");
		Map<String, Map<String, PriceTable>> googleMap = new HashMap<>();
		{
			final int total = nasdaqMap.keySet().size();
			int count = 0;
			for(String symbol: nasdaqMap.keySet()) {
				if ((count++ % 100) == 0) {
					logger.info("{}", String.format("%4d / %4d  %s", count, total, symbol));
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

		// Merge yahoo and google dateList
		final List<String> dateList = new ArrayList<>();
		{
			List<String> googleDateList = getDateList(googleMap);
			for(String date: googleDateList) {
				if (dateList.contains(date)) continue;
				dateList.add(date);
			}
			Collections.sort(dateList);
		}
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

				final Map<String, PriceTable> google = googleMap.get(symbol);

				int countMissing = 0;
				for(String date: dateList) {
					if (google.containsKey(date)) {
						data.add(google.get(date));
					} else {
						countMissing++;
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
		} catch (RuntimeException | IOException | InterruptedException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		
		logger.info("STOP");
	}
}
