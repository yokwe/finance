package yokwe.finance.securities.eod;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.DoubleUtil;
import yokwe.finance.securities.util.FileUtil;
import yokwe.finance.securities.util.HttpUtil;

public class UpdatePrice {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdatePrice.class);
	
	private static final int       DURTION_YEAR  = 1; // we need one year data
	private static final int       MAX_RETRY     = 3;  // try 3 times
	
	private static final LocalDate DATE_LAST     = Market.getLastTradingDate();
	private static final LocalDate DATE_FIRST    = DATE_LAST.minusYears(DURTION_YEAR);
	
	public static final class UpdateProviderGoogle implements UpdateProvider {
		private static final String PATH_DIR      = "tmp/eod/price";
		public String getRootPath() {
			return PATH_DIR;
		}
		public String getName() {
			return GOOGLE;
		}
		
		public File getFile(String symbol) {
			String path = String.format("%s-%s/%s.csv", PATH_DIR, getName(), symbol);
			File file = new File(path);
			return file;
		}

		private static final DateTimeFormatter DATE_FORMAT_URL    = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);
		private static final DateTimeFormatter DATE_FORMAT_PARSE  = DateTimeFormatter.ofPattern("d-MMM-yy");
		private static final DateTimeFormatter DATE_FORMAT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		public boolean updateFile(String exch, String symbol, LocalDate dateFirst, LocalDate dateLast) {
			File file = getFile(symbol);
			
			String dateFrom = dateFirst.format(DATE_FORMAT_URL).replace(" ", "%20");
			String dateTo   = dateLast.format(DATE_FORMAT_URL).replace(" ", "%20");
			
			Stock stock = StockUtil.get(symbol.replace(".PR.", "-"));

			// Convert from '.PR.' to  '-' in symbol for google
			String url = String.format("https://www.google.com/finance/historical?q=%s:%s&startdate=%s&enddate=%s&output=csv", stock.exchange, stock.symbol, dateFrom, dateTo);

			String content = HttpUtil.downloadAsString(url);
			if (content == null) {
				// cannot get content
				file.delete();
				return false;
			}
			
			String[] lines = content.split("\n");
//			logger.info("lines {}", lines.length);
			if (lines.length <= 1) {
				// only header
				file.delete();
				return false;
			}
			
			// Sanity check
			String GOOGLE_PRICE_HEADER = "\uFEFFDate,Open,High,Low,Close,Volume";
			String header = lines[0];
			if (!header.equals(GOOGLE_PRICE_HEADER)) {
				logger.error("Unexpected header  {}", header);
				throw new SecuritiesException("Unexpected header");
			}

			String      targetDate  = dateLast.toString();
			boolean     targetFound = false;
			List<Price> priceList   = new ArrayList<>();
			
			for(String line: lines) {
				if (line.startsWith("\uFEFFDate,Open,High,Low,Close,Volume")) continue;
				
				String[] values = line.split(",");
				if (values.length != 6) {
					logger.error("Unexpected line  {}", line);
					throw new SecuritiesException("Unexpected header");
				}
				
				// Fix format of date  02-Jan-14 => 2014-01-02
				// Fix year of date    02-Jan-80 => 1980-01-02
				{
					LocalDate localDate = LocalDate.from(DATE_FORMAT_PARSE.parse(values[0]));
					if (DATE_LAST.getYear() < localDate.getYear()) localDate = localDate.minusYears(100);
					values[0] = DATE_FORMAT_FORMAT.format(localDate);
				}
				
				// Special when field (open, high and low) contains dash
				if (values[1].equals("-")) {
					values[1] = values[4];
				}
				if (values[2].equals("-")) {
					values[2] = values[4];
				}
				if (values[3].equals("-")) {
					values[3] = values[4];
				}
				
				// Special when field (volume) contains dash
				if (values[5].equals("-")) {
					values[5] = "0";
				}

				String date   = values[0];
				double open   = DoubleUtil.round(Double.valueOf(values[1]), 2);
				double high   = DoubleUtil.round(Double.valueOf(values[2]), 2);
				double low    = DoubleUtil.round(Double.valueOf(values[3]), 2);
				double close  = DoubleUtil.round(Double.valueOf(values[4]), 2);
				long   volume = Long.valueOf(values[5]);
				
				if (date.equals(targetDate)) targetFound = true;
				
				priceList.add(new Price(date, symbol, open, high, low, close, volume));
			}
			
			if (targetFound) {
				Price.save(priceList, file);
				return true;
			} else {
				// no target date data
				// file.delete(); // keep old file
				return false;
			}
		}
	}
	
	public static final class UpdateProviderYahoo implements UpdateProvider {
		private static final String PATH_DIR      = "tmp/eod/price";
		public String getName() {
			return YAHOO;
		}
		
		public File getFile(String symbol) {
			String path = String.format("%s-%s/%s.csv", PATH_DIR, getName(), symbol);
			File file = new File(path);
			return file;
		}

		public boolean updateFile(String exch, String symbol, LocalDate dateFirst, LocalDate dateLast) {
			File file = getFile(symbol);
			
			Stock stock = StockUtil.get(symbol.replace(".PR.", "-"));

			// first
			int a = dateFirst.getMonthValue(); // mm
			int b = dateFirst.getDayOfMonth(); // dd
			int c = dateFirst.getYear();       // yyyy
			// last
			int d = dateLast.getMonthValue(); // mm
			int e = dateLast.getDayOfMonth(); // dd
			int f = dateLast.getYear();       // yyyy
			String url = String.format("http://real-chart.finance.yahoo.com/table.csv?s=%s&a=%02d&b=%02d&c=%04d&d=%02d&e=%02d&f=%04d&ignore=.csv", stock.symbolYahoo, a - 1, b, c, d - 1, e, f);
			String content = HttpUtil.downloadAsString(url);
			if (content == null) {
				// cannot get content
				file.delete();
				return false;
			}
			
			String[] lines = content.split("\n");
//			logger.info("lines {}", lines.length);
			if (lines.length <= 1) {
				// only header
				file.delete();
				return false;
			}
			
			// Sanity check
			String YAHOO_PRICE_HEADER = "Date,Open,High,Low,Close,Volume,Adj Close";
			String header = lines[0];
			if (!header.equals(YAHOO_PRICE_HEADER)) {
				logger.error("Unexpected header  {}", header);
				throw new SecuritiesException("Unexpected header");
			}

			String      targetDate  = dateLast.toString();
			boolean     targetFound = false;
			List<Price> priceList   = new ArrayList<>();
			
			for(String line: lines) {
				if (line.startsWith(YAHOO_PRICE_HEADER)) continue;
				
				String[] values = line.split(",");
				if (values.length != 7) {
					logger.error("Unexpected line  {}", line);
					throw new SecuritiesException("Unexpected header");
				}
				
				String date   = values[0];
				double open   = DoubleUtil.round(Double.valueOf(values[1]), 2);
				double high   = DoubleUtil.round(Double.valueOf(values[2]), 2);
				double low    = DoubleUtil.round(Double.valueOf(values[3]), 2);
				double close  = DoubleUtil.round(Double.valueOf(values[4]), 2);
				long   volume = Long.valueOf(values[5]);
				
				if (date.equals(targetDate)) targetFound = true;
				
				priceList.add(new Price(date, symbol, open, high, low, close, volume));
			}
			
			if (targetFound) {
				Price.save(priceList, file);
				return true;
			} else {
				// no target date data
				// file.delete(); // keep old file
				return false;
			}
		}
	}
	
	private static Map<String, UpdateProvider> updateProviderMap = new TreeMap<>();
	static {
		updateProviderMap.put(UpdateProvider.GOOGLE, new UpdateProviderGoogle());
		updateProviderMap.put(UpdateProvider.YAHOO,  new UpdateProviderYahoo());
	}
	public static UpdateProvider getProvider(String provider) {
		if (updateProviderMap.containsKey(provider)) {
			return updateProviderMap.get(provider);
		} else {
			logger.error("Unknonw provider = {}", provider);
			throw new SecuritiesException("Unknonw provider");
		}
	}

	private static boolean needUpdate(File file, LocalDate dateFirst, LocalDate dateLast) {
		String content = FileUtil.read(file);
		String[] lines = content.split("\n");
		
		if (content.length() == 0) return true;
		
		// Sanity check
		if (lines.length <= 1) {
			logger.error("Unexpected content {}", content);
			throw new SecuritiesException("Unexpected content");
		}
		
		// first line should be header
		String HEADER = "date,symbol,open,high,low,close,volume";
		String header = lines[0];
		if (!header.equals(HEADER)) {
			logger.error("Unexpected header  {}", header);
			throw new SecuritiesException("Unexpected header");
		}
		// second line should be last data
		String line = lines[1];
		String[] values = line.split(",");
		if (values.length != 7) {
			logger.error("Unexpected line  {}", line);
			throw new SecuritiesException("Unexpected header");
		}
		String date = values[0];
		
		return !date.equals(dateLast.toString());
	}
	
	private static void updateFile(UpdateProvider updateProvider) {
		Map<String, Stock> stockMap = new TreeMap<>();
		StockUtil.getAll().stream().forEach(e -> stockMap.put(e.symbol, e));
		Set<String> symbolSet = new TreeSet<>(stockMap.keySet());
		logger.info("symbolSet {}", symbolSet.size());
		
		int total = stockMap.size();
		int countUpdate = 0;
		int countOld    = 0;
		int countSkip   = 0;
		int countNew    = 0;
		int countNone   = 0;
		
		int retryCount  = 0;
		boolean needSleep = false;

		for(;;) {
			retryCount++;
			if (MAX_RETRY < retryCount) break;
			logger.info("retry  {}", String.format("%4d", retryCount));
			
			if (needSleep) {
				try {
					logger.info("sleep");
					Thread.sleep(1 * 60 * 1000); // 1 minute
				} catch (InterruptedException e1) {
					logger.info("InterruptedException");
				}
			}
			
			int count = 0;
			int lastOutputCount = -1;
			Set<String> nextSymbolSet = new TreeSet<>();
			int lastCountOld = countOld;
			countOld = 0;

			int symbolSetSize = symbolSet.size();
			for(String symbol: symbolSet) {
				Stock  stock = stockMap.get(symbol);
				String exch  = stock.exchange;

				int showInterval = (symbolSet.size() < 100) ? 1 : 100;
				int outputCount = count / showInterval;
				boolean showOutput;
				if (outputCount != lastOutputCount) {
					showOutput = true;
					lastOutputCount = outputCount;
				} else {
					showOutput = false;
				}

				count++;
				
				File file = updateProvider.getFile(symbol);
				if (file.exists()) {
					if (needUpdate(file, DATE_FIRST, DATE_LAST)) {
						if (updateProvider.updateFile(exch, symbol, DATE_FIRST, DATE_LAST)) {
							if (showOutput) logger.info("{}  update {}", String.format("%4d / %4d",  count, symbolSetSize), symbol);
							countUpdate++;
						} else {
							if (showOutput) logger.info("{}  old    {}", String.format("%4d / %4d",  count, symbolSetSize), symbol);
							countOld++;
							nextSymbolSet.add(symbol);
						}
					} else {
						if (showOutput) logger.info("{}  skip   {}", String.format("%4d / %4d",  count, symbolSetSize), symbol);
						countSkip++;
					}
				} else {
					if (updateProvider.updateFile(exch, symbol, DATE_FIRST, DATE_LAST)) {
						/*if (showOutput)*/ logger.info("{}  new    {}", String.format("%4d / %4d",  count, symbolSetSize), symbol);
						countNew++;
					} else {
//						/*if (showOutput)*/ logger.info("{}  none   {}", String.format("%4d / %4d",  count, symbolSetSize), symbol);
						countNone++;
					}
				}
			}
			logger.info("old    {}", String.format("%4d", countOld));
			if (countOld != lastCountOld) {
				retryCount = 0; // reset retry count
			}
			needSleep = true;
			symbolSet = nextSymbolSet;
		}
		logger.info("===========");
		logger.info("update {}", String.format("%4d", countUpdate));
		logger.info("old    {}", String.format("%4d", countOld));
		logger.info("skip   {}", String.format("%4d", countSkip));
		logger.info("new    {}", String.format("%4d", countNew));
		logger.info("none   {}", String.format("%4d", countNone));
		logger.info("total  {}", String.format("%4d", countUpdate + countOld + countSkip + countNew + countNone));
		logger.info("total  {}", String.format("%4d", total));
	}
	
	// This methods update end of day csv in tmp/eod directory.
	public static void main(String[] args) {
		logger.info("START");
		
		String providerName = args[0];
		UpdateProvider updateProvider = getProvider(providerName);
		logger.info("UpdateProvider {}", updateProvider.getName());
		
		{
			File dir = updateProvider.getFile("DUMMY").getParentFile();
			if (!dir.exists()) {
				dir.mkdirs();
			} else {
				if (!dir.isDirectory()) {
					logger.info("Not directory {}", dir.getAbsolutePath());
					throw new SecuritiesException("Not directory");
				}
			}
			
			// Remove unknown file
			File[] fileList = dir.listFiles();
			for(File file: fileList) {
				String name = file.getName();
				if (name.endsWith(".csv")) {
					String symbol = name.replace(".csv", "");
					if (StockUtil.contains(symbol)) continue;
				}
				
				logger.info("delete unknown file {}", name);
				file.delete();
			}
		}
		
		updateFile(updateProvider);
		logger.info("STOP");
	}
}
