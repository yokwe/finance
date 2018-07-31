package yokwe.finance.securities.iex;

import java.time.LocalDateTime;
import java.util.Map;

import javax.json.JsonObject;

public class DelayedQuote extends IEXBase {
	public static final String TYPE = "delayed-quote";

	public String        symbol;           // refers to the stock ticker.
	public double        delayedPrice;     // refers to the 15 minute delayed market price.
	public double        high;             // refers to the 15 minute delayed high price.
	public double        low;              // refers to the 15 minute delayed low price.
	public long          delayedSize;      // refers to the 15 minute delayed last trade size.
	public LocalDateTime delayedPriceTime; // refers to the time of the delayed market price.
	public LocalDateTime processedTime;    // refers to when IEX processed the SIP price.

	public DelayedQuote() {
		symbol           = null;
		delayedPrice     = 0;
		delayedSize      = 0;
		delayedPriceTime = null;
		processedTime    = null;
	}
	public DelayedQuote(JsonObject jsonObject) {
		super(jsonObject);
	}

	public static Map<String, DelayedQuote> getStock(String... symbols) {
		return IEXBase.getStockObject(DelayedQuote.class, symbols);
	}
}
