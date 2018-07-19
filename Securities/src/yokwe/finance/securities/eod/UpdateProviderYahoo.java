package yokwe.finance.securities.eod;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.DoubleUtil;
import yokwe.finance.securities.util.Pause;

public final class UpdateProviderYahoo implements UpdateProvider {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateProviderYahoo.class);

	private static final long   PAUSE_INTERVAL = 1000; // 1000 milliseconds = 1 sec
	private static final double PAUSE_JITTER   = 0.2;  // 0.2 = 20%
	private static final Pause  PAUSE          = Pause.getInstance(PAUSE_INTERVAL, PAUSE_JITTER);

	public Pause getPause() {
		return PAUSE;
	}

	public String getName() {
		return YAHOO;
	}
	
	public File getFile(String symbol) {
		String path = String.format("%s-%s/%s.csv", UpdatePrice.PATH_DIR, getName(), symbol);
		File file = new File(path);
		return file;
	}
	
	public boolean updateFile(String symbol, boolean newFile, LocalDate dateFirst, LocalDate dateLast) {
		Stock stock = StockUtil.get(symbol.replace(".PR.", "-"));
		return updateFile(stock.exchange, stock.symbol, stock.symbolYahoo, newFile, dateFirst, dateLast);
	}		

	private YahooQuery yahooQuery = YahooQuery.getInstance();
	
	public boolean updateFile(String exch, String symbol, String symbolURL, boolean newFile, LocalDate dateFirst, LocalDate dateLast) {
		File file = getFile(symbol);
		
		String content = yahooQuery.downloadPrice(dateFirst, dateLast, symbol);
		if (content == null) {
			// cannot get content
			file.delete();
			return false;
		}
		
		String[] lines = content.split("\n");
//			logger.info("lines {}", lines.length);
		if (lines.length <= 1) {
			// only header
			file.delete();
			return false;
		}
		
		// Sanity check
		
		String YAHOO_PRICE_HEADER = "Date,Open,High,Low,Close,Adj Close,Volume";
		String header = lines[0];
		if (!header.equals(YAHOO_PRICE_HEADER)) {
			UpdatePrice.logger.error("Unexpected header  {}", header);
			throw new SecuritiesException("Unexpected header");
		}

		String      targetDate  = dateLast.toString();
		boolean     targetFound = false;
		List<Price> priceList   = new ArrayList<>();
		
		for(String line: lines) {
			if (line.startsWith(YAHOO_PRICE_HEADER)) continue;
			if (line.contains("null")) continue;
			
			String[] values = line.split(",");
			if (values.length != 7) {
				UpdatePrice.logger.error("Unexpected line  {}", line);
				throw new SecuritiesException("Unexpected header");
			}
//				logger.info("line = {}!", line);
			
			String date   = values[0];
			double open   = DoubleUtil.round(Double.valueOf(values[1]), 2);
			double high   = DoubleUtil.round(Double.valueOf(values[2]), 2);
			double low    = DoubleUtil.round(Double.valueOf(values[3]), 2);
			double close  = DoubleUtil.round(Double.valueOf(values[4]), 2);
			long   volume = Long.valueOf(values[6]);
			
			if (date.equals(targetDate)) targetFound = true;
			
			priceList.add(new Price(date, symbol, open, high, low, close, volume));
		}
		
		if (targetFound || (newFile && 1 < lines.length)) {
			priceList.sort((a, b) -> -a.date.compareTo(b.date));
			Price.save(priceList, file);
			return true;
		} else {
			// no target date data
			// file.delete(); // keep old file
			return false;
		}
	}
}