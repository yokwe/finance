package yokwe.finance.securities.app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.database.DividendTable;
import yokwe.finance.securities.database.NasdaqTable;
import yokwe.finance.securities.update.YahooDividend;
import yokwe.finance.securities.util.NasdaqUtil;

public final class UpdateDividend {
	private static final Logger logger = LoggerFactory.getLogger(UpdateDividend.class);
	
	static final int BUFFER_SIZE = 64 * 1024;
	
	static final long WAIT_TIME = 300;

	static enum Provider {
		YAHOO,
	}
	// YEAR-SYMBOL-PROVIDER.csv
	//   2015-VCLT-yahoo.csv
	private static String HTML_FILE_PATH_FORMAT = "tmp/update/dividend/%d/%s-%s.csv";
	static String getFilePath(final int year, final String symbol, final Provider provider) {
		return String.format(HTML_FILE_PATH_FORMAT, year, symbol, provider.name());
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
			String dirPath = String.format("tmp/update/dividend/%d", year);
			File dir = new File(dirPath);
			if (!dir.exists()) dir.mkdirs();
		}
		
		// Download dividend file from yahoo and google		
		logger.info("Build pathURLMap");
		Map<String, PathURL> yahooPathURLMap  = new TreeMap<>();
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
				
				final String url = String.format("http://real-chart.finance.yahoo.com/table.csv?s=%s&a=%02d&b=%02d&c=%d&d=%02d&e=%02d&f=%d&g=v&ignore=.csv", s, a, b, c, d, e, f);

				yahooPathURLMap.put(symbol, new PathURL(path, url));
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
					PathURL pathURL = yahooPathURLMap.get(symbol);
					File file = new File(pathURL.path);
					
					if (file.exists()) continue;
					
					long thisTime = System.currentTimeMillis();
					long sleepTime = lastTime + WAIT_TIME - thisTime;
					lastTime = thisTime;
					if (0 < sleepTime) Thread.sleep(sleepTime);

					Fetch.download(pathURL.url, pathURL.path, new ArrayList<>());
				}
			}
		}
		
		logger.info("Build data");
		List<DividendTable> data = new ArrayList<>();
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
					try (BufferedReader br = new BufferedReader(new FileReader(file), BUFFER_SIZE)) {
						final String header = br.readLine();
						if (header != null) {
							YahooDividend.CSVRecord.checkHeader(header);
							for(;;) {
								final String line = br.readLine();
								if (line == null) break;
								final DividendTable table = YahooDividend.CSVRecord.toDividendTable(symbol, line);
								data.add(table);
							}
						}
					}
				}
			}
		}
		
		logger.info("Save data");
		{
			final String saveFilePathFormat = "tmp/database/dividend-%d.csv";
			final String saveFilePath = String.format(saveFilePathFormat, dateFrom.getYear());
			final File saveFile = new File(saveFilePath);
			if (!saveFile.exists()) saveFile.createNewFile();
			
			logger.info("Save {}", saveFilePath);
			saveFile.createNewFile();
			try (BufferedWriter save = new BufferedWriter(new FileWriter(saveFile), BUFFER_SIZE)) {
				for(DividendTable table: data) {
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
