package yokwe.finance.securities.iex;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.iex.IEXBase.Range;
import yokwe.finance.securities.util.CSVUtil;

public class UpdateDividends {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateDividends.class);
	
	public static final String PATH_DIR         = "tmp/iex/dividends";
	public static final String PATH_DELISTED_DIR = "tmp/iex/dividends-delisted";
	
	private static String getFilePath(String symbol) {
		return String.format("%s/%s.csv", PATH_DIR, symbol);
	}
	
	public static void main (String[] args) {
		logger.info("START");
		
		List<String> symbolList = UpdateSymbols.getSymbolList();
		int symbolListSize = symbolList.size();
		logger.info("symbolList {}", symbolList.size());
		
		// Remove unknown file
		{
			File dir = new File(PATH_DIR);
			Set<String> symbolSet = new TreeSet<>(symbolList);
			
			File dirDelisted = new File(PATH_DELISTED_DIR);
			if (!dirDelisted.exists()) {
				dirDelisted.mkdirs();
			}
			
			String destFileSuffix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
			List<File> fileList = Arrays.asList(dir.listFiles((d, name) -> (name.endsWith(".csv"))));
			fileList.sort((a, b) -> a.getName().compareTo(b.getName()));
			try {
				for(File file: fileList) {
					String name = file.getName();
					String symbol = name.replace(".csv", "");
					if (symbolSet.contains(symbol)) continue;
					
						File destFile = new File(PATH_DELISTED_DIR, String.format("%s-%s", name, destFileSuffix));
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
		
		int countGetStock = 0;
		int countData = 0;
		for(int i = 0; i < symbolListSize; i += IEXBase.MAX_PARAM) {
			int fromIndex = i;
			int toIndex = Math.min(fromIndex + IEXBase.MAX_PARAM, symbolListSize);
			
			List<String> getList = new ArrayList<>();
			for(String symbol: symbolList.subList(fromIndex, toIndex)) {
				File file = new File(getFilePath(symbol));
				if (file.exists()) continue;
				getList.add(symbol);
			}
			if (getList.isEmpty()) continue;
			logger.info("  {} ({}){}", fromIndex, getList.size(), getList.toString());
			countGetStock += getList.size();

			Map<String, Dividends[]> dividendsMap = Dividends.getStock(Range.Y1, getList.toArray(new String[0]));
			for(Map.Entry<String, Dividends[]>entry: dividendsMap.entrySet()) {
				List<Dividends> dataList = Arrays.asList(entry.getValue());
				if (dataList.size() == 0) continue;
				CSVUtil.saveWithHeader(dataList, getFilePath(entry.getKey()));
				countData++;
			}
		}

		logger.info("stats {} / {}", countData, countGetStock);
		logger.info("STOP");
	}
}
