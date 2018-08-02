package yokwe.finance.securities.eod;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;

import javax.json.JsonObject;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.iex.DelayedQuote;
import yokwe.finance.securities.iex.IEXBase;

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
			Double delayedQuote = getDelayedQuote(symbol, date);
			
			if (delayedQuote != null) {
				Price data = new Price(date, symbol, 0, 0, 0, delayedQuote, 0);
				map.put(date, data);
				logger.warn("using delayedQuote  {}  {}  {}", date, symbol, delayedQuote);
			} else {
				logger.warn("no data in map  {}  {}", date, symbol);
			}
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
	
	public static class IEX extends IEXBase {
		public static final String TYPE = DelayedQuote.TYPE;
		
		public String        symbol;           // refers to the stock ticker.
		public double        delayedPrice;     // refers to the 15 minute delayed market price.
		public LocalDateTime delayedPriceTime; // refers to the time of the delayed market price.
		
		IEX() {
			symbol           = null;
			delayedPrice     = 0;
			delayedPriceTime = null;
		}
		
		public IEX(JsonObject jsonObject) {
			super(jsonObject);
		}
		
		public static Map<String, IEX> getStock(String... symbols) {
			Map<String, IEX> ret = IEXBase.getStockObject(IEX.class, symbols);
			return ret;
		}
	
		
	}
	
	private static Map<String, Double> delayedQuoteMap = new TreeMap<>();
	public static Double getDelayedQuote(String symbol, String date) {
		String key = String.format("%s-%s", symbol, date);
		if (delayedQuoteMap.containsKey(key)) {
			return (double)delayedQuoteMap.get(key);
		}
		
		Map<String, IEX> result = IEX.getStock(symbol);
		if (!result.containsKey(symbol)) return null;

		IEX iex = result.get(symbol);
		String delayedPriceTime = iex.delayedPriceTime.toLocalDate().toString();
		if (!date.equals(delayedPriceTime)) return null;
		
		delayedQuoteMap.put(key, iex.delayedPrice);
		return iex.delayedPrice;
	}
}
