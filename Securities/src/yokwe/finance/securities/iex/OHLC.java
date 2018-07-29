package yokwe.finance.securities.iex;

import java.io.StringReader;
import java.time.LocalDateTime;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.util.HttpUtil;

public class OHLC extends JSONBase {
	public static class Pair extends JSONBase {
		public double        price; // refers to the official open or close price
		public LocalDateTime time;  // refers to the official listing exchange time for the open or close
		
		public Pair() {
			price = 0;
			time  = null;
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
	OHLC(JsonObject jsonObject) {
		super(jsonObject);
	}
	
	public static OHLC get(String symbol) {
		String url = String.format("https://api.iextrading.com/1.0/stock/%s/ohlc", symbol);
		String jsonString = HttpUtil.downloadAsString(url);

		try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
			JsonObject jsonObject = reader.readObject();
			return new OHLC(jsonObject);
		}
	}

	static void test(Logger logger) {
		String jsonString = "{\"open\":{\"price\":146.89,\"time\":1532698210193},\"close\":{\"price\":145.15,\"time\":1532721693191},\"high\":147.14,\"low\":144.66}";
		logger.info("json {}", jsonString);
		
		try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
			JsonObject jsonObject = reader.readObject();
			OHLC ohlc = new OHLC(jsonObject);
			logger.info("ohlc {}", ohlc.toString());
		}
	}
	public static void main(String[] args) {
		Logger logger = LoggerFactory.getLogger(OHLC.class);
		logger.info("START");
		
		test(logger);
		
		{
			OHLC ohlc = OHLC.get("ibm");
			logger.info("ohlc {}", ohlc.toString());
		}

		logger.info("STOP");
	}
}
