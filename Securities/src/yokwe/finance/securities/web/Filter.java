package yokwe.finance.securities.web;

import java.util.function.DoubleUnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.DoubleStreamUtil.HistoricalVolatility;
import yokwe.finance.securities.util.DoubleStreamUtil.SimpleMovingStats;
import yokwe.finance.securities.util.DoubleStreamUtil.ValueAtRisk;
import yokwe.finance.securities.util.MovingStats;

class Filter {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Filter.class);

	private static Matcher matcher = Pattern.compile("(avg|sd|skew|kurt|hv|var|smam|smas|emam|emas)([0-9]+)").matcher("");
	
	public static DoubleUnaryOperator getInstance(String value) {
		final String type;
		final int    interval;
		
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
			return SimpleMovingStats.getInstance(SimpleMovingStats.Type.MEAN, interval);
		case "sd":
			return SimpleMovingStats.getInstance(SimpleMovingStats.Type.STANDARD_DEVIATION, interval);
		case "skew":
			return SimpleMovingStats.getInstance(SimpleMovingStats.Type.SKEWNESS, interval);
		case "kurt":
			return SimpleMovingStats.getInstance(SimpleMovingStats.Type.KURTOSIS, interval);
		case "hv":
			return HistoricalVolatility.getInstance(interval);
		case "var":
			return ValueAtRisk.getInstance(interval);
		case "smam":
			return MovingStats.SMA.mean(interval);
		case "smas":
			return MovingStats.SMA.sd(interval);
		case "emam":
			return MovingStats.EMA.mean(interval);
		case "emas":
			return MovingStats.EMA.sd(interval);
		default:
			logger.error("Unknonw type = {}", type);
			throw new SecuritiesException("Unknown type");
		}
	}
}