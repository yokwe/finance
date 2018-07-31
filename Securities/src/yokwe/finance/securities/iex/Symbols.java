package yokwe.finance.securities.iex;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import yokwe.finance.securities.util.HttpUtil;

public class Symbols extends IEXBase implements Comparable<Symbols> {
	public static final String TYPE = "symbols";

	public String  symbol;    // refers to the symbol represented in Nasdaq Integrated symbology (INET).
	public String  name;      // refers to the name of the company or security.
	public String  date;      // refers to the date the symbol reference data was generated.
	public boolean isEnabled; // will be true if the symbol is enabled for trading on IEX.
	public String  type;      // refers to the common issue type (AD - ADR
	                   // RE - REIT
				       // CE - Closed end fund
				       // SI - Secondary Issue
				       // LP - Limited Partnerships
				       // CS - Common Stock
				       // ET - ETF)
	public String  iexId;     // unique ID applied by IEX to track securities through symbol changes.

	public Symbols() {
		symbol    = null;
		name      = null;
		date      = null;
		isEnabled = false;
		type      = null;
		iexId     = null;
	}
	public Symbols(JsonObject jsonObject) {
		super(jsonObject);
	}
	
	@Override
	public int compareTo(Symbols that) {
		return this.symbol.compareTo(that.symbol);
	}

	public static Symbols[] getRefData() {
		String url = String.format("%s/ref-data/%s", END_POINT, TYPE);
		String jsonString = HttpUtil.downloadAsString(url);

		try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
			JsonArray jsonArray = reader.readArray();
			int jsonArraySize = jsonArray.size();
			
			Symbols[] ret = new Symbols[jsonArraySize];
			for(int i = 0; i < jsonArraySize; i++) {
				JsonObject element = jsonArray.getJsonObject(i);
				ret[i] = new Symbols(element);
			}
			
			return ret;
		}
	}

//	static void test(Logger logger) {
//		String jsonString = "[{\"symbol\":\"VENUSDT\",\"name\":\"VeChain USD\",\"date\":\"2018-07-27\",\"isEnabled\":true,\"type\":\"crypto\",\"iexId\":10000015},{\"symbol\":\"XLMUSDT\",\"name\":\"Stellar Lumens USD\",\"date\":\"2018-07-27\",\"isEnabled\":true,\"type\":\"crypto\",\"iexId\":10000016},{\"symbol\":\"QTUMUSDT\",\"name\":\"Qtum USD\",\"date\":\"2018-07-27\",\"isEnabled\":true,\"type\":\"crypto\",\"iexId\":10000017}]";
//		logger.info("json {}", jsonString);
//		
//		try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
//			JsonArray jsonArray = reader.readArray();
//			int jsonArraySize = jsonArray.size();
//			logger.info("jsonArraySize = {}", jsonArraySize);
//			
//			List<Symbols> result = new ArrayList<>();
//			for(int i = 0; i < jsonArraySize; i++) {
//				JsonObject element = jsonArray.getJsonObject(i);
//				Symbols chart = new Symbols(element);
//				result.add(chart);
//			}
//
//			logger.info("result ({}){}", result.size(), result.toString());
//		}
//	}
//
//	public static void main(String[] args) {
//		Logger logger = LoggerFactory.getLogger(Symbols.class);
//		logger.info("START");
//		
//		test(logger);
//		
//		{
//			Symbols[] symbols = Symbols.getRefData();
//			logger.info("symbols {}", symbols.length);
////			logger.info("symbols {}", Arrays.asList(symbols).toString());
//		}
//
//		logger.info("STOP");
//	}
}
