package yokwe.finance.etf.web;

import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import yokwe.finance.etf.ETFException;

public abstract class Generator {
	private static Map<String, Generator> generatorMap = new TreeMap<>();
	static {
		generatorMap.put("price", new PriceData());
		generatorMap.put("vol",   new VolumeData());
		generatorMap.put("div",   new DividendData());
	}
	public static Generator getInstance(String type) {
		CSVServlet.logger.info("type = {}", type);
		Generator ret = generatorMap.get(type);
		if (ret == null) {
			CSVServlet.logger.error("Unknown type = {}", type);
			throw new ETFException();
		}
		return ret;
	}

	public abstract List<DailyData> generate(Statement statement, String symbol, Period period);
}