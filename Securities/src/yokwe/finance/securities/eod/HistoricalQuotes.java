package yokwe.finance.securities.eod;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.util.CSVUtil;

public class HistoricalQuotes {
	public String date;
	public double close;
	public String volume;
	public double open;
	public double high;
	public double low;
	
	public HistoricalQuotes() {
		this("", 0, "", 0, 0, 0);
	}
	
	public HistoricalQuotes(String date, double close, String volume, double open, double high, double low) {
		this.date   = date;
		this.close  = close;
		this.volume = volume;
		this.open   = open;
		this.high   = high;
		this.low    = low;
	}
	
	@Override
	public String toString() {
//		return String.format("[%s  %6.2f %6.2f %6.2f %6.2f %d]", date, open, high, low, close, volume);
		return String.format("[%-9s %6.2f]", date, close);
	}
	
	public static List<HistoricalQuotes> load(String path) {
		return CSVUtil.loadWithHeader(path, HistoricalQuotes.class);
	}
	
	private static void process(String symbol, String path) {
		final org.slf4j.Logger logger = LoggerFactory.getLogger(HistoricalQuotes.class);
		
		DateTimeFormatter DATE_FORMAT_PARSE  = DateTimeFormatter.ofPattern("yyyy/MM/dd");
						
		String PATH_OUTPUT = String.format("tmp/eod/%s.csv", symbol);

		logger.info("PATH_INPUT  {}", path);
		logger.info("PATH_OUTPUT {}", PATH_OUTPUT);
		
		LocalDate DATE_LAST = Market.getLastTradingDate();
		logger.info("last trading date  {}", DATE_LAST);
		
		List<Price> priceList = new ArrayList<>();
		
		List<HistoricalQuotes> hitoricalQuoteList = load(path);
		for(HistoricalQuotes hitoricalQuote: hitoricalQuoteList) {
			LocalDate date;
			long volume;
			
			if (hitoricalQuote.date.equals("16:00")) {
				date = DATE_LAST;
				volume = Long.valueOf(hitoricalQuote.volume.replace(",", ""));
			} else {
				date = LocalDate.from(DATE_FORMAT_PARSE.parse(hitoricalQuote.date));
				volume = Math.round(Double.valueOf(hitoricalQuote.volume.replace(",", "")));
			}
			priceList.add(new Price(date.toString(), symbol, hitoricalQuote.open, hitoricalQuote.high, hitoricalQuote.low, hitoricalQuote.close, volume));
		}
		// sort with date
		priceList.sort((a, b) -> -a.date.compareTo(b.date));
		
		logger.info("priceList size  {}", priceList.size());
		logger.info("priceList last  {}", priceList.get(0));
		logger.info("priceList first {}", priceList.get(priceList.size() - 1));
		
		// save
		Price.save(priceList, new File(PATH_OUTPUT));
	}
	
	public static void main(String[] args) {
		final org.slf4j.Logger logger = LoggerFactory.getLogger(HistoricalQuotes.class);
		
		String PATH_DIR  = "tmp/eod/";
		Matcher matcher = Pattern.compile("HistoricalQuotes-([A-Z]+).csv").matcher("");
		
		logger.info("START");
		
		for(File file: new File(PATH_DIR).listFiles()) {
			String name = file.getName();
			boolean found = matcher.reset(name).find();
			if (found) {
				String symbol = matcher.group(1);
				String path   = file.getPath();
				
				process(symbol, path);
			}
		}
		
		logger.info("STOP");
	}
}