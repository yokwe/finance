package yokwe.finance.securities.web;

import java.util.function.DoubleUnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.DoubleUtil;

class Filter {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Filter.class);

	private static Matcher matcher = Pattern.compile("(avg|sd|skew|kurt|smasd)([0-9]+)").matcher("");
	
	public static DoubleUnaryOperator getInstance(String value) {
		final String type;
		final int    interval;
		
		// special for var and ema
		switch(value) {
		case "var":
			return DoubleUtil.valueAtRisk(DoubleUtil.DEFAULT_ALPHA, DoubleUtil.Confidence.DEFAULT);
		}
		
		matcher.reset(value);
		if (matcher.matches()) {
			if (matcher.groupCount() != 2) {
				logger.error("groupCount = {}", matcher.groupCount());
				throw new SecuritiesException("groupCount");
			}
			type   = matcher.group(1);
			String number = matcher.group(2);
			try {
				interval = Integer.parseInt(number);
			} catch (NumberFormatException e) {
				logger.error("number = {}", number);
				throw new SecuritiesException("number");
			}
			logger.info("filter {} {}", type, number);
		} else {
			logger.error("value = {}", value);
			throw new SecuritiesException("fiter");
		}
		
		switch(type) {
		case "avg":
			return DoubleUtil.sma(interval);
		case "sd":
			return DoubleUtil.simpleStats(DoubleUtil.StatsType.SD, interval);
		case "skew":
			return DoubleUtil.simpleStats(DoubleUtil.StatsType.SKEW, interval);
		case "kurt":
			return DoubleUtil.simpleStats(DoubleUtil.StatsType.KURT, interval);
		case "smasd":
			return DoubleUtil.sma_sd(interval);
		default:
			logger.error("Unknonw type = {}", type);
			throw new SecuritiesException("Unknown type");
		}
	}
}