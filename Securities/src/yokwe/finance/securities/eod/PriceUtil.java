package yokwe.finance.securities.eod;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class PriceUtil {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PriceUtil.class);

	private static Map<String, Map<String, Price>> priceMap = new TreeMap<>();
	
	public static boolean contains(String symbol, String date) {
		symbol = symbol.replace(".PR.", "-");
		
		if (!priceMap.containsKey(symbol)) {
			File file = Price.getFile(symbol);
			if (file.canRead()) {
				Map<String, Price> map = new TreeMap<>();
				for(Price price: Price.load(file)) {
					map.put(price.date, price);
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
	public static Price getPrice(String symbol, String date) {
		symbol = symbol.replace(".PR.", "-");

		if (!priceMap.containsKey(symbol)) {
			File file = Price.getFile(symbol);
			if (file.canRead()) {
				Map<String, Price> map = new TreeMap<>();
				for(Price price: Price.load(file)) {
					map.put(price.date, price);
				}
				priceMap.put(symbol, map);
			} else {
				logger.error("Unexpected symbol {}", symbol);
				throw new SecuritiesException("Unexpected symbol");
			}
		}
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
}
