package yokwe.finance.securities.web;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.database.DividendTable;
import yokwe.finance.securities.database.PriceTable;

public abstract class Data {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Data.class);
	
	public static final class Daily {
		public final String date;
		public final String symbol;
		public final double value;
		
		public Daily(String date, String symbol, double value) {
			this.date   = date;
			this.symbol = symbol;
			this.value  = value;
		}
		public Daily() {
			this.date   = "";
			this.symbol = "";
			this.value  = 0;
		}
	}

	private static Map<String, Data> dataMap = new TreeMap<>();
	static {
		dataMap.put("price", new Price());
		dataMap.put("vol",   new Volume());
		dataMap.put("div",   new Dividend());
	}
	public static Data getInstance(String type) {
		logger.info("type = {}", type);
		Data ret = dataMap.get(type);
		if (ret == null) {
			logger.error("Unknown type = {}", type);
			throw new SecuritiesException();
		}
		return ret;
	}

	public abstract List<Daily> generate(Connection connection, String symbol, Period period);
	
	public static List<Daily> logReturn(List<Daily> dailyList) {
		if (dailyList == null) return null;
		if (dailyList.size() <= 1) return null;
		
		List<Daily> ret = new ArrayList<>();
		
		boolean firstTime = true;
		double lastValue = 0;;
		for(Daily daily: dailyList) {
			if (firstTime) {
				lastValue = daily.value;
				firstTime = false;
			} else {
				double value = daily.value;
				ret.add(new Daily(daily.date, daily.symbol, Math.log(value / lastValue)));
				lastValue = value;
			}
		}
		return ret;
	}
	
	public static final class Price extends Data {
		@Override
		public List<Daily> generate(Connection connection, String symbol, Period period) {
			List<PriceTable> result = PriceTable.getAllBySymbolDateRange(connection, symbol, period.dateStart, period.dateEnd);
			List<Daily>      ret    = new ArrayList<>();
			result.stream().forEach(o -> ret.add(new Daily(o.date, o.symbol, o.close)));
			return ret;
		}
	}

	public static final class Volume extends Data {
		@Override
		public List<Daily> generate(Connection connection, String symbol, Period period) {
			List<PriceTable> result = PriceTable.getAllBySymbolDateRange(connection, symbol, period.dateStart, period.dateEnd);
			List<Daily>      ret    = new ArrayList<>();
			result.stream().forEach(o -> ret.add(new Daily(o.date, o.symbol, o.volume)));
			return ret;
		}
	}

	public static final class Dividend extends Data {
		@Override
		public List<Daily> generate(Connection connection, String symbol, Period period) {
			List<DividendTable> result = DividendTable.getAllBySymbolDateRange(connection, symbol, period.dateStart, period.dateEnd);

			Map<String, DividendTable> map = new TreeMap<>();
			for(DividendTable table: result) {
				// Manipulate date to make summation of values within same month
				table.date = table.date.substring(0, 8) + "01";
				if (map.containsKey(table.date)) {
					map.get(table.date).dividend += table.dividend;
				} else {
					map.put(table.date, table);
				}
			}
			
			List<Daily> ret = new ArrayList<>();
			map.values().stream().forEach(o -> ret.add(new Daily(o.date, o.symbol, o.dividend)));
			return ret;
		}
	}
}
