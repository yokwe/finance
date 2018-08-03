package yokwe.finance.securities.iex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.util.CSVUtil;

public class UpdateOHLC {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateOHLC.class);
	
	public static String getCSVPath() {
		return IEXBase.getCSVPath(OHLC.class);
	}

	public static void main (String[] args) {
		logger.info("START");
		
		List<String> symbolList = UpdateSymbols.getSymbolList();
		int symbolListSize = symbolList.size();
		logger.info("symbolList {}", symbolList.size());
		
		List<OHLC.CSV> dataList = new ArrayList<>();
		for(int i = 0; i < symbolListSize; i += IEXBase.MAX_PARAM) {
			int fromIndex = i;
			int toIndex = Math.min(fromIndex + IEXBase.MAX_PARAM, symbolListSize);
			List<String> getList = symbolList.subList(fromIndex, toIndex);
			logger.info("  {} {}", fromIndex, getList.toString());
			
			Map<String, OHLC> companyMap = OHLC.getStock(getList.toArray(new String[0]));
			for(Map.Entry<String, OHLC> entry: companyMap.entrySet()) {
				String symbol = entry.getKey();
				OHLC   ohlc   = entry.getValue();
				
				dataList.add(new OHLC.CSV(symbol, ohlc));
			}
		}
		logger.info("dataList {}", dataList.size());
		
		String csvPath = getCSVPath();
		logger.info("csvPath {}", csvPath);
		
		CSVUtil.saveWithHeader(dataList, csvPath);

		logger.info("STOP");
	}

}
