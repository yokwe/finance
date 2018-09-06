package yokwe.finance.stock.iex;

import java.time.LocalDateTime;
import java.util.Map;

import javax.json.JsonObject;

public class OHLC extends IEXBase {
	public static class CSV implements Comparable<CSV> {
		public String        symbol;
		public double        open;
		public LocalDateTime openTime;
		public double        high;
		public double        low;
		public double        close;
		public LocalDateTime closeTime;
		
		public CSV(String symbol, double open, LocalDateTime openTime, double high, double low, double close, LocalDateTime closeTime) {
			this.symbol    = symbol;
			this.open      = open;
			this.openTime  = openTime;
			this.high      = high;
			this.low       = low;
			this.close     = close;
			this.closeTime = closeTime;
		}
		public CSV() {
			this("", 0, IEXBase.NULL_LOCAL_DATE_TIME, 0, 0, 0, IEXBase.NULL_LOCAL_DATE_TIME);
		}
		public CSV(String symbol, OHLC ohlc) {
			this.symbol    = symbol;
			this.open      = ohlc.open.price;
			this.openTime  = ohlc.open.time;
			this.high      = ohlc.high;
			this.low       = ohlc.low;
			this.close     = ohlc.close.price;
			this.closeTime = ohlc.close.time;
		}
		
		@Override
		public String toString() {
			return String.format("{symbol: %s, open: %s, openTime: %s, high: %s, low: %s, close: %s, closeTime: %s}",
					symbol, Double.toString(open), openTime, Double.toString(high), Double.toString(low), Double.toString(close), closeTime);
		}
		
		public OHLC toOHLC() {
			OHLC ohlc  = new OHLC();
			
			ohlc.open.price  = open;
			ohlc.open.time   = openTime;
			ohlc.high        = high;
			ohlc.low         = low;
			ohlc.close.price = close;
			ohlc.close.time  = closeTime;
			
			return ohlc;
		}
		
		@Override
		public int compareTo(CSV that) {
			return this.symbol.compareTo(that.symbol);
		}
	}
	
	public static final String TYPE = "ohlc";

	public static class Pair extends IEXBase {
		public static final String TYPE = "pair";
		
		public double        price; // refers to the official open or close price
		public LocalDateTime time;  // refers to the official listing exchange time for the open or close
		
		public Pair() {
			price = 0;
			time  = NULL_LOCAL_DATE_TIME;
		}
		Pair(JsonObject jsonObject) {
			super(jsonObject);
		}
	}
	
	public Pair   open;  // open price
	public Pair   close; // close price
	public double high;  // refers to the market-wide highest price from the SIP (15 minute delayed)
	public double low;   // refers to the market-wide lowest price from the SIP (15 minute delayed)

	public OHLC() {
		open  = new Pair();
		close = new Pair();
		high  = 0;
		low   = 0;
	}
	public OHLC(JsonObject jsonObject) {
		super(jsonObject);
	}
	
	public static Map<String, OHLC> getStock(String... symbols) {
		return IEXBase.getStockObject(OHLC.class, symbols);
	}

//	public static OHLC getStock(String symbol) {
//		String url = String.format("%s/stock/%s/%s", END_POINT, symbol, TYPE);
//		String jsonString = HttpUtil.downloadAsString(url);
//
//		try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
//			JsonObject jsonObject = reader.readObject();
//			return new OHLC(jsonObject);
//		}
//	}
//
//	static void test(Logger logger) {
//		String jsonString = "{\"open\":{\"price\":146.89,\"time\":1532698210193},\"close\":{\"price\":145.15,\"time\":1532721693191},\"high\":147.14,\"low\":144.66}";
//		logger.info("json {}", jsonString);
//		
//		try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
//			JsonObject jsonObject = reader.readObject();
//			OHLC ohlc = new OHLC(jsonObject);
//			logger.info("ohlc {}", ohlc.toString());
//		}
//	}
//	public static void main(String[] args) {
//		Logger logger = LoggerFactory.getLogger(OHLC.class);
//		logger.info("START");
//		
//		test(logger);
//		
//		{
//			OHLC ohlc = OHLC.getStock("ibm");
//			logger.info("ohlc {}", ohlc.toString());
//		}
//
//		logger.info("STOP");
//	}
}
