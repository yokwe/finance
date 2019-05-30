package yokwe.finance.stock.data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.stock.util.CSVUtil;

public class Market {
	private static final Logger logger = LoggerFactory.getLogger(Market.class);

	public static final String PATH_MARKET_HOLIDAY_CSV = "data/market/marketHoliday.csv";
	public static final int    HOUR_CLOSE_MARKET       = 16; // market close at 1600
	public static final ZoneId ZONE_ID                 = ZoneId.of("America/New_York");
	
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
			boolean   closed = marketHoliday.status.toLowerCase().startsWith("close"); // To avoid confusion comes from misspelled word
			holidayMap.put(date, new Holiday(date, closed));
		}
	}
	
	private static final LocalDate lastTradingDate;
	
	static {
		LocalDateTime today = LocalDateTime.now(ZONE_ID);
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
	public static final boolean isSaturdayOrSunday(String date) {
		return isSaturdayOrSunday(LocalDate.parse(date));
	}
	public static final boolean isSaturdayOrSunday(LocalDate date) {
		DayOfWeek dayOfWeek = date.getDayOfWeek();
		if (dayOfWeek == DayOfWeek.SUNDAY)   return true;
		if (dayOfWeek == DayOfWeek.SATURDAY) return true;
		return false;
	}
	
	public static LocalDate getNextTradeDate(LocalDate date) {
		LocalDate nextDate = date;
		for(;;) {
			nextDate = nextDate.plusDays(1);
			if (isClosed(nextDate)) continue;
			return nextDate;
		}
	}
	public static LocalDate getPreviousTradeDate(LocalDate date) {
		LocalDate prevDate = date;
		for(;;) {
			prevDate = prevDate.minusDays(1);
			if (isClosed(prevDate)) continue;
			return prevDate;
		}
	}
	private static LocalDate T2_SETTLEMENT_START_DATE = LocalDate.of(2017, 9, 5);
	// See settlement calendar below
	//    https://stlcl.com/?year=2016&month=11
	private static Map<LocalDate, LocalDate> irregularlementDateMap = new TreeMap<>();
	static {
		irregularlementDateMap.put(LocalDate.of(2016, 11, 10), LocalDate.of(2016, 11, 16));
		irregularlementDateMap.put(LocalDate.of(2017, 10,  6), LocalDate.of(2017, 10, 11));
		irregularlementDateMap.put(LocalDate.of(2018, 10,  4), LocalDate.of(2018, 10,  9));
		irregularlementDateMap.put(LocalDate.of(2018, 10,  5), LocalDate.of(2018, 10, 10));
		irregularlementDateMap.put(LocalDate.of(2018, 11,  9), LocalDate.of(2018, 11, 14));
	}
	public static LocalDate toSettlementDate(LocalDate tradeDate) {
		if (irregularlementDateMap.containsKey(tradeDate)) {
			return irregularlementDateMap.get(tradeDate);
		}
			
		LocalDate t0 = tradeDate;
		LocalDate t1 = getNextTradeDate(t0);
		LocalDate t2 = getNextTradeDate(t1);
		LocalDate t3 = getNextTradeDate(t2);
		
		if (tradeDate.isBefore(T2_SETTLEMENT_START_DATE)) {
			return t3;
		} else {
			return t2;
		}
	}
}
