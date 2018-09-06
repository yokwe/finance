package yokwe.finance.stock.iex;

import java.util.Map;

import javax.json.JsonObject;

public class Dividends extends IEXBase {
	public static final String TYPE = "dividends";

	public String exDate;       // refers to the dividend ex-date
	public String paymentDate;  // refers to the payment date
	public String recordDate;   // refers to the dividend record date
	public String declaredDate; // refers to the dividend declaration date
	public double amount;       // refers to the payment amount
	public String flag;         // refers to the dividend flag (FI = Final dividend, div ends or instrument ends,
								// LI = Liquidation, instrument liquidates,
								// PR = Proceeds of a sale of rights or shares,
								// RE = Redemption of rights,
								// AC = Accrued dividend,
								// AR = Payment in arrears,
								// AD = Additional payment,
								// EX = Extra payment,
								// SP = Special dividend,
								// YE = Year end,
								// UR = Unknown rate,
								// SU = Regular dividend is suspended)
	public String type;			// refers to the dividend payment type (Dividend income, Interest income, Stock dividend, Short term capital gain, Medium term capital gain, Long term capital gain, Unspecified term capital gain)
	public String qualified;	// refers to the dividend income type
								// P = Partially qualified income
								// Q = Qualified income
								// N = Unqualified income
								// null = N/A or unknown
	public String indicated; 	// refers to the indicated rate of the dividend
	
	public Dividends() {
		exDate       = null;
		paymentDate  = null;
		recordDate   = null;
		declaredDate = null;
		amount       = 0;
		flag         = null;
		type         = null;
		indicated    = null;
	}
	public Dividends(JsonObject jsonObject) {
		super(jsonObject);
	}
	
	public static Map<String, Dividends[]> getStock(Range range, String... symbols) {
		return IEXBase.getStockArray(Dividends.class, range, symbols);
	}

//	public static Dividends[] getStock(String symbol, Range range) {
//		String url = String.format("%s/stock/%s/%s/%s", END_POINT, symbol, TYPE, range.toString());
//		String jsonString = HttpUtil.downloadAsString(url);
//
//		try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
//			JsonArray jsonArray = reader.readArray();
//			int jsonArraySize = jsonArray.size();
//			
//			Dividends[] ret = new Dividends[jsonArraySize];
//			for(int i = 0; i < jsonArraySize; i++) {
//				JsonObject element = jsonArray.getJsonObject(i);
//				ret[i] = new Dividends(element);
//			}
//			
//			return ret;
//		}
//	}
//
//	static void test(Logger logger) {
//		String jsonString = "[{\"exDate\":\"2018-02-08\",\"paymentDate\":\"2018-03-10\",\"recordDate\":\"2018-02-09\",\"declaredDate\":\"2018-01-30\",\"amount\":1.5,\"flag\":\"FI\",\"type\":\"Dividend income\",\"qualified\":\"\",\"indicated\":\"\"},{\"exDate\":\"2017-11-09\",\"paymentDate\":\"2017-12-09\",\"recordDate\":\"2017-11-10\",\"declaredDate\":\"2017-10-31\",\"amount\":1.5,\"flag\":\"\",\"type\":\"Dividend income\",\"qualified\":\"\",\"indicated\":\"\"},{\"exDate\":\"2017-08-08\",\"paymentDate\":\"2017-09-09\",\"recordDate\":\"2017-08-10\",\"declaredDate\":\"2017-07-25\",\"amount\":1.5,\"flag\":\"\",\"type\":\"Dividend income\",\"qualified\":\"Q\",\"indicated\":\"\"},{\"exDate\":\"2017-05-08\",\"paymentDate\":\"2017-06-10\",\"recordDate\":\"2017-05-10\",\"declaredDate\":\"2017-04-25\",\"amount\":1.5,\"flag\":\"\",\"type\":\"Dividend income\",\"qualified\":\"Q\",\"indicated\":\"\"}]";
//		logger.info("json {}", jsonString);
//		
//		try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
//			JsonArray jsonArray = reader.readArray();
//			int jsonArraySize = jsonArray.size();
//			logger.info("jsonArraySize = {}", jsonArraySize);
//			
//			List<Dividends> result = new ArrayList<>();
//			for(int i = 0; i < jsonArraySize; i++) {
//				JsonObject element = jsonArray.getJsonObject(i);
//				Dividends dividends = new Dividends(element);
//				result.add(dividends);
//			}
//
//			logger.info("result ({}){}", result.size(), result.toString());
//		}
//	}
//
	
//	public static class EOD extends IEXBase {
//		public static final String TYPE = "dividends";
//		
//		// date,symbol,open,high,low,close,volume
//		@JSONName("paymentDate")
//		public String date;
//		@IgnoreField
//		public String symbol;
//		@JSONName("amount")
//		public double dividend;
//
//		EOD() {
//			date     = null;
//			symbol   = null;
//			dividend = 0;
//		}
//		
//		public EOD(JsonObject jsonObject) {
//			super(jsonObject);
//		}
//		
//		public static Map<String, EOD[]> getStock(Range range, String... symbols) {
//			Map<String, EOD[]> ret = IEXBase.getStockArray(EOD.class, range, symbols);
//			for(Map.Entry<String, EOD[]> entry: ret.entrySet()) {
//				String symbol = entry.getKey();
//				EOD[]  value  = entry.getValue();
//				for(int i = 0; i < value.length; i++) {
//					value[i].symbol = symbol;
//				}
//			}
//			return ret;
//		}
//	}
//
//	public static void main(String[] args) {
//		Logger logger = LoggerFactory.getLogger(Dividends.class);
//		logger.info("START");
//		
//		{
//			Map<String, EOD[]> dataMap = EOD.getStock(Range.Y1, "ibm", "bt");
//			logger.info("dataMap {}", dataMap.size());
//			for(Map.Entry<String, EOD[]> entry: dataMap.entrySet()) {
//				logger.info("  {} {}", entry.getKey(), Arrays.asList(entry.getValue()));
//			}
//		}
//
//		logger.info("STOP");
//	}
}
