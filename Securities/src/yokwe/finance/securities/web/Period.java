package yokwe.finance.securities.web;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class Period {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Period.class);

	private static Matcher matcherPeriod     = Pattern.compile("([0-9]+)([ymd])").matcher("");              // [0-9]+[ymd]
	private static Matcher matcherDatePeriod = Pattern.compile("([0-9]{6})\\-([0-9]+)([ymd])").matcher(""); // yyyymm-[0-9]+[ymd]

	final LocalDate dateStart;
	final LocalDate dateEnd;

	public Period(String value) {
		matcherPeriod.reset(value);     // [0-9]+[ymd]
		matcherDatePeriod.reset(value); // yyyymm-[0-9]+[ymd]

		if (matcherPeriod.matches()) {
			// [0-9]+[ymd]
			final int groupCount = matcherPeriod.groupCount();
			if (groupCount != 2) {
				logger.error("groupCount = {}", groupCount);
				throw new SecuritiesException("groupCount");
			}
			String number = matcherPeriod.group(1);
			String mode   = matcherPeriod.group(2);
			final int duration;
			try {
				duration = Integer.parseInt(number);
			} catch (NumberFormatException e) {
				logger.error("number = {}", number);
				throw new SecuritiesException("number");
			}
			
			dateEnd = LocalDate.now();
			switch(mode) {
			case "y":
				dateStart = dateEnd.minusYears(duration);
				break;
			case "m":
				dateStart = dateEnd.minusMonths(duration);
				break;
			case "d":
				dateStart = dateEnd.minusDays(duration);
				break;
			default:
				logger.error("mode = {}", mode);
				throw new SecuritiesException("mode");
			}
		} else if (matcherDatePeriod.matches()) {
			// yyyymm-[0-9]+[ymd]
			final int groupCount = matcherDatePeriod.groupCount();
			if (groupCount != 3) {
				logger.error("groupCount = {}", groupCount);
				throw new SecuritiesException("groupCount");
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
					throw new SecuritiesException("year");
				}
				if (month < 1 || 12 < month) {
					logger.error("month = {}", month);
					throw new SecuritiesException("month");
				}
				dateStart = LocalDate.of(year, month, 1);
			}

			// [0-9]+[ymd]
			{
				int duration;
				try {
					duration = Integer.parseInt(number);
				} catch (NumberFormatException e) {
					logger.error("number = {}", number);
					throw new SecuritiesException("number");
				}
				switch(mode) {
				case "y":
					dateEnd = dateStart.plusYears(duration);
					break;
				case "m":
					dateEnd = dateStart.plusMonths(duration);
					break;
				case "d":
					dateEnd = dateStart.plusDays(duration);
					break;
				default:
					logger.error("mode = {}", mode);
					throw new SecuritiesException("mode");
				}
			}
		} else {
			logger.error("value = {}", value);
			throw new SecuritiesException("period");
		}
		
		logger.info("period = {}  {}", value, toString());
	}
	
	@Override
	public String toString() {
		return String.format("{%s - %s}", dateStart, dateEnd);
	}
}
