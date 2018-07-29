package yokwe.finance.securities.iex;

import java.io.StringReader;
import java.time.LocalDateTime;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.util.HttpUtil;

public class Quote extends IEXBase {
	public String        symbol;                // refers to the stock ticker.
	public String        companyName;           // refers to the company name.
	public String        primaryExchange;       // refers to the primary listings exchange.
	public String        sector;                // refers to the sector of the stock.
	public String        calculationPrice;      // refers to the source of the latest price. ("tops", "sip", "previousclose" or "close")
	public double        open;                  // refers to the official open price
	public LocalDateTime openTime;              // refers to the official listing exchange time for the open
	public double        close;                 // refers to the official close price
	public LocalDateTime closeTime;             // refers to the official listing exchange time for the close
	public double        high;                  // refers to the market-wide highest price from the SIP. 15 minute delayed
	public double        low;                   // refers to the market-wide lowest price from the SIP. 15 minute delayed
	public double        latestPrice;           // refers to the latest price being the IEX real time price, the 15 minute delayed market price, or the previous close price.
	public String        latestSource;          // refers to the source of latestPrice. ("IEX real time price", "15 minute delayed price", "Close" or "Previous close")
	public String        latestTime;            // refers to a human readable time of the latestPrice. The format will vary based on latestSource.
	public LocalDateTime latestUpdate;          // refers to the update time of latestPrice in milliseconds since midnight Jan 1, 1970.
	public long          latestVolume;          // refers to the total market volume of the stock.
	public double        iexRealtimePrice;      // refers to last sale price of the stock on IEX. (Refer to the attribution section above.)
	public double        iexRealtimeSize;       // refers to last sale size of the stock on IEX.
	public LocalDateTime iexLastUpdated;        // refers to the last update time of the data in milliseconds since midnight Jan 1, 1970 UTC or -1 or 0. If the value is -1 or 0, IEX has not quoted the symbol in the trading day.
	public double        delayedPrice;          // refers to the 15 minute delayed market price during normal market hours 9:30 - 16:00.
	public LocalDateTime delayedPriceTime;      // refers to the time of the delayed market price during normal market hours 9:30 - 16:00.
	public double        extendedPrice;         // refers to the 15 minute delayed market price outside normal market hours 8:00 - 9:30 and 16:00 - 17:00.
	public double        extendedChange;        // is calculated using extendedPrice from calculationPrice.
	public double        extendedChangePercent; // is calculated using extendedPrice from calculationPrice.
	public LocalDateTime extendedPriceTime;     // refers to the time of the delayed market price outside normal market hours 8:00 - 9:30 and 16:00 - 17:00.
	public double        previousClose;         // refers to the official close price of previous trading day
	public double        change;                // is calculated using calculationPrice from previousClose.
	public double        changePercent;         // is calculated using calculationPrice from previousClose.
	public double        iexMarketPercent;      // refers to IEX’s percentage of the market in the stock.
	public long          iexVolume;             // refers to shares traded in the stock on IEX.
	public long          avgTotalVolume;        // refers to the 30 day average volume on all markets.
	public double        iexBidPrice;           // refers to the best bid price on IEX.
	public double        iexBidSize;            // refers to amount of shares on the bid on IEX.
	public double        iexAskPrice;           // refers to the best ask price on IEX.
	public double        iexAskSize;            // refers to amount of shares on the ask on IEX.
	public long          marketCap;             // is calculated in real time using calculationPrice.
	public double        peRatio;               // is calculated in real time using calculationPrice.
	public double        week52High;            // refers to the adjusted 52 week high.
	public double        week52Low;             // refers to the adjusted 52 week low.
	public double        ytdChange;             // refers to the price change percentage from start of year to previous close.
	
	Quote() {
		symbol                = null;
		companyName           = null;
		primaryExchange       = null;
		sector                = null;
		calculationPrice      = null;
		open                  = 0;
		openTime              = null;
		close                 = 0;
		closeTime             = null;
		high                  = 0;
		low                   = 0;
		latestPrice           = 0;
		latestSource          = null;
		latestTime            = null;
		latestUpdate          = null;
		latestVolume          = 0;
		iexRealtimePrice      = 0;
		iexRealtimeSize       = 0;
		iexLastUpdated        = null;
		delayedPrice          = 0;
		delayedPriceTime      = null;
		extendedPrice         = 0;
		extendedChange        = 0;
		extendedChangePercent = 0;
		extendedPriceTime     = null;
		change                = 0;
		changePercent         = 0;
		iexMarketPercent      = 0;
		iexVolume             = 0;
		avgTotalVolume        = 0;
		iexBidPrice           = 0;
		iexBidSize            = 0;
		iexAskPrice           = 0;
		iexAskSize            = 0;
		marketCap             = 0;
		peRatio               = 0;
		week52High            = 0;
		week52Low             = 0;
		ytdChange             = 0;
	}
	
	Quote(JsonObject jsonObject) {
		super(jsonObject);
	}
	
	public static Quote getStock(String symbol) {
		String url = String.format("%s/stock/%s/quote", END_POINT, symbol);
		String jsonString = HttpUtil.downloadAsString(url);

		try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
			JsonObject jsonObject = reader.readObject();
			return new Quote(jsonObject);
		}
	}
	
	static void test(Logger logger) {
		String jsonString = "{\"symbol\":\"IBM\",\"companyName\":\"International Business Machines Corporation\",\"primaryExchange\":\"New York Stock Exchange\",\"sector\":\"Technology\",\"calculationPrice\":\"close\",\"open\":146.89,\"openTime\":1532698210193,\"close\":145.15,\"closeTime\":1532721693191,\"high\":147.14,\"low\":144.66,\"latestPrice\":145.15,\"latestSource\":\"Close\",\"latestTime\":\"July 27, 2018\",\"latestUpdate\":1532721693191,\"latestVolume\":3749662,\"iexRealtimePrice\":null,\"iexRealtimeSize\":null,\"iexLastUpdated\":null,\"delayedPrice\":145.15,\"delayedPriceTime\":1532721693191,\"extendedPrice\":144.99,\"extendedChange\":-0.16,\"extendedChangePercent\":-0.0011,\"extendedPriceTime\":1532724796346,\"previousClose\":146.71,\"change\":-1.56,\"changePercent\":-0.01063,\"iexMarketPercent\":null,\"iexVolume\":null,\"avgTotalVolume\":4762213,\"iexBidPrice\":null,\"iexBidSize\":null,\"iexAskPrice\":null,\"iexAskSize\":null,\"marketCap\":132521950000,\"peRatio\":10.36,\"week52High\":171.13,\"week52Low\":137.45,\"ytdChange\":-0.049811407713423766}";
		logger.info("json {}", jsonString);
		
		try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
			JsonObject jsonObject = reader.readObject();
			Quote quote = new Quote(jsonObject);
			logger.info("quote {}", quote.toString());
		}
	}
	
	public static void main(String[] args) {
		Logger logger = LoggerFactory.getLogger(Quote.class);
		logger.info("START");
		
		test(logger);
		
		{
			Quote quote = Quote.getStock("ibm");
			logger.info("quote {}", quote.toString());
		}

		logger.info("STOP");
	}
}
