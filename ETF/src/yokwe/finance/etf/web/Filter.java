package yokwe.finance.etf.web;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.DoubleFunction;
import java.util.function.ToDoubleFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import yokwe.finance.etf.ETFException;
import yokwe.finance.etf.util.DoubleStreamUtil.MovingStats;

class Filter {
	private static Matcher matcher = Pattern.compile("(avg|sd|skew|kurt)([0-9]+)").matcher("");

	static Map<String, ToDoubleFunction<MovingStats>> map = new TreeMap<>();
	static {
		map.put("avg",  o -> o.mean);
		map.put("sd",   o -> o.standardDeviation);
		map.put("skew", o -> o.skewness);
		map.put("kurt", o -> o.kurtosis);
	}
	
	private ToDoubleFunction<MovingStats> mapToDouble;
	private DoubleFunction<MovingStats>   mapToObj;
	private int                           interval;
	public Filter(String value) {
		matcher.reset(value);
		if (matcher.matches()) {
			if (matcher.groupCount() != 2) {
				CSVServlet.logger.error("groupCount = {}", matcher.groupCount());
				throw new ETFException("groupCount");
			}
			String type   = matcher.group(1);
			String number = matcher.group(2);
			try {
				interval = Integer.parseInt(number);
			} catch (NumberFormatException e) {
				CSVServlet.logger.error("number = {}", number);
				throw new ETFException("number");
			}
			mapToDouble = map.get(type);
			if (mapToDouble == null) {
				CSVServlet.logger.error("type = {}", type);
				throw new ETFException("type");
			}
			CSVServlet.logger.info("filter {} {}", type, number);
		} else {
			CSVServlet.logger.error("value = {}", value);
			throw new ETFException("fiter");
		}
		mapToObj = MovingStats.mapToObj(interval);
	}
	
	public double[] apply(double[] doubleData) {
		return Arrays.stream(doubleData).mapToObj(mapToObj).mapToDouble(mapToDouble).toArray();
	}
}