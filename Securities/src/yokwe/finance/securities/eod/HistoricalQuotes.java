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
//	date,open,high,low,close,volume,unadjustedVolume,change,changePercent,vwap,label,changeOverTime
//	2017-06-12,5.19,5.38,5.035,5.05,755186,755186,-0.14,-2.697,5.1207,"Jun 12, 17",0

	public String date;
	public double open;
	public double high;
	public double low;	
	public double close;
	public long   volume;
	
	public double unadjustedVolume;
	public double change;
	public double changePercent;
	public double vwap;
	public String label;
	public double changeOverTime;
	
	
	public HistoricalQuotes() {
		this("", 0, 0, 0, 0, 0, 0, 0, 0, 0, "", 0);
	}
	
	public HistoricalQuotes(String date, double open, double high, double low, double close, long volume,
			double unadjustedVolume, double change, double changePercent, double vwap, String label, double changeOverTime) {
		this.date   = date;
		this.open   = open;
		this.high   = high;
		this.low    = low;
		this.close  = close;
		this.volume = volume;
		
		this.unadjustedVolume = unadjustedVolume;
		this.change           = change;
		this.changePercent    = changePercent;
		this.vwap             = vwap;
		this.label            = label;
		this.changeOverTime   = changeOverTime;		
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
		
		DateTimeFormatter DATE_FORMAT_PARSE  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
						
		String PATH_OUTPUT = String.format("tmp/eod/%s.csv", symbol);

		logger.info("PATH_INPUT  {}", path);
		logger.info("PATH_OUTPUT {}", PATH_OUTPUT);
		
		LocalDate DATE_LAST = Market.getLastTradingDate();
		logger.info("last trading date  {}", DATE_LAST);
		
		List<Price> priceList = new ArrayList<>();
		
		List<HistoricalQuotes> hitoricalQuoteList = load(path);
		for(HistoricalQuotes hitoricalQuote: hitoricalQuoteList) {
			LocalDate date;
			
			date = LocalDate.from(DATE_FORMAT_PARSE.parse(hitoricalQuote.date));
			priceList.add(new Price(date.toString(), symbol, hitoricalQuote.open, hitoricalQuote.high, hitoricalQuote.low, hitoricalQuote.close, hitoricalQuote.volume));
		}
		// sort with date
		priceList.sort((a, b) -> -a.date.compareTo(b.date));
		
		logger.info("priceList size  {}", priceList.size());
		logger.info("priceList last  {}", priceList.get(0));
		logger.info("priceList first {}", priceList.get(priceList.size() - 1));
		
		// save
		UpdatePrice.save(priceList, new File(PATH_OUTPUT));
	}
	
	public static void main(String[] args) {
		final org.slf4j.Logger logger = LoggerFactory.getLogger(HistoricalQuotes.class);
		
		String PATH_DIR  = "tmp/eod/";
		// IEX_stock_GNRT_chart_1y.csv
		Matcher matcher = Pattern.compile("IEX_stock_([A-Z]+)_chart_1y.csv").matcher("");
		
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