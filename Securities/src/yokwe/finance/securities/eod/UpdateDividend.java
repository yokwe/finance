package yokwe.finance.securities.eod;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.JsonObject;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.iex.IEXBase.Range;
import yokwe.finance.securities.util.CSVUtil;
import yokwe.finance.securities.iex.Dividends;
import yokwe.finance.securities.iex.IEXBase;

public class UpdateDividend {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateDividend.class);
	
	// Define eod.Dividend compatible class using IEXBase
	public static class IEX extends IEXBase {
		public static final String TYPE = Dividends.TYPE;
		
		@JSONName("paymentDate")
		public String date;
		@IgnoreField
		public String symbol;
		@JSONName("amount")
		public double dividend;

		IEX() {
			date     = null;
			symbol   = null;
			dividend = 0;
		}
		
		public IEX(JsonObject jsonObject) {
			super(jsonObject);
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
	
	public static final String TYPE = "dividend";
	
	public static final String PATH_DATA_DIR = "tmp/eod";
	
	public static final Range UPDATE_RANGE = Range.Y1;
	
	public static final int DELTA = IEXBase.MAX_PARAM;
	
	public static String getCSVDir() {
		return String.format("%s/%s", PATH_DATA_DIR, TYPE);
	}
	public static String getCSVPath(String symbol) {
		return String.format("%s/%s.csv", getCSVDir(), symbol);
	}
	public static String getDelistedCSVDir() {
		return String.format("%s/%s-delisted", PATH_DATA_DIR, TYPE);
	}
	public static String getDelistedCSVPath(String symbol, String suffix) {
		return String.format("%s/%s.csv-%s", getDelistedCSVDir(), symbol, suffix);
	}
	
	public static List<Dividend> load(String symbol) {
		return CSVUtil.loadWithHeader(getCSVPath(symbol), Dividend.class);
	}
	public static List<Dividend> load(File file) {
		return CSVUtil.loadWithHeader(file.getPath(), Dividend.class);
	}
	
	public static void save(List<Dividend> dataList, File file) {
		CSVUtil.saveWithHeader(dataList, file.getPath());
	}

	// This methods update end of day csv in tmp/eod directory.
	public static void main(String[] args) {
		logger.info("START");
		
		logger.info("UPDATE_RANGE {}", UPDATE_RANGE.toString());

		List<String> symbolList = UpdateStock.getSymbolList();
		logger.info("symbolList {}", symbolList.size());
		
		// Remove unknown file
		{
			Set<String> symbolSet = new HashSet<>(symbolList);
			
			File dir = new File(getCSVDir());			
			if (!dir.exists()) {
				dir.mkdirs();
			}
			
			File dirDelisted = new File(getDelistedCSVDir());
			if (!dirDelisted.exists()) {
				dirDelisted.mkdirs();
			}

			String suffix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
			
			// Make list of csv file.
			List<File> fileList = Arrays.asList(dir.listFiles((d, name) -> (name.endsWith(".csv"))));
			fileList.sort((a, b) -> a.getName().compareTo(b.getName()));
			try {
				for(File file: fileList) {
					String symbol = file.getName().replace(".csv", "");
					if (symbolSet.contains(symbol)) continue;
					
						File destFile = new File(getDelistedCSVPath(symbol, suffix));
						logger.info("move unknown file {} to {}", file.getPath(), destFile.getPath());
											
						// Copy file to new location
						Files.copy(file.toPath(), destFile.toPath());
						// Delete file after successful copy
						file.delete();
				}
			} catch (IOException e) {
				logger.error("IOException {}", e.toString());
				throw new SecuritiesException("IOException");
			}
		}
	
		// Update csv file
		{
			int symbolListSize = symbolList.size();
			
			int countGet  = 0;
			int countData = 0;

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
					CSVUtil.saveWithHeader(dataList, getCSVPath(entry.getKey()));
					countData++;
				}
			}
			
			logger.info("count {} / {}", countData, countGet);
		}

		logger.info("STOP");
	}
}
