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

public class UpdateChart {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateChart.class);
	
	private static final int DELTA = 10;
	
	public static String getCSVPath(String symbol) {
		return IEXBase.getCSVPath(Chart.class, symbol);
	}
	public static String getDelistedCSVPath(String symbol, String suffix) {
		return IEXBase.getDelistedCSVPath(Chart.class, symbol, suffix);
	}
	
	public static void main (String[] args) {
		logger.info("START");
		
		List<String> symbolList = UpdateSymbols.getSymbolList();
		int symbolListSize = symbolList.size();
		logger.info("symbolList {}", symbolList.size());
		
		// Remove unknown file
		{
			
			File dir;
			{
				File csvFile = new File(getCSVPath("A"));
				dir = csvFile.getParentFile();
			}
			Set<String> symbolSet = new TreeSet<>(symbolList);
			
			File dirDelisted;
			{
				File delistedFile = new File(getDelistedCSVPath("A", "000"));
				dirDelisted = delistedFile.getParentFile();
			}
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
					
					File destFile = new File(getDelistedCSVPath(name, destFileSuffix));
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
		for(int i = 0; i < symbolListSize; i += DELTA) {
			int fromIndex = i;
			int toIndex = Math.min(fromIndex + DELTA, symbolListSize);
			
			List<String> getList = new ArrayList<>();
			for(String symbol: symbolList.subList(fromIndex, toIndex)) {
				File file = new File(IEXBase.getCSVPath(Chart.class, symbol));
				if (file.exists()) continue;
				getList.add(symbol);
			}
			if (getList.isEmpty()) continue;
			logger.info("  {} ({}){}", fromIndex, getList.size(), getList.toString());
			countGetStock += getList.size();
			
			Map<String, Chart[]> chartMap = Chart.getStock(Range.Y1, getList.toArray(new String[0]));
			for(Map.Entry<String, Chart[]>entry: chartMap.entrySet()) {
				List<Chart> dataList = Arrays.asList(entry.getValue());
				if (dataList.size() == 0) continue;
				CSVUtil.saveWithHeader(dataList, getCSVPath(entry.getKey()));
				countData++;
			}
		}

		logger.info("stats {} / {}", countData, countGetStock);
		logger.info("STOP");
	}
}
