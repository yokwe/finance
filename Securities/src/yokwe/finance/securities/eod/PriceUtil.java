package yokwe.finance.securities.eod;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class PriceUtil {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PriceUtil.class);

	private static Map<String, DateMap<Price>> priceMap = new TreeMap<>();
	
	public static boolean contains(String symbol, String date) {
		symbol = symbol.replace(".PR.", "-");
		
		fillMap(symbol);
		DateMap<Price> map = priceMap.get(symbol);
		if (map.containsKey(date)) {
			return true;
		} else {
			logger.warn("no data in map  {}  {}", date, symbol);
			return false;
		}
	}
	
	private static void fillMap(String symbol) {
		if (priceMap.containsKey(symbol)) return;
		
		File file = new File(UpdatePrice.getCSVPath(symbol));
		if (file.canRead()) {
			DateMap<Price> map = new DateMap<>();
			for(Price price: UpdatePrice.load(file)) {
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
		DateMap<Price> map = priceMap.get(symbol);
		if (!map.containsKey(date)) {
			logger.warn("no data in map  {}  {}", date, symbol);
		}
		return map.get(date);
	}
	public static double getClose(String symbol, String date) {
		return getPrice(symbol, date).close;
	}
	
	public static Price getLastPrice(String symbol) {
		symbol = symbol.replace(".PR.", "-");

		fillMap(symbol);
		DateMap<Price> map = priceMap.get(symbol);
		return map.getLast();
	}
}
