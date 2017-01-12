package yokwe.finance.securities.tax;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.database.NasdaqTable;
import yokwe.finance.securities.util.CSVUtil;
import yokwe.finance.securities.util.HttpUtil;
import yokwe.finance.securities.util.NasdaqUtil;

public class Price {
	public static class Cache {
		private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Cache.class);

		private static final String PATH_CACHE       = "tmp/fetch/priceCache.csv";
		private static final File   FILE_PRICE_CACHE = new File(PATH_CACHE);
		
		//private static final List<String> SYMBOL_LIST = Arrays.asList("BT", "CSCO", "INTC");
		private static final List<String> SYMBOL_LIST = Arrays.asList("IBM", "NYT", "PEP");
		
		private static final DateTimeFormatter DATE_FORMAT_URL    = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);
		private static final DateTimeFormatter DATE_FORMAT_PARSE  = DateTimeFormatter.ofPattern("d-MMM-yy");
		private static final DateTimeFormatter DATE_FORMAT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		
		private static final LocalDate DATE_TARGET;
		static {
			LocalDateTime today = LocalDateTime.now(ZoneId.of("America/New_York"));
			if (today.getHour() < 16) today = today.minusDays(1); // Move to yesterday if it is before market close
			
			// Adjust for weekends
			DayOfWeek dayOfWeek = today.getDayOfWeek();
			if (dayOfWeek == DayOfWeek.SUNDAY)   today = today.minusDays(-2); // Move to previous Friday
			if (dayOfWeek == DayOfWeek.SATURDAY) today = today.minusDays(-1); // Move to previous Friday
			
			DATE_TARGET = today.toLocalDate();
			logger.info("DATE_TARGET       {}", DATE_TARGET);
		}
		
		private static final Map<String, NasdaqTable> nasdaqMap  = NasdaqUtil.getMap();
		private static final Map<String, Price>       priceCache = new TreeMap<>();
		private static boolean                        needSave   = false;

		private static void loadPriceCache() {
			if (FILE_PRICE_CACHE.exists()) {
				priceCache.clear();
				List<Price> priceList = CSVUtil.loadWithHeader(PATH_CACHE, Price.class);
				for(Price price: priceList) {
					priceCache.put(price.symbol, price);
				}
				needSave = false;
			}
		}
		private static void validatePriceCache(String date) {
			boolean needClear = false;
			for(Price price: priceCache.values()) {
				if (price.date.equals(date)) continue;
				needClear = true;
				break;
			}
			if (needClear) {
				priceCache.clear();
				needSave = true;
			}
		}
		public static void savePriceCache() {
			if (needSave) {
				List<Price> priceList = new ArrayList<>(priceCache.values());
				CSVUtil.saveWithHeader(priceList, PATH_CACHE);
				needSave = false;
			}
		}


		private static final LocalDate LAST_TRADING_DATE;
		static {
			loadPriceCache();
			validatePriceCache(DATE_TARGET.toString());		
			
			if (priceCache.isEmpty()) {
				// Build priceList from SYMBOL_LIST
				List<Price> priceList = new ArrayList<>();
				for(String symbol: SYMBOL_LIST) {
					Price price = getLastPrice(symbol, DATE_TARGET.minusDays(5), DATE_TARGET);
					priceList.add(price);
				}
				String date = priceList.get(0).date;
				
				// Sanity check
				for(Price price: priceList) {
					if (!price.date.equals(date)) {
						logger.error("Inconsistent date");
						for(Price e: priceList) {
							logger.error("{} {}", e.symbol, e.date);
						}
						throw new SecuritiesException("Inconsistent date");
					}
					// Fill cache for later use
					priceCache.put(price.symbol, price);
				}
				LAST_TRADING_DATE = LocalDate.parse(date);
				
				needSave = true;
				savePriceCache();
			} else {
				LAST_TRADING_DATE = DATE_TARGET;
			}
			logger.info("LAST_TRADING_DATE {}", LAST_TRADING_DATE);
		}
		
		private static Price getLastPrice(String symbol, LocalDate startDate, LocalDate endDate) {
			String dateFrom = startDate.format(DATE_FORMAT_URL).replace(" ", "%20");
			String dateTo   = endDate.format(DATE_FORMAT_URL).replace(" ", "%20");

			// Convert from '.PR.' to  '-' in symbol of preferred stock for nasdaqMap
			NasdaqTable nasdaq = nasdaqMap.get(symbol.replace(".PR.", "-"));
			if (nasdaq == null) {
				logger.error("No symbol in nasdaqMap {}", symbol);
				throw new SecuritiesException("No symbol in nasdaqMap");
			}
			String url = String.format("https://www.google.com/finance/historical?q=%s:%s&startdate=%s&enddate=%s&output=csv", nasdaq.exchange, nasdaq.google, dateFrom, dateTo);
//			logger.info("url {}", url);
			
			String content = HttpUtil.downloadAsString(url);
			String[] lines = content.split("\n");
//			logger.info("lines {}", lines.length);
			if (lines.length <= 1) {
				logger.error("Unexpected content {}", content);
				throw new SecuritiesException("Unexpected content");
			}
			
			// Sanity check
			String GOOGLE_PRICE_HEADER = "\uFEFFDate,Open,High,Low,Close,Volume";
			String header = lines[0];
			if (!header.equals(GOOGLE_PRICE_HEADER)) {
				logger.error("Unexpected header  {}", header);
				throw new SecuritiesException("Unexpected header");
			}
			String[] values = lines[1].split(",");
			if (values.length != 6) {
				logger.error("Unexpected line  {}", lines[1]);
				throw new SecuritiesException("Unexpected header");
			}
			
			// Fix format of date  02-Jan-14 => 2014-01-02
			// Fix year of date    02-Jan-80 => 1980-01-02
			{
				LocalDate localDate = LocalDate.from(DATE_FORMAT_PARSE.parse(values[0]));
				if (DATE_TARGET.getYear() < localDate.getYear()) localDate = localDate.minusYears(100);
				values[0] = DATE_FORMAT_FORMAT.format(localDate);
			}
			
			// Special when field (open, high and low) contains dash
			if (values[1].equals("-")) {
				values[1] = values[4];
			}
			if (values[2].equals("-")) {
				values[2] = values[4];
			}
			if (values[3].equals("-")) {
				values[3] = values[4];
			}
			
			// Special when field (volume) contains dash
			if (values[5].equals("-")) {
				values[5] = "0";
			}

			String date   = values[0];
			double open   = Double.valueOf(values[1]);
			double high   = Double.valueOf(values[2]);
			double low    = Double.valueOf(values[3]);
			double close  = Double.valueOf(values[4]);
			long   volume = Long.valueOf(values[5]);
			
			Price ret = new Price(symbol, date, open, high, low, close, volume);
			return ret;
		}

		public static Price getLastPrice(String symbol) {
			Price price;
			if (priceCache.containsKey(symbol)) {
				price = priceCache.get(symbol);
			} else {
				price = getLastPrice(symbol, DATE_TARGET.minusDays(5), DATE_TARGET);
				
				if (!price.date.equals(LAST_TRADING_DATE.toString())) {
					logger.warn("Inconsistent date {} {}", price.symbol, price.date);
				}
				priceCache.put(symbol, price);
				needSave = true;
			}
			return price;
		}
	}
	
	
	public static void saveCache() {
		Cache.savePriceCache();
	}
	public static Price getLastPrice(String symbol) {
		return Cache.getLastPrice(symbol);
	}
	public static List<Price> getLastPriceList(List<String> symbolList) {
		List<Price> priceList = new ArrayList<>();
		for(String symbol: symbolList) {
			Price price = Cache.getLastPrice(symbol);
			priceList.add(price);
		}
		return priceList;
	}
	
	public String symbol;
	public String date;
	public double open;
	public double high;
	public double low;
	public double close;
	public long   volume;
	
	public Price() {
		this("", "", 0, 0, 0, 0, 0);
	}
	
	public Price(String symbol, String date, double open, double high, double low, double close, long volume) {
		this.symbol = symbol;
		this.date   = date;
		this.open   = open;
		this.high   = high;
		this.low    = low;
		this.close  = close;
		this.volume = volume;
	}
	
	@Override
	public String toString() {
//		return String.format("[%s  %6.2f %6.2f %6.2f %6.2f %d]", date, open, high, low, close, volume);
		return String.format("[%-8s %s %6.2f]", symbol, date, close);
	}
}
