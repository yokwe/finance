package yokwe.finance.securities.eod;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.CSVUtil;

public class MarketHoliday {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MarketHoliday.class);

	private static final String PATH_HOLIDAY = "data/market/marketHoliday.csv";
	
	// 2 January 2017
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d LLLL uuuu", Locale.US);
	
	public static class CSVRecord {
		public String date;   // YYYY-MM-DD
		public String event;  // any
		public String status; // Closed or any
	}
	public static class Record {
		public final LocalDate date;
		public final boolean   closed;
		
		public Record(LocalDate date, boolean closed) {
			this.date   = date;
			this.closed = closed;
		}
	}
	
	private static Map<LocalDate, Record> map = new TreeMap<>();
	static {
		List<CSVRecord> recordList = CSVUtil.loadWithHeader(PATH_HOLIDAY, CSVRecord.class);
		for(CSVRecord record: recordList) {
			// Sanity check
			logger.info("date {}", record.date);
			LocalDate date  = LocalDate.parse(record.date, DATE_FORMATTER);
			boolean closed = record.status.equals("Closed");
			map.put(date, new Record(date, closed));
			
			logger.info("{} {}", date, closed);
		}
	}
	
	public static boolean isClosed(LocalDate date) {
		if (date.getDayOfWeek().equals(DayOfWeek.SATURDAY)) return true;
		if (date.getDayOfWeek().equals(DayOfWeek.SUNDAY))   return true;
		if (map.containsKey(date)) {
			Record record = map.get(date);
			return record.closed;
		} else {
			return false;
		}
	}
	public static boolean isHoliday(String date) {
		// Sanity check
		if (!date.matches("20[0-9][0-9]-(0[1-9]|1[12])-(0[1-9]|1[0-9]|2[0-9]|3[01])")) {
			logger.error("Unexpected format date {}", date);
			throw new SecuritiesException("Unexpected format date");
		}
		return map.containsKey(date);
	}
	
	public static void main(String[] args) {
		logger.info("START");
		{
			LocalDate localDate = LocalDate.parse("2017-01-01");
			boolean closed = isClosed(localDate);
			logger.info("{} {}", localDate, closed);
		}
		{
			LocalDate localDate = LocalDate.parse("2017-01-02");
			boolean closed = isClosed(localDate);
			logger.info("{} {}", localDate, closed);
		}
		{
			LocalDate localDate = LocalDate.parse("2017-01-03");
			boolean closed = isClosed(localDate);
			logger.info("{} {}", localDate, closed);
		}
		logger.info("STOP");
	}
}
