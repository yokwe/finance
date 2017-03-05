package yokwe.finance.securities.eod;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.FileUtil;

public class StockUtil {
	private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
	
	private static Map<String, Stock> map = new TreeMap<>();
	
	static {
		for(Stock table: Stock.load()) {
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
