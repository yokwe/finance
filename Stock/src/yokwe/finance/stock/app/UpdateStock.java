package yokwe.finance.stock.app;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.json.JsonObject;

import org.slf4j.LoggerFactory;

import yokwe.finance.stock.UnexpectedException;
import yokwe.finance.stock.data.Stock;
import yokwe.finance.stock.iex.IEXBase;
import yokwe.finance.stock.iex.Stats;
import yokwe.finance.stock.iex.UpdateSymbols;
import yokwe.finance.stock.util.CSVUtil;

public class UpdateStock {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateStock.class);
	
	// Define data.Stock compatible class using iex.Stats
	public static class IEX extends IEXBase {
		public static final String TYPE = Stats.TYPE;
		
		@IgnoreField
		public String symbol;
		@JSONName("companyName")
		public String name;

		public double beta;
		public double week52high;
		public double week52low;
		public double week52change;
		public long   shortInterest;
		public String shortDate;
		public double dividendRate;
		public double dividendYield;
		public String exDividendDate;
		
		IEX() {}
		
		public IEX(JsonObject jsonObject) {
			super(jsonObject);
		}
		
		public static Map<String, IEX> getStock(String... symbols) {
			Map<String, IEX> ret = IEXBase.getStockObject(IEX.class, symbols);
			return ret;
		}
	}
	
	public static final String TYPE = "stock";
	
	public static final String PATH_DATA_DIR = "tmp/data";
	
	public static final int DELTA = IEXBase.MAX_PARAM;
	
	public static String getCSVPath() {
		return String.format("%s/%s.csv", PATH_DATA_DIR, TYPE);
	}
	
	public static List<Stock> load() {
		return CSVUtil.loadWithHeader(getCSVPath(), Stock.class);
	}
	public static List<Stock> load(File file) {
		return CSVUtil.loadWithHeader(file.getPath(), Stock.class);
	}
	
	public static void save(List<Stock> dataList, File file) {
		CSVUtil.saveWithHeader(dataList, file.getPath());
	}
	
	private static List<Stock> stockListCache = null;
	public static List<Stock> getStockList() {
		if (stockListCache == null) {
			stockListCache = load();
			Collections.sort(stockListCache);
		}
		return stockListCache;
	}
	private static List<String> symbolListCache = null;
	public static List<String> getSymbolList() {
		if (symbolListCache == null) {
			symbolListCache = getStockList().stream().map(o -> o.symbol).collect(Collectors.toList());
			Collections.sort(symbolListCache);
		}
		return symbolListCache;
	}
	// This methods update end of day csv in tmp/data directory.
	public static void main(String[] args) {
		logger.info("START");
		
		// To update symbolList, invoke UpdateSymbols
		UpdateSymbols.main(new String[0]);
		
		final List<String> symbolList;

		{
			Set<String> symbolSet = new TreeSet<>();
			
			// Add candidate from UpdateSymbols.getSymbolList()
			{
				for(String symbol: UpdateSymbols.getSymbolList()) {
					// Remove suffix for Called
					if (symbol.endsWith("*")) {
						symbol = symbol.substring(0, symbol.length() - 1);
					}
					// Remove suffix for When Issued
					if (symbol.endsWith("#")) {
						symbol = symbol.substring(0, symbol.length() - 1);
					}
					symbolSet.add(symbol);
				}
			}
			
			// Add candidate from price-delisted
			{
				String dirPath = UpdatePrice.getDelistedCSVDir(".");
				File dir = new File(dirPath);
				for(File file: dir.listFiles(f -> f.isFile() && f.getName().contains(".csv-"))) {
					// ACSF.csv-20180828-121551
					//     12345678901234567890
					String name = file.getName();
					String symbol = name.substring(0, name.length() - 20);
					String suffix = name.substring(name.length() - 20, name.length());
					if (!suffix.startsWith(".csv")) {
						logger.error("Unexpected {}", suffix);
						throw new UnexpectedException("Unexpected");
					}
					
					// Remove suffix for Called
					if (symbol.endsWith("*")) {
						symbol = symbol.substring(0, symbol.length() - 1);
					}
					// Remove suffix for When Issued
					if (symbol.endsWith("#")) {
						symbol = symbol.substring(0, symbol.length() - 1);
					}

					symbolSet.add(symbol);
				}
			}

			symbolList = new ArrayList<>(symbolSet);
		}
		logger.info("symbolList {}", symbolList.size());
		
		// Update csv file
		{
			int symbolListSize = symbolList.size();
			
			int countGet  = 0;
			int countData = 0;

			List<IEX> dataList = new ArrayList<>();

			for(int i = 0; i < symbolListSize; i += DELTA) {
				int fromIndex = i;
				int toIndex = Math.min(fromIndex + DELTA, symbolListSize);
				
				List<String> getList = new ArrayList<>();
				for(String symbol: symbolList.subList(fromIndex, toIndex)) {
					getList.add(symbol);
				}
				if (getList.isEmpty()) continue;
				if (getList.size() == 1) {
					logger.info("  {}", String.format("%4d  %3d %-7s", fromIndex, getList.size(), getList.get(0)));
				} else {
					logger.info("  {}", String.format("%4d  %3d %-7s - %-7s", fromIndex, getList.size(), getList.get(0), getList.get(getList.size() - 1)));
				}
				countGet += getList.size();

				Map<String, IEX> dataMap = IEX.getStock(getList.toArray(new String[0]));
				for(Map.Entry<String, IEX> entry: dataMap.entrySet()) {
					IEX iex = entry.getValue();
					iex.symbol = entry.getKey();
					if (iex.exDividendDate.equals("0")) {
						iex.exDividendDate = "";
					} else if (iex.exDividendDate.endsWith(" 00:00:00.0")) {
						iex.exDividendDate = iex.exDividendDate.substring(0, 10);
					} else {
						logger.error("Unexpected exDividendDate {}", iex.exDividendDate);
						throw new UnexpectedException("Unexpected exDividendDate");
					}
				}
				dataList.addAll(dataMap.values());
				countData += dataMap.size();
			}
			
			CSVUtil.saveWithHeader(dataList, getCSVPath());
			
			logger.info("count {} / {}", countData, countGet);
		}

		logger.info("STOP");
	}
}
