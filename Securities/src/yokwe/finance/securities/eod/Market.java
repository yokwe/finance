package yokwe.finance.securities.eod;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.util.CSVUtil;

public class Market {
	private static final Logger logger = LoggerFactory.getLogger(Market.class);

	public static final String PATH_MARKET_HOLIDAY_CSV = "data/market/marketHoliday.csv";
	public static final int HOUR_CLOSE_MARKET = 16; // market close at 1600
	
	public static class MarketHoliday {
		public String date;
		public String event;
		public String status; // Closed or other
	}
	public static class Holiday {
		public final LocalDate date;
		public final boolean   closed;
		public Holiday(LocalDate date, boolean closed) {
			this.date   = date;
			this.closed = closed;
		}
	}
	private static final Map<LocalDate, Holiday> holidayMap = new TreeMap<>();
	static {
		List<MarketHoliday> marketHolidayList = CSVUtil.loadWithHeader(PATH_MARKET_HOLIDAY_CSV, MarketHoliday.class);
		for(MarketHoliday marketHoliday: marketHolidayList) {
			LocalDate date   = LocalDate.parse(marketHoliday.date);
			boolean   closed = marketHoliday.status.equals("Closed");
			holidayMap.put(date, new Holiday(date, closed));
		}
	}
	
	public static final LocalDate lastTradingDate;
	
	static {
		LocalDateTime today = LocalDateTime.now(ZoneId.of("America/New_York"));
		if (today.getHour() < HOUR_CLOSE_MARKET) today = today.minusDays(1); // Move to yesterday if it is before market close
		
		
		for(;;) {
			DayOfWeek dayOfWeek = today.getDayOfWeek();
			if (dayOfWeek == DayOfWeek.SUNDAY) {
				today = today.minusDays(2); // Move to previous Friday
				continue;
			}
			if (dayOfWeek == DayOfWeek.SATURDAY) {
				today = today.minusDays(1); // Move to previous Friday
				continue;
			}
			
			Holiday holiday = holidayMap.get(today.toLocalDate());
			if (holiday != null) {
				if (holiday.closed) {
					today = today.minusDays(1); // Move to previous day
					continue;
				}
			}

			break;
		}
		
		lastTradingDate  = today.toLocalDate();
		logger.info("Last Trading Date {}", lastTradingDate);
	}
	
	public static LocalDate getLastTradingDate() {
		return lastTradingDate;
	}
}
