package yokwe.finance.stock.iex;

import java.io.StringReader;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.slf4j.LoggerFactory;

import yokwe.finance.stock.util.HttpUtil;

public class Previous extends IEXBase implements Comparable<Previous> {
	public static final String TYPE = "previous";

	public String symbol;           // refers to the stock ticker.
	public String date;             // refers to the date of the returned data in the format YYYY-MM-DD
	public double open;
	public double high;
	public double low;
	public double close;
	public long   volume;           // adjusted for splits
	public long   unadjustedVolume;
	public double change;
	public double changePercent;
	public double vwap;	            // Volume weighted average price
	
	public Previous() {
		symbol           = null;
		date             = null;
		open             = 0;
		high             = 0;
		low              = 0;
		close            = 0;
		volume           = 0;
		unadjustedVolume = 0;
		change           = 0;
		changePercent    = 0;
		vwap             = 0;
	}
	public Previous(JsonObject jsonObject) {
		super(jsonObject);
	}
	
	@Override
	public int compareTo(Previous that) {
		return this.symbol.compareTo(that.symbol);
	}
	
	public static Previous[] getMarket() {
		String url        = String.format("%s/stock/market/%s", END_POINT, TYPE);
		String jsonString = HttpUtil.downloadAsString(url);
		
//		logger.info("url {}", url);
//		logger.info("jsonString {}", jsonString);

		try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
			// Assume result is only one object
			JsonObject result     = reader.readObject();
			int        resultSize = result.size();
			Previous[] ret        = new Previous[resultSize];
			int i = 0;
			for(String resultKey: result.keySet()) {
				JsonValue  resultChild = result.get(resultKey);
				JsonObject element     = resultChild.asJsonObject();
				
				ret[i++] = new Previous(element);
			}
			
			return ret;
		}
	}

	public static Map<String, Previous> getStock(String... symbols) {
		return IEXBase.getStockObject(Previous.class, symbols);
	}
	
	public static void main(String[] args) {
		final org.slf4j.Logger logger = LoggerFactory.getLogger(Previous.class);

		logger.info("START");
		
		{
			Previous[] previous = Previous.getMarket();
			logger.info("previous {}", previous.length);
			for(int i = 0; i < previous.length; i++) {
				logger.info("  {} {}", i, previous[i]);			
			}
		}
	
		logger.info("STOP");
	}
}
