package yokwe.finance.securities.eod;

import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class PriceUtil {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PriceUtil.class);

	private static Map<String, Map<String, Price>> priceMap = new TreeMap<>();
	
	public static double getPrice(String symbol, String date) {
		if (!priceMap.containsKey(symbol)) {
			Map<String, Price> map = new TreeMap<>();
			for(Price price: Price.load(symbol)) {
				map.put(price.date, price);
			}
			priceMap.put(symbol, map);
		}
		Map<String, Price> map = priceMap.get(symbol);
		if (map.containsKey(date)) {
			Price price = map.get(date);
			return price.close;
		} else {
			logger.info("Unexpected date = {}", date);
			throw new SecuritiesException("Unexpected");
		}
	}
}
