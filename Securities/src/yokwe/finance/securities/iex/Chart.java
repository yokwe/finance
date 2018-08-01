package yokwe.finance.securities.iex;

import java.util.Arrays;
import java.util.Map;

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Chart extends IEXBase {
	public static class EOD extends IEXBase {
		public static final String TYPE = "chart";
		
		// date,symbol,open,high,low,close,volume
		public String date;
		@IgnoreField
		public String symbol;
		public double open;
		public double high;
		public double low;
		public double close;
		public long   volume;
		
		EOD() {
			date   = null;
			symbol = null;
			open   = 0;
			high   = 0;
			low    = 0;
			close  = 0;
			volume = 0;
		}
		
		public EOD(JsonObject jsonObject) {
			super(jsonObject);
		}
		
		public static Map<String, EOD[]> getStock(Range range, String... symbols) {
			Map<String, EOD[]> ret = IEXBase.getStockArray(EOD.class, range, symbols);
			for(Map.Entry<String, EOD[]> entry: ret.entrySet()) {
				String symbol = entry.getKey();
				EOD[]  value  = entry.getValue();
				for(int i = 0; i < value.length; i++) {
					value[i].symbol = symbol;
				}
			}
			return ret;
		}
	}

	public static final String TYPE = "chart";
	
	// Support all charts except 1d
	public double high;             // is available on all charts.
	public double low;              // is available on all charts.
	public long   volume;           // is available on all charts.
	public String label;            // is available on all charts. A variable formatted version of the date depending on the range. Optional convenience field.
	public double changeOverTime;   // is available on all charts. Percent change of each interval relative to first value. Useful for comparing multiple stocks.
	public String date;             // is available on all charts.
	public double open;             // is available on all charts.
	public double close;            // is available on all charts.
	public long   unadjustedVolume; // is not available on 1d chart.
	public double change;           // is not available on 1d chart.
	public double changePercent;    // is not available on 1d chart.
	public double vwap;             // is not available on 1d chart.
	
	public Chart() {
		high             = 0;
		low              = 0;
		volume           = 0;
		label            = null;
		changeOverTime   = 0;
		date             = null;
		open             = 0;
		close            = 0;
		unadjustedVolume = 0;
		change           = 0;
		changePercent    = 0;
		vwap             = 0;
	}
	public Chart(JsonObject jsonObject) {
		super(jsonObject);
	}
	
	public static Map<String, Chart[]> getStock(Range range, String... symbols) {
		return IEXBase.getStockArray(Chart.class, range, symbols);
	}
	
//	public static Chart[] getStock(String symbol, Range range) {
//		Logger logger = LoggerFactory.getLogger(Chart.class);
//
//		String url = String.format("%s/stock/%s/%s/%s", END_POINT, symbol, TYPE, range.toString());
//		String jsonString = HttpUtil.downloadAsString(url);
//		
//		logger.info("url {}", url);
//		logger.info("jsonString {}", jsonString);
//
//		try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
//			JsonArray jsonArray = reader.readArray();
//			int jsonArraySize = jsonArray.size();
//			
//			Chart[] ret = new Chart[jsonArraySize];
//			for(int i = 0; i < jsonArraySize; i++) {
//				JsonObject element = jsonArray.getJsonObject(i);
//				ret[i] = new Chart(element);
//			}
//			
//			return ret;
//		}
//	}
//
//	static void test(Logger logger) {
//		String jsonString = "[{\"date\":\"2018-06-29\",\"open\":140.32,\"high\":141.359,\"low\":139.61,\"close\":139.7,\"volume\":3753684,\"unadjustedVolume\":3753684,\"change\":-0.34,\"changePercent\":-0.243,\"vwap\":140.3891,\"label\":\"Jun 29\",\"changeOverTime\":0},{\"date\":\"2018-07-02\",\"open\":138.28,\"high\":140.22,\"low\":138.2,\"close\":139.86,\"volume\":3405642,\"unadjustedVolume\":3405642,\"change\":0.16,\"changePercent\":0.115,\"vwap\":139.5625,\"label\":\"Jul 2\",\"changeOverTime\":0.0011453113815320332},{\"date\":\"2018-07-03\",\"open\":140.65,\"high\":140.94,\"low\":139.3678,\"close\":139.57,\"volume\":1963232,\"unadjustedVolume\":1963232,\"change\":-0.29,\"changePercent\":-0.207,\"vwap\":140.1424,\"label\":\"Jul 3\",\"changeOverTime\":-0.0009305654974945989},{\"date\":\"2018-07-05\",\"open\":140.48,\"high\":141.43,\"low\":139.93,\"close\":141.43,\"volume\":3744730,\"unadjustedVolume\":3744730,\"change\":1.86,\"changePercent\":1.333,\"vwap\":140.9622,\"label\":\"Jul 5\",\"changeOverTime\":0.012383679312813303},{\"date\":\"2018-07-06\",\"open\":141.53,\"high\":142.94,\"low\":141.17,\"close\":142.48,\"volume\":2910426,\"unadjustedVolume\":2910426,\"change\":1.05,\"changePercent\":0.742,\"vwap\":142.4092,\"label\":\"Jul 6\",\"changeOverTime\":0.01989978525411597},{\"date\":\"2018-07-09\",\"open\":142.59,\"high\":144.72,\"low\":142.47,\"close\":144.39,\"volume\":3904652,\"unadjustedVolume\":3904652,\"change\":1.91,\"changePercent\":1.341,\"vwap\":144.0618,\"label\":\"Jul 9\",\"changeOverTime\":0.03357193987115246},{\"date\":\"2018-07-10\",\"open\":144.51,\"high\":145.59,\"low\":144.255,\"close\":144.71,\"volume\":3776991,\"unadjustedVolume\":3776991,\"change\":0.32,\"changePercent\":0.222,\"vwap\":144.741,\"label\":\"Jul 10\",\"changeOverTime\":0.03586256263421632},{\"date\":\"2018-07-11\",\"open\":144,\"high\":146.19,\"low\":144,\"close\":144.94,\"volume\":3526565,\"unadjustedVolume\":3526565,\"change\":0.23,\"changePercent\":0.159,\"vwap\":145.1344,\"label\":\"Jul 11\",\"changeOverTime\":0.03750894774516829},{\"date\":\"2018-07-12\",\"open\":145.85,\"high\":146.83,\"low\":145.74,\"close\":146.45,\"volume\":3119505,\"unadjustedVolume\":3119505,\"change\":1.51,\"changePercent\":1.042,\"vwap\":146.3615,\"label\":\"Jul 12\",\"changeOverTime\":0.04831782390837509},{\"date\":\"2018-07-13\",\"open\":146.45,\"high\":146.9799,\"low\":145.8,\"close\":145.9,\"volume\":3067638,\"unadjustedVolume\":3067638,\"change\":-0.55,\"changePercent\":-0.376,\"vwap\":146.3285,\"label\":\"Jul 13\",\"changeOverTime\":0.04438081603435947},{\"date\":\"2018-07-16\",\"open\":145.67,\"high\":145.79,\"low\":144.21,\"close\":145.46,\"volume\":3468817,\"unadjustedVolume\":3468817,\"change\":-0.44,\"changePercent\":-0.302,\"vwap\":145.2196,\"label\":\"Jul 16\",\"changeOverTime\":0.041231209735146886},{\"date\":\"2018-07-17\",\"open\":144.75,\"high\":145,\"low\":143.34,\"close\":143.49,\"volume\":5096741,\"unadjustedVolume\":5096741,\"change\":-1.97,\"changePercent\":-1.354,\"vwap\":143.875,\"label\":\"Jul 17\",\"changeOverTime\":0.02712956335003594},{\"date\":\"2018-07-18\",\"open\":143.51,\"high\":144.8,\"low\":142.7351,\"close\":144.52,\"volume\":6935288,\"unadjustedVolume\":6935288,\"change\":1.03,\"changePercent\":0.718,\"vwap\":143.8397,\"label\":\"Jul 18\",\"changeOverTime\":0.03450250536864726},{\"date\":\"2018-07-19\",\"open\":147.85,\"high\":150.54,\"low\":147.25,\"close\":149.24,\"volume\":14655804,\"unadjustedVolume\":14655804,\"change\":4.72,\"changePercent\":3.266,\"vwap\":149.0564,\"label\":\"Jul 19\",\"changeOverTime\":0.06828919112383694},{\"date\":\"2018-07-20\",\"open\":148.58,\"high\":148.86,\"low\":146.26,\"close\":146.35,\"volume\":6415972,\"unadjustedVolume\":6415972,\"change\":-2.89,\"changePercent\":-1.936,\"vwap\":147.1503,\"label\":\"Jul 20\",\"changeOverTime\":0.047602004294917726},{\"date\":\"2018-07-23\",\"open\":146.35,\"high\":146.7,\"low\":145.015,\"close\":145.7,\"volume\":3897892,\"unadjustedVolume\":3897892,\"change\":-0.65,\"changePercent\":-0.444,\"vwap\":145.6464,\"label\":\"Jul 23\",\"changeOverTime\":0.04294917680744453},{\"date\":\"2018-07-24\",\"open\":146.7,\"high\":147.04,\"low\":145.92,\"close\":146.38,\"volume\":3891625,\"unadjustedVolume\":3891625,\"change\":0.68,\"changePercent\":0.467,\"vwap\":146.4848,\"label\":\"Jul 24\",\"changeOverTime\":0.04781675017895495},{\"date\":\"2018-07-25\",\"open\":146.01,\"high\":146.65,\"low\":145.5,\"close\":146.62,\"volume\":3623182,\"unadjustedVolume\":3623182,\"change\":0.24,\"changePercent\":0.164,\"vwap\":146.149,\"label\":\"Jul 25\",\"changeOverTime\":0.0495347172512528},{\"date\":\"2018-07-26\",\"open\":147.43,\"high\":149.27,\"low\":146.63,\"close\":146.71,\"volume\":4778022,\"unadjustedVolume\":4778022,\"change\":0.09,\"changePercent\":0.061,\"vwap\":147.4952,\"label\":\"Jul 26\",\"changeOverTime\":0.05017895490336449},{\"date\":\"2018-07-27\",\"open\":146.89,\"high\":147.14,\"low\":144.66,\"close\":145.15,\"volume\":3749642,\"unadjustedVolume\":3749642,\"change\":-1.56,\"changePercent\":-1.063,\"vwap\":145.6371,\"label\":\"Jul 27\",\"changeOverTime\":0.0390121689334289}]";
//		logger.info("json {}", jsonString);
//		
//		try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
//			JsonArray jsonArray = reader.readArray();
//			int jsonArraySize = jsonArray.size();
//			logger.info("jsonArraySize = {}", jsonArraySize);
//			
//			List<Chart> result = new ArrayList<>();
//			for(int i = 0; i < jsonArraySize; i++) {
//				JsonObject element = jsonArray.getJsonObject(i);
//				Chart chart = new Chart(element);
//				result.add(chart);
//			}
//
//			logger.info("result ({}){}", result.size(), result.toString());
//		}
//	}
//	
	public static void main(String[] args) {
		Logger logger = LoggerFactory.getLogger(Chart.class);
		logger.info("START");
		
		{
			Map<String, EOD[]> dataMap = EOD.getStock(Range.M1, "ibm", "bt");
			logger.info("dataMap {}", dataMap.size());
			for(Map.Entry<String, EOD[]> entry: dataMap.entrySet()) {
				logger.info("  {} {}", entry.getKey(), Arrays.asList(entry.getValue()));
			}
		}

		logger.info("STOP");
	}
}

