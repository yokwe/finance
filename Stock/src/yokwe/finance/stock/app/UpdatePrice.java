package yokwe.finance.stock.app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.json.JsonObject;

import org.slf4j.LoggerFactory;

import yokwe.finance.stock.UnexpectedException;
import yokwe.finance.stock.data.Market;
import yokwe.finance.stock.data.Price;
import yokwe.finance.stock.iex.Chart;
import yokwe.finance.stock.iex.IEXBase;
import yokwe.finance.stock.iex.IEXBase.Range;
import yokwe.finance.stock.util.CSVUtil;

public class UpdatePrice {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdatePrice.class);
	
	// Define data.Price compatible class using IEXBase
	public static class IEX extends IEXBase implements Comparable<IEX> {
		public static final String TYPE = Chart.TYPE;
		
		// date,symbol,open,high,low,close,volume
		public String date;
		@IgnoreField
		public String symbol;
		public double open;
		public double high;
		public double low;
		public double close;
		public long   volume;
		
		IEX() {
			date   = null;
			symbol = null;
			open   = 0;
			high   = 0;
			low    = 0;
			close  = 0;
			volume = 0;
		}
		
		public IEX(JsonObject jsonObject) {
			super(jsonObject);
		}

		@Override
		public int compareTo(IEX that) {
			int ret = this.symbol.compareTo(that.symbol);
			if (ret == 0) ret = this.date.compareTo(that.date);
			return ret;
		}
		
		public static Map<String, IEX[]> getStock(Range range, String... symbols) {
			Map<String, IEX[]> ret = IEXBase.getStockArray(IEX.class, range, symbols);
			for(Map.Entry<String, IEX[]> entry: ret.entrySet()) {
				String symbol = entry.getKey();
				IEX[]  value  = entry.getValue();
				for(int i = 0; i < value.length; i++) {
					value[i].symbol = symbol;
				}
			}
			return ret;
		}
	}
	
	public static final String TYPE = "price";
	
	public static final String PATH_DATA_DIR = "tmp/data";
	
	public static final String PATH_DELISTED_DIR = "tmp/data/delisted";
	
	public static final Range UPDATE_RANGE = Range.Y1;
	
	public static final int DELTA = 100;
	
	public static String getCSVDir(String basePath) {
		return String.format("%s/%s/%s", basePath, PATH_DATA_DIR, TYPE);
	}

	public static String getCSVPath(String basePath, String symbol) {
		return String.format("%s/%s.csv", getCSVDir(basePath), symbol);
	}
	public static String getDelistedCSVDir(String basePath) {
		return String.format("%s/%s/%s-delisted", basePath, PATH_DATA_DIR, TYPE);
	}
	public static String getDelistedCSVPath(String basePath, String symbol, String suffix) {
		return String.format("%s/%s.csv-%s", getDelistedCSVDir(basePath), symbol, suffix);
	}
	
	public static List<Price> load(String basePath, String symbol) {
		return CSVUtil.loadWithHeader(getCSVPath(basePath, symbol), Price.class);
	}
	public static List<Price> load(File file) {
		return CSVUtil.loadWithHeader(file.getPath(), Price.class);
	}
	
	public static void save(List<Price> dataList, File file) {
		CSVUtil.saveWithHeader(dataList, file.getPath());
	}
	
	// This methods update end of day csv in tmp/data directory.
	public static void main(String[] args) {
		String basePath = ".";
		
		logger.info("START");
		
		logger.info("UPDATE_RANGE {}", UPDATE_RANGE.toString());
		
		String lastTradingDateM0 = Market.getLastTradingDate().toString();
		logger.info("lastTradingDateM0 {}", lastTradingDateM0);

		String lastTradingDateM1 = Market.getPreviousTradeDate(Market.getLastTradingDate()).toString();
		logger.info("lastTradingDateM1 {}", lastTradingDateM1);

		List<String> symbolList = UpdateStock.getSymbolList();
		logger.info("symbolList {}", symbolList.size());
		
		{
			File dir = new File(getCSVDir(basePath));			
			if (!dir.exists()) {
				dir.mkdirs();
			}
			
			File dirDelisted = new File(getDelistedCSVDir(basePath));
			if (!dirDelisted.exists()) {
				dirDelisted.mkdirs();
			}
		}
		
		Set<String> delistedSymbolSet = new TreeSet<>();
		{
			File dir = new File(PATH_DELISTED_DIR);
			for(File file: dir.listFiles()) {
				String name = file.getName();

				// Sanity checks
				if (!name.endsWith(".csv")) {
					logger.warn("Not CSV file {}", file.getPath());
					continue;
				}
				if (file.length() == 0) {
					logger.warn("Empty file {}", file.getPath());
					continue;
				}
				
				String symbol = name.substring(0, name.length() - 4); // minus 4 for ".csv"
				delistedSymbolSet.add(symbol);
			}
		}
		
		Set<String> updatedSymbolSet = new TreeSet<>();

		// Update csv file
		{
			int symbolListSize = symbolList.size();
			
			int countGet  = 0;
			int countData = 0;
			int countHasLastTradingDateM0 = 0;
			int countHasLastTradingDateM1 = 0;
			
			for(int i = 0; i < symbolListSize; i += DELTA) {
				int fromIndex = i;
				int toIndex = Math.min(fromIndex + DELTA, symbolListSize);
				
				List<String> getList = new ArrayList<>();
				for(String symbol: symbolList.subList(fromIndex, toIndex)) {
//					File file = new File(getCSVPath(symbol));
//					if (file.exists()) continue;
					getList.add(symbol);
				}
				if (getList.isEmpty()) continue;
				if (getList.size() == 1) {
					logger.info("  {}", String.format("%4d  %3d %-7s", fromIndex, getList.size(), getList.get(0)));
				} else {
					logger.info("  {}", String.format("%4d  %3d %-7s - %-7s", fromIndex, getList.size(), getList.get(0), getList.get(getList.size() - 1)));
				}
				countGet += getList.size();

				Map<String, IEX[]> dataMap = IEX.getStock(UPDATE_RANGE, getList.toArray(new String[0]));
				for(Map.Entry<String, IEX[]>entry: dataMap.entrySet()) {
					List<IEX> dataList = Arrays.asList(entry.getValue());
					if (dataList.size() == 0) continue;
					Collections.sort(dataList);
					if (dataList.stream().filter(o -> o.date.equals(lastTradingDateM0)).count() != 0) countHasLastTradingDateM0++;
					if (dataList.stream().filter(o -> o.date.equals(lastTradingDateM1)).count() != 0) countHasLastTradingDateM1++;
					CSVUtil.saveWithHeader(dataList, getCSVPath(basePath, entry.getKey()));
					countData++;
					
					updatedSymbolSet.add(entry.getKey());
				}
				
				// Too few update
				if (100 < countGet && countHasLastTradingDateM0 < (countHasLastTradingDateM1 / 2)) {
					logger.error("Too few update  {} - {} / {} / {}", countHasLastTradingDateM1, countHasLastTradingDateM0, countData, countGet);
					throw new UnexpectedException("Too few update");
				}
			}
			
			// Remove not updatedSymbol
			
			logger.info("count {} - {} / {} / {}", countHasLastTradingDateM1, countHasLastTradingDateM0, countData, countGet);
		}

		// Copy csv files from PATH_DELISTED_DIR
		{
			try {
				for(String symbol: delistedSymbolSet) {
					logger.info("copy delisted  {}", symbol);
					File source = new File(String.format("%s/%s.csv", PATH_DELISTED_DIR, symbol));
					File target = new File(getCSVPath(basePath, symbol));
					
					Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (IOException e) {
				logger.info("IOException {}", e.getMessage());
				throw new UnexpectedException("IOException");
			}
		}

		// Remove not updated symbol file
		{
			String suffix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
			
			// Make list of csv file.
			File dir = new File(getCSVDir(basePath));			
			List<File> fileList = Arrays.asList(dir.listFiles((d, name) -> (name.endsWith(".csv"))));
			fileList.sort((a, b) -> a.getName().compareTo(b.getName()));
			try {
				for(File file: fileList) {
					String symbol = file.getName().replace(".csv", "");
					if (updatedSymbolSet.contains(symbol)) continue;
					if (delistedSymbolSet.contains(symbol)) continue;
					
					File destFile = new File(getDelistedCSVPath(basePath, symbol, suffix));
					logger.info("move not updated file {} to {}", file.getPath(), destFile.getPath());
										
					// Copy file to new location
					Files.copy(file.toPath(), destFile.toPath());
					// Delete file after successful copy
					file.delete();
				}
			} catch (IOException e) {
				logger.error("IOException {}", e.toString());
				throw new UnexpectedException("IOException");
			}
		}

		logger.info("STOP");
	}
}
