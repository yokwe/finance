package yokwe.finance.etf.web;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import yokwe.finance.etf.ETFException;
import yokwe.finance.etf.util.StreamUtil.MovingStats;
import yokwe.finance.etf.util.StreamUtil.MovingStats.Accumlator;

class Filter {
	private static Matcher matcher = Pattern.compile("(avg|sd|skew|kurt)([0-9]+)").matcher("");

	static Map<String, Function<MovingStats, Double>> mapperMap = new TreeMap<>();
	static {
		mapperMap.put("avg",  o -> o.mean);
		mapperMap.put("sd",   o -> o.standardDeviation);
		mapperMap.put("skew", o -> o.skewness);
		mapperMap.put("kurt", o -> o.kurtosis);
	}
	
	private Function<MovingStats, Double>                    mapper;
	private int                                              interval;
	private Collector<Double, Accumlator, List<MovingStats>> collector;
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
			mapper = mapperMap.get(type);
			if (mapper == null) {
				CSVServlet.logger.error("type = {}", type);
				throw new ETFException("type");
			}
			CSVServlet.logger.info("filter {} {}", type, number);
		} else {
			CSVServlet.logger.error("value = {}", value);
			throw new ETFException("fiter");
		}
		collector = MovingStats.getInstance(interval);
	}
	
	public List<Double> apply(List<Double> data) {
		List<MovingStats> stats = data.stream().collect(collector);
		List<Double> ret = stats.stream().map(mapper).collect(Collectors.toList());
		return ret;
	}
}