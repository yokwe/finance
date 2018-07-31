package yokwe.finance.securities.iex;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.iex.IEXBase.Range;
import yokwe.finance.securities.util.CSVUtil;

public class UpdateDividends {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateDividends.class);
	
	public static final String PATH_DIR = "tmp/iex/dividends";
	
	private static String getFilePath(String symbol) {
		return String.format("%s/%s.csv", PATH_DIR, symbol);
	}
	
	public static void main (String[] args) {
		logger.info("START");
		
		List<String> symbolList = UpdateSymbols.getSymbolList();
		int symbolListSize = symbolList.size();
		logger.info("symbolList {}", symbolList.size());
		
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
