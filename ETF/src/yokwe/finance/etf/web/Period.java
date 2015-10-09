package yokwe.finance.etf.web;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import yokwe.finance.etf.ETFException;

class Period {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Period.class);

	private static Matcher matcherPeriod     = Pattern.compile("([0-9]+)([ymd])").matcher(""); // [0-9]+[ymd]
	private static Matcher matcherDatePeriod = Pattern.compile("([0-9]{6})\\-([0-9]+)([ymd])").matcher(""); // yyyymm-[0-9]+[ymd]

	private static String dateString(Calendar calendar) {
		return String.format("%4d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
				calendar.get(Calendar.DAY_OF_MONTH));
	}

	final String dateStart;
	final String dateEnd;
	
	public Period(String value) {
		Calendar today = Calendar.getInstance();
		Calendar calFrom = Calendar.getInstance();
		Calendar calTo   = Calendar.getInstance();
		
		calFrom.setTimeInMillis(today.getTimeInMillis());
		calTo.setTimeInMillis(today.getTimeInMillis());
		
		matcherPeriod.reset(value); // [0-9]+[ymd]
		matcherDatePeriod.reset(value); // yyyymm-[0-9]+[ymd]
		if (matcherPeriod.matches()) {
			// [0-9]+[ymd]
			final int groupCount = matcherPeriod.groupCount();
			if (groupCount != 2) {
				logger.error("groupCount = {}", groupCount);
				throw new ETFException("groupCount");
			}
			String number = matcherPeriod.group(1);
			String mode   = matcherPeriod.group(2);
			final int duration;
			try {
				duration = Integer.parseInt(number);
			} catch (NumberFormatException e) {
				logger.error("number = {}", number);
				throw new ETFException("number");
			}
			
			switch(mode) {
			case "y":
				calFrom.add(Calendar.YEAR, -duration);
				break;
			case "m":
				calFrom.add(Calendar.MONTH, -duration);
				break;
			case "d":
				calFrom.add(Calendar.DATE, -duration);
				break;
			default:
				logger.error("mode = {}", mode);
				throw new ETFException("mode");
			}
		} else if (matcherDatePeriod.matches()) {
			// yyyymm-[0-9]+[ymd]
			final int groupCount = matcherDatePeriod.groupCount();
			if (groupCount != 3) {
				logger.error("groupCount = {}", groupCount);
				throw new ETFException("groupCount");
			}
			String yyyymm = matcherDatePeriod.group(1);
			String number = matcherDatePeriod.group(2);
			String mode   = matcherDatePeriod.group(3);
			{
				String yyyy = yyyymm.substring(0, 4);
				String mm = yyyymm.substring(4, 6);
				
				int year = Integer.parseInt(yyyy);
				int month = Integer.parseInt(mm);
				
				if (year < 1900 || 2100 < year) {
					logger.error("year = {}", year);
					throw new ETFException("year");
				}
				if (month < 1 || 12 < month) {
					logger.error("month = {}", month);
					throw new ETFException("month");
				}
				
				calFrom.set(Calendar.YEAR, year);
				calFrom.set(Calendar.MONTH, month - 1); // 0 base
				calFrom.set(Calendar.DATE, 1); // 1 base
			}

			calTo.setTimeInMillis(calFrom.getTimeInMillis());

			// [0-9]+[ymd]
			{
				int duration;
				try {
					duration = Integer.parseInt(number);
				} catch (NumberFormatException e) {
					logger.error("number = {}", number);
					throw new ETFException("number");
				}
				switch(mode) {
				case "y":
					calTo.add(Calendar.YEAR, duration);
					break;
				case "m":
					calTo.add(Calendar.MONTH, duration);
					break;
				case "d":
					calTo.add(Calendar.DATE, duration);
					break;
				default:
					logger.error("mode = {}", mode);
					throw new ETFException("mode");
				}
			}
		} else {
			logger.error("value = {}", value);
			throw new ETFException("period");
		}
		
		dateStart = dateString(calFrom);
		dateEnd   = dateString(calTo);
		logger.info("period = {}  {} - {}", value, dateStart, dateEnd);
	}
}