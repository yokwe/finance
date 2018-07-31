package yokwe.finance.securities.iex;

import java.util.Map;

public class Price extends IEXBase {
	public static final String TYPE = "price";
	
	public double current; // A single number, being the IEX real time price, the 15 minute delayed market price, or the previous close price, is returned.
	
	Price(String current) {
		this.current = Double.valueOf(current);
	}
	
	public static Map<String, Price> getStock(String... symbols) {
		return IEXBase.getStockNumber(Price.class, symbols);
	}

//	public static Price getStock(String symbol) {
//		String url = String.format("%s/stock/%s/%s", END_POINT, symbol, TYPE);
//		String jsonString = HttpUtil.downloadAsString(url);
//		
//		return new Price(jsonString);
//	}
//
//	static void test(Logger logger) {
//		String jsonString = "145.15";
//		logger.info("json {}", jsonString);
//		
//		Price price = new Price(jsonString);
//		logger.info("price {}", price.toString());
//	}
//	public static void main(String[] args) {
//		Logger logger = LoggerFactory.getLogger(Price.class);
//		logger.info("START");
//		
//		test(logger);
//		
//		{
//			Price price = Price.getStock("ibm");
//			logger.info("price {}", price.toString());
//		}
//
//		logger.info("STOP");
//	}
}
