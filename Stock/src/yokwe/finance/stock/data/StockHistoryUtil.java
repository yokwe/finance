package yokwe.finance.stock.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import yokwe.finance.stock.util.CSVUtil;

public class StockHistoryUtil {
	public static final String PATH_STOCK_HISTORY_MONEX     = "tmp/monex/stock-history-monex.csv";
	public static final String PATH_STOCK_HISTORY_FIRSTRADE = "tmp/firstrade/stock-history-firstrade.csv";
	//                group
	public static Map<String, List<StockHistory>> getStockHistoryMap(String pathBase, String fileName) {
		String path = String.format("%s/%s", pathBase, fileName);
		List<StockHistory> stockHistoryList = CSVUtil.loadWithHeader(path, StockHistory.class);
		
		Map<String, List<StockHistory>> ret = new TreeMap<>();
		
		for(StockHistory stockHistory: stockHistoryList) {
			String key = stockHistory.group;
			if (!ret.containsKey(key)) {
				ret.put(key, new ArrayList<>());
			}
			ret.get(key).add(stockHistory);
		}
		
		for(Map.Entry<String, List<StockHistory>> entry: ret.entrySet()) {
			Collections.sort(entry.getValue());
		}
		
		return ret;
	}
}
