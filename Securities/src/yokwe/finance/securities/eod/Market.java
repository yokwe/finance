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
	
	private static final LocalDate lastTradingDate;
	
	static {
		LocalDateTime today = LocalDateTime.now(ZoneId.of("America/New_York"));
		if (today.getHour() < HOUR_CLOSE_MARKET) today = today.minusDays(1); // Move to yesterday if it is before market close
		
		
		for(;;) {
			if (isClosed(today)) {
				today = today.minusDays(1);
				continue;
			}

			break;
		}
		
		lastTradingDate  = today.toLocalDate();
		logger.info("Last Trading Date {}", lastTradingDate);
	}
	
	public static LocalDate getLastTradingDate() {
		return lastTradingDate;
	}
	
	public static final boolean isClosed(LocalDateTime dateTime) {
		return isClosed(dateTime.toLocalDate());
	}
	public static final boolean isClosed(String date) {
		return isClosed(LocalDate.parse(date));
	}
	public static final boolean isClosed(LocalDate date) {
		DayOfWeek dayOfWeek = date.getDayOfWeek();
		if (dayOfWeek == DayOfWeek.SUNDAY)   return true;
		if (dayOfWeek == DayOfWeek.SATURDAY) return true;
		
		Holiday holiday = holidayMap.get(date);
		return holiday != null && holiday.closed;
	}
}
