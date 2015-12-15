package yokwe.finance.securities.stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.database.PriceTable;

public final class Data {
	private static final Logger logger = LoggerFactory.getLogger(Data.class);

	public static class Daily {
		public final String date;
		public final double value;
		public Daily(String date, double value) {
			this.date  = date;
			this.value = value;
		}
		public Daily getInstance(double newValue) {
			return new Daily(this.date, newValue);
		}
	}
	
	private Map<String, Daily[]> map = new TreeMap<>();
	public Data(List<PriceTable> dataList) {
		Map<String, List<Daily>> symbolMap = new TreeMap<>();
		for(PriceTable priceTable: dataList) {
			String symbol = priceTable.symbol;
			Daily  daily  = new Daily(priceTable.date, priceTable.close);
			
			final List<Daily> dailyList;
			if (symbolMap.containsKey(symbol)) {
				dailyList = symbolMap.get(symbol);
			} else {
				dailyList = new ArrayList<>();
				symbolMap.put(symbol, dailyList);
			}
			dailyList.add(daily);
		}
		for(String symbol: symbolMap.keySet()) {
			logger.info("symbol = {}", symbol);
			List<Daily> dailyList = symbolMap.get(symbol);
			map.put(symbol, dailyList.toArray(new Daily[0]));
		}
	}
	public Daily[] toArray(String symbol) {
		if (!map.containsKey(symbol)) {
			logger.error("Unknown symbol = {}", symbol);
			throw new SecuritiesException("Unknown symol");
		}
		Daily dailyArray[] = map.get(symbol);
		return Arrays.copyOf(dailyArray, dailyArray.length);
	}
	public double[] toDoubleArray(String symbol) {
		if (!map.containsKey(symbol)) {
			logger.error("Unknown symbol = {}", symbol);
			throw new SecuritiesException("Unknown symol");
		}
		Daily dailyArray[] = map.get(symbol);
		return Arrays.stream(dailyArray).mapToDouble(o -> o.value).toArray();
	}
}
