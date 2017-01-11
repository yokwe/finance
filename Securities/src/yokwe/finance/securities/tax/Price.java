package yokwe.finance.securities.tax;

import java.time.LocalDate;
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
import yokwe.finance.securities.util.HttpUtil;
import yokwe.finance.securities.util.NasdaqUtil;

public class Price {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Price.class);
	
	private static final List<String> SYMBOL_LIST = Arrays.asList("BT", "CSCO", "INTC");
	
	private static final DateTimeFormatter DATE_FORMAT_URL    = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);
	private static final DateTimeFormatter DATE_FORMAT_PARSE  = DateTimeFormatter.ofPattern("d-MMM-yy");
	private static final DateTimeFormatter DATE_FORMAT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	
	private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/New_York"));
	
	private static final Map<String, Price>       priceCache = new TreeMap<>();
	private static final Map<String, NasdaqTable> nasdaqMap  = NasdaqUtil.getMap();

	private static final LocalDate LAST_TRADING_DATE;
	static {
		List<Price> priceList = getPriceList(SYMBOL_LIST, TODAY.minusDays(5), TODAY);
		String date = priceList.get(0).date;
		
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
		logger.info("LAST_TRADING_DATE {}", LAST_TRADING_DATE);
	}
	
	private static Price getPrice(String symbol, LocalDate startDate, LocalDate endDate) {
		// Convert to '-' style naming from '.PR.' for preferred stock
		if (symbol.contains(".PR.")) symbol = symbol.replace(".PR.", "-");

		String dateFrom = startDate.format(DATE_FORMAT_URL).replace(" ", "%20");
		String dateTo   = endDate.format(DATE_FORMAT_URL).replace(" ", "%20");

		NasdaqTable nasdaq = nasdaqMap.get(symbol);
		String url = String.format("https://www.google.com/finance/historical?q=%s:%s&startdate=%s&enddate=%s&output=csv", nasdaq.exchange, nasdaq.google, dateFrom, dateTo);
//		logger.info("url {}", url);
		
		String content = HttpUtil.downloadAsString(url);
		String[] lines = content.split("\n");
//		logger.info("lines {}", lines.length);
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
			if (TODAY.getYear() < localDate.getYear()) localDate = localDate.minusYears(100);
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

	// Uncached version
	private static List<Price> getPriceList(List<String> symbolList, LocalDate startDate, LocalDate endDate) {
		List<Price> priceList = new ArrayList<>();
		for(String symbol: symbolList) {
			Price price = getPrice(symbol, startDate, endDate);
			priceList.add(price);
		}
		return priceList;
	}
	
	public static Price getLastPrice(String symbol) {
		Price price;
		if (priceCache.containsKey(symbol)) {
			price = priceCache.get(symbol);
		} else {
			price = getPrice(symbol, TODAY.minusDays(5), TODAY);
			
			if (!price.date.equals(LAST_TRADING_DATE.toString())) {
				logger.error("Inconsistent date");
				logger.error("{} {}", price.symbol, price.date);
				throw new SecuritiesException("Inconsistent date");
			}
			priceCache.put(symbol, price);
		}
		return price;
	}
	
	public static List<Price> getLastPriceList(List<String> symbolList) {
		List<Price> priceList = new ArrayList<>();
		for(String symbol: symbolList) {
			Price price = getLastPrice(symbol);
			priceList.add(price);
		}
		return priceList;
	}
	
	
	public final String symbol;
	public final String date;
	public final double open;
	public final double high;
	public final double low;
	public final double close;
	public final long   volume;
	
	private Price(String symbol, String date, double open, double high, double low, double close, long volume) {
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
	
	public static void main(String[] args) {
		logger.info("START");
		
		List<String> symbolList = Arrays.asList("IBM", "NYT", "PEP");
		
		List<Price> priceList = getLastPriceList(symbolList);
		for(Price price: priceList) {
			logger.info("price  {}", price.toString());
		}

		logger.info("STOP");
	}
}
