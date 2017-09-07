package yokwe.finance.securities.eod;

import java.io.File;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class PriceUtil {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PriceUtil.class);

	private static Map<String, NavigableMap<String, Price>> priceMap = new TreeMap<>();
	
	public static boolean contains(String symbol, String date) {
		symbol = symbol.replace(".PR.", "-");
		
		if (!priceMap.containsKey(symbol)) {
			File file = Price.getFile(symbol);
			if (file.canRead()) {
				NavigableMap<String, Price> map = new TreeMap<>();
				for(Price price: Price.load(file)) {
					map.put(price.date, price);
				}
				if(map.isEmpty()) {
					logger.error("map is empty {}", symbol);
					throw new SecuritiesException("Unexpected");
				}
				priceMap.put(symbol, map);
			} else {
				logger.warn("no price file  {}", file.getPath());
				return false;
			}
		}
		Map<String, Price> map = priceMap.get(symbol);
		if (map.containsKey(date)) {
			return true;
		} else {
			logger.warn("no data in map  {}  {}", symbol, date);
			return false;
		}
	}
	
	private static void fillMap(String symbol) {
		if (priceMap.containsKey(symbol)) return;
		
		File file = Price.getFile(symbol);
		if (file.canRead()) {
			NavigableMap<String, Price> map = new TreeMap<>();
			for(Price price: Price.load(file)) {
				map.put(price.date, price);
			}
			if(map.isEmpty()) {
				logger.error("map is empty {}", symbol);
				throw new SecuritiesException("Unexpected");
			}
			priceMap.put(symbol, map);
		} else {
			logger.warn("no price file  {}", file.getPath());
			throw new SecuritiesException("Unexpected");
		}
	}
	public static Price getPrice(String symbol, String date) {
		symbol = symbol.replace(".PR.", "-");

		fillMap(symbol);
		Map<String, Price> map = priceMap.get(symbol);
		if (map.containsKey(date)) {
			return map.get(date);
		} else {
			logger.error("Unexpected {}  {}  {}", symbol, date, map.size());
			throw new SecuritiesException("Unexpected");
		}
	}
	public static double getClose(String symbol, String date) {
		return getPrice(symbol, date).close;
	}
	
	public static Price getLastPrice(String symbol) {
		symbol = symbol.replace(".PR.", "-");

		fillMap(symbol);
		NavigableMap<String, Price> map = priceMap.get(symbol);
		return map.lastEntry().getValue();
	}
}
