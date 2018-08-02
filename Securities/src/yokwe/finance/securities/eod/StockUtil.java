package yokwe.finance.securities.eod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class StockUtil {
	private static final Logger logger = LoggerFactory.getLogger(StockUtil.class);
	
	private static Map<String, Stock> map        = new TreeMap<>();
	private static List<String>       symbolList = new ArrayList<>();
	
	static {
		for(Stock stock: UpdateStock.load()) {
			map.put(stock.symbol, stock);
			symbolList.add(stock.symbol);
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
	
	public static List<String> getSymbolList() {
		return symbolList;
	}
}
