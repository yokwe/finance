package yokwe.finance.securities.web;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.ToDoubleFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.DoubleStreamUtil.MovingStats;

class Filter {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Filter.class);

	private static Matcher matcher = Pattern.compile("(avg|sd|skew|kurt)([0-9]+)").matcher("");

	static Map<String, ToDoubleFunction<MovingStats>> map = new TreeMap<>();
	static {
		map.put("avg",  o -> o.mean);
		map.put("sd",   o -> o.standardDeviation);
		map.put("skew", o -> o.skewness);
		map.put("kurt", o -> o.kurtosis);
	}
	
	private ToDoubleFunction<MovingStats> mapToDouble;
	private MovingStats.MapToObj          mapToObj;
	private int                           interval;
	public Filter(String value) {
		matcher.reset(value);
		if (matcher.matches()) {
			if (matcher.groupCount() != 2) {
				logger.error("groupCount = {}", matcher.groupCount());
				throw new SecuritiesException("groupCount");
			}
			String type   = matcher.group(1);
			String number = matcher.group(2);
			try {
				interval = Integer.parseInt(number);
			} catch (NumberFormatException e) {
				logger.error("number = {}", number);
				throw new SecuritiesException("number");
			}
			mapToDouble = map.get(type);
			if (mapToDouble == null) {
				logger.error("type = {}", type);
				throw new SecuritiesException("type");
			}
			logger.info("filter {} {}", type, number);
		} else {
			logger.error("value = {}", value);
			throw new SecuritiesException("fiter");
		}
		mapToObj = MovingStats.mapToObj(interval);
	}
	
	public double[] apply(double[] doubleData) {
		// Need to call clear before processing data after second time
		mapToObj.clear();
		return Arrays.stream(doubleData).mapToObj(mapToObj).mapToDouble(mapToDouble).toArray();
	}
}