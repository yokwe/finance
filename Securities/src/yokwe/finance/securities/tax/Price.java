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

	private static final ZoneId ZONEID_NEW_YORK = ZoneId.of("America/New_York");
	
	private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);

	private static final Map<String, NasdaqTable> nasdaqMap = NasdaqUtil.getMap();
	
	private static final int THIS_YEAR = LocalDate.now().getYear();
	private static final DateTimeFormatter PARSE_DATE  = DateTimeFormatter.ofPattern("d-MMM-yy");
	private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	
	private static final LocalDate DATE_TO   = LocalDate.now(ZONEID_NEW_YORK);
	private static final LocalDate DATE_FROM = DATE_TO.minusDays(5);
	
	private static final String START_DATE = DATE_FROM.format(dateFormatter).replace(" ", "%20");
	private static final String END_DATE   = DATE_TO.format(dateFormatter).replace(" ", "%20");
	
	private static final Map<String, Price> priceMap = new TreeMap<>();


	private static Price getPrice(String symbol, String startdate, String enddate) {
		// Convert to '-' style naming from '.PR.' for preferred stock
		if (symbol.contains(".PR.")) symbol = symbol.replace(".PR.", "-");

		if (priceMap.containsKey(symbol)) return priceMap.get(symbol);

		NasdaqTable nasdaq = nasdaqMap.get(symbol);
		String url = String.format("https://www.google.com/finance/historical?q=%s:%s&startdate=%s&enddate=%s&output=csv", nasdaq.exchange, nasdaq.google, startdate, enddate);
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
			LocalDate localDate = LocalDate.from(PARSE_DATE.parse(values[0]));
			if (THIS_YEAR < localDate.getYear()) localDate = localDate.minusYears(100);
			values[0] = FORMAT_DATE.format(localDate);
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
		priceMap.put(symbol, ret);
		return ret;
	}
	
	public static Price getPrice(String symbol) {
		return getPrice(symbol, START_DATE, END_DATE);
	}
	
	private static List<Price> getPriceList(List<String> symbolList, String startdate, String enddate) {
		List<Price> priceList = new ArrayList<>();
		for(String symbol: symbolList) {
			Price price = getPrice(symbol, startdate, enddate);
			priceList.add(price);
		}
		
		// Sanity check
		String date = priceList.get(0).date;
		for(Price price: priceList) {
			if (!price.date.equals(date)) {
				logger.error("Unexpected date {}  {}", date, price.toString());
				throw new SecuritiesException("Unexpected date");
			}
		}
		
		return priceList;
	}
	
	public static List<Price> getPriceList(List<String> symbolList) {
		return getPriceList(symbolList, START_DATE, END_DATE);
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
		
		List<Price> priceList = getPriceList(symbolList);
		for(Price price: priceList) {
			logger.info("price  {}", price.toString());
		}

		logger.info("STOP");
	}
}
