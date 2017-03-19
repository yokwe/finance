package yokwe.finance.securities.eod;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.DoubleUtil;
import yokwe.finance.securities.util.FileUtil;
import yokwe.finance.securities.util.HttpUtil;

public class UpdateDividend {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateDividend.class);
	
	public static class UpdateProviderYahoo implements UpdateProvider {
		private static final String PATH_DIR      = "tmp/eod/dividend";
		public String getName() {
			return YAHOO;
		}
		
		public File getFile(String symbol) {
			String path = String.format("%s-%s/%s.csv", PATH_DIR, getName(), symbol);
			File file = new File(path);
			return file;
		}

		public boolean updateFile(String exch, String symbol) {
			File file = getFile(symbol);
			
			Stock stock = StockUtil.get(symbol.replace(".PR.", "-"));

			// first
			int a = DATE_FIRST.getMonthValue(); // mm
			int b = DATE_FIRST.getDayOfMonth(); // dd
			int c = DATE_FIRST.getYear();       // yyyy
			// last
			int d = DATE_LAST.getMonthValue(); // mm
			int e = DATE_LAST.getDayOfMonth(); // dd
			int f = DATE_LAST.getYear();       // yyyy
			String url = String.format("http://real-chart.finance.yahoo.com/table.csv?s=%s&a=%02d&b=%02d&c=%04d&d=%02d&e=%02d&f=%04d&g=v&ignore=.csv", stock.symbolYahoo, a - 1, b, c, d - 1, e, f);
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
			String YAHOO_DIVIDEND_HEADER = "Date,Dividends";
			String header = lines[0];
			if (!header.equals(YAHOO_DIVIDEND_HEADER)) {
				logger.error("Unexpected header  {}", header);
				throw new SecuritiesException("Unexpected header");
			}

			List<Dividend> dividendList = new ArrayList<>();
			
			for(String line: lines) {
				if (line.startsWith(YAHOO_DIVIDEND_HEADER)) continue;
				
				String[] values = line.split(",");
				if (values.length != 2) {
					logger.error("Unexpected line  {}", line);
					throw new SecuritiesException("Unexpected header");
				}
				
				String date     = values[0];
				double dividend = DoubleUtil.round(Double.valueOf(values[1]), 4);
				
				dividendList.add(new Dividend(date, symbol, dividend));
			}
			
			if (0 < dividendList.size()) {
				Dividend.save(dividendList, file);
				return true;
			} else {
				// no data
				// file.delete(); // keep old file
				return false;
			}
		}
	}
	
	private static Map<String, UpdateProvider> updateProviderMap = new TreeMap<>();
	static {
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

	private static boolean needUpdate(File file) {
		String content = FileUtil.read(file);
		String[] lines = content.split("\n");
		
		if (content.length() == 0) return true;
		
		// Sanity check
		if (lines.length <= 1) {
			logger.error("Unexpected content {}", content);
			throw new SecuritiesException("Unexpected content");
		}
		
		// first line should be header
		String HEADER = "date,symbol,dividend";
		String header = lines[0];
		if (!header.equals(HEADER)) {
			logger.error("Unexpected header  {}", header);
			throw new SecuritiesException("Unexpected header");
		}
		// second line should be last data
		String line = lines[1];
		String[] values = line.split(",");
		if (values.length != 3) {
			logger.error("Unexpected line  {}", line);
			throw new SecuritiesException("Unexpected header");
		}
		String date = values[0];
		
		return !date.equals(UpdateProvider.DATE_LAST.toString());
	}
	
	// This methods update end of day csv in tmp/eod directory.
	public static void main(String[] args) {
		logger.info("START");
		
		logger.info("DATE_FIRST {}", UpdateProvider.DATE_FIRST);
		logger.info("DATE_LAST  {}", UpdateProvider.DATE_LAST);

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
		
		{
			Collection<Stock> stockCollection = StockUtil.getAll();
			
//			Collection<Stock> stockCollection = new ArrayList<>();
//			stockCollection.add(StockUtil.get("IBM"));
//			stockCollection.add(StockUtil.get("NYT"));
//			stockCollection.add(StockUtil.get("PEP"));
			
			int total = stockCollection.size();
			int count = 0;
			
			int countUpdate = 0;
			int countOld    = 0;
			int countSkip   = 0;
			int countNew    = 0;
			int countNone   = 0;
			
			int showInterval = 100;
			boolean showOutput;
			int lastOutputCount = -1;
			for(Stock stock: stockCollection) {
				String exch   = stock.exchange;
				String symbol = stock.symbol;

				int outputCount = count / showInterval;
				if (outputCount != lastOutputCount) {
					showOutput = true;
					lastOutputCount = outputCount;
				} else {
					showOutput = false;
				}

				count++;
				
				File file = updateProvider.getFile(symbol);
				if (file.exists()) {
					if (needUpdate(file)) {
						if (updateProvider.updateFile(exch, symbol)) {
							if (showOutput) logger.info("{}  update {}", String.format("%4d / %4d",  count, total), symbol);
							countUpdate++;
						} else {
							if (showOutput) logger.info("{}  old    {}", String.format("%4d / %4d",  count, total), symbol);
							countOld++;
						}
					} else {
						if (showOutput) logger.info("{}  skip   {}", String.format("%4d / %4d",  count, total), symbol);
						countSkip++;
					}
				} else {
					if (updateProvider.updateFile(exch, symbol)) {
						/*if (showOutput)*/ logger.info("{}  new    {}", String.format("%4d / %4d",  count, total), symbol);
						countNew++;
					} else {
						if (showOutput) logger.info("{}  none   {}", String.format("%4d / %4d",  count, total), symbol);
						countNone++;
					}
				}
			}
			logger.info("update {}", String.format("%4d", countUpdate));
			logger.info("old    {}", String.format("%4d", countOld));
			logger.info("skip   {}", String.format("%4d", countSkip));
			logger.info("new    {}", String.format("%4d", countNew));
			logger.info("none   {}", String.format("%4d", countNone));
			logger.info("total  {}", String.format("%4d", countUpdate + countOld + countSkip + countNew + countNone));
		}
		logger.info("STOP");
	}
}
