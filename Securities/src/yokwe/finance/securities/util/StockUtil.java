package yokwe.finance.securities.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.eod.Stock;
import yokwe.finance.securities.eod.UpdateStock;

public class StockUtil {
	private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
	
	private static Map<String, Stock> map = new TreeMap<>();
	
	static {
		List<Stock> tableList = CSVUtil.loadWithHeader(UpdateStock.PATH_STOCK, Stock.class);
		for(Stock table: tableList) {
			map.put(table.symbol, table);
		}
	}
	
	public static boolean contains(String symbol) {
		return map.containsKey(symbol);
	}
	
	public static Stock get(String symbol) {
		if (!map.containsKey(symbol)) {
			logger.error("symbol = {}", symbol);
			throw new SecuritiesException();
		}
		return map.get(symbol);
	}
	
	public static Collection<Stock> getAll() {
		return map.values();
	}
	
	public static Map<String, Stock> getMap() {
		return map;
	}
}
