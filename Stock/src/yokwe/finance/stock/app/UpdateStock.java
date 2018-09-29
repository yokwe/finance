package yokwe.finance.stock.app;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.json.JsonObject;

import org.slf4j.LoggerFactory;

import yokwe.finance.stock.data.Stock;
import yokwe.finance.stock.iex.Company;
import yokwe.finance.stock.iex.IEXBase;
import yokwe.finance.stock.iex.UpdateSymbols;
import yokwe.finance.stock.util.CSVUtil;

public class UpdateStock {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateStock.class);
	
	// Define data.Price compatible class using IEXBase
	public static class IEX extends IEXBase {
		public static final String TYPE = Company.TYPE;
		
		public String symbol;
		public String exchange;
		public String issueType;
		public String sector;
		public String industry;
		@JSONName("companyName")
		public String name;

		
		IEX() {
			symbol    = null;
			exchange  = null;
			issueType = null;
			sector    = null;
			industry  = null;
			name      = null;
		}
		
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
	
	private static List<Stock> stockList = null;
	public static List<Stock> getStockList() {
		if (stockList == null) {
			stockList = load();
			Collections.sort(stockList);
		}
		return stockList;
	}
	private static List<String> symbolList = null;
	public static List<String> getSymbolList() {
		if (symbolList == null) {
			symbolList = getStockList().stream().map(o -> o.symbol).collect(Collectors.toList());
			Collections.sort(symbolList);
		}
		return symbolList;
	}
	// This methods update end of day csv in tmp/data directory.
	public static void main(String[] args) {
		logger.info("START");
		
		// To update symbolList, invoke UpdateSymbols
		UpdateSymbols.main(new String[0]);
		
		List<String> symbolList = new ArrayList<>();
		for(String symbol: UpdateSymbols.getSymbolList()) {
			// Remove suffix for Called
			if (symbol.endsWith("*")) {
				symbolList.add(symbol.substring(0, symbol.length() - 1));
				continue;
			}
			// Remove suffix for When Issued
			if (symbol.endsWith("#")) {
				symbolList.add(symbol.substring(0, symbol.length() - 1));
				continue;
			}
			symbolList.add(symbol);
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
				dataList.addAll(dataMap.values());
				countData += dataMap.size();
			}
			
			CSVUtil.saveWithHeader(dataList, getCSVPath());
			
			logger.info("count {} / {}", countData, countGet);
		}

		logger.info("STOP");
	}
}
