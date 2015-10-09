package yokwe.finance.etf.web;

import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.etf.ETFException;

public abstract class Data {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Data.class);

	private static Map<String, Data> dataMap = new TreeMap<>();
	static {
		dataMap.put("price", new PriceData());
		dataMap.put("vol",   new VolumeData());
		dataMap.put("div",   new DividendData());
	}
	public static Data getInstance(String type) {
		logger.info("type = {}", type);
		Data ret = dataMap.get(type);
		if (ret == null) {
			logger.error("Unknown type = {}", type);
			throw new ETFException();
		}
		return ret;
	}

	public abstract List<DailyData> generate(Statement statement, String symbol, Period period);
}