package yokwe.finance.securities.eod;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.DoubleUtil;
import yokwe.finance.securities.util.HttpUtil;
import yokwe.finance.securities.util.Pause;

public final class UpdateProviderIEX implements UpdateProvider {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateProviderIEX.class);

	private static final long   PAUSE_INTERVAL = 1000; // 1000 milliseconds = 1 sec
	private static final double PAUSE_JITTER   = 0.2;  // 0.2 = 20%
	private static final Pause  PAUSE          = Pause.getInstance(PAUSE_INTERVAL, PAUSE_JITTER);
	
	private static interface UpdatePriceList {
		public void update(List<Price> priceList, String symbol, String line);
	}
	
	private static final class PricessHeader1 implements UpdateProviderIEX.UpdatePriceList {
		public static final String HEADER = "date,open,high,low,close,volume,unadjustedVolume,change,changePercent,vwap,label,changeOverTime";
		public static final int    LENGTH = HEADER.split(",").length;
		
		public void update(List<Price> priceList, String symbol, String line) {
			if (line.equals(HEADER)) return;
			
			String[] values = line.split(",");
			
			if (!(values.length == LENGTH || values.length == (LENGTH + 1))) {
				UpdatePrice.logger.error("Unexpected line  {}  {} = {}", symbol, values.length, line);
				throw new SecuritiesException("Unexpected line");
			}

			// Special when field (open, high and low) is empty
			if (values[1].equals("")) {
				values[1] = values[4];
			}
			if (values[2].equals("")) {
				values[2] = values[4];
			}
			if (values[3].equals("")) {
				values[3] = values[4];
			}

			String date   = values[0];
			double open   = DoubleUtil.round(Double.valueOf(values[1]), 2);
			double high   = DoubleUtil.round(Double.valueOf(values[2]), 2);
			double low    = DoubleUtil.round(Double.valueOf(values[3]), 2);
			double close  = DoubleUtil.round(Double.valueOf(values[4]), 2);
			long   volume = Long.valueOf(values[5]);
			
			priceList.add(new Price(date, symbol, open, high, low, close, volume));
		}
	}
	
	private static final class PricessHeader2 implements UpdateProviderIEX.UpdatePriceList {
		public static final String HEADER = "date,open,high,low,close,volume,unadjustedVolume,change,changePercent,label,changeOverTime";
		public static final int    LENGTH = HEADER.split(",").length;
		
		public void update(List<Price> priceList, String symbol, String line) {
			if (line.equals(HEADER)) return;
			
			String[] values = line.split(",");
			
			if (!(values.length == LENGTH || values.length == (LENGTH + 1))) {
				UpdatePrice.logger.error("Unexpected line  {}  {} = {}", symbol, values.length, line);
				throw new SecuritiesException("Unexpected line");
			}
			
			// Special when field (open, high and low) is empty
			if (values[1].equals("")) {
				values[1] = values[4];
			}
			if (values[2].equals("")) {
				values[2] = values[4];
			}
			if (values[3].equals("")) {
				values[3] = values[4];
			}

			String date   = values[0];
			double open   = DoubleUtil.round(Double.valueOf(values[1]), 2);
			double high   = DoubleUtil.round(Double.valueOf(values[2]), 2);
			double low    = DoubleUtil.round(Double.valueOf(values[3]), 2);
			double close  = DoubleUtil.round(Double.valueOf(values[4]), 2);
			long   volume = Long.valueOf(values[5]);
			
			priceList.add(new Price(date, symbol, open, high, low, close, volume));
		}
	}

	private static final class PricessHeader3 implements UpdateProviderIEX.UpdatePriceList {
		public static final String HEADER = "date,high,low,close,volume,unadjustedVolume,change,changePercent,vwap,label,changeOverTime";
		public static final int    LENGTH = HEADER.split(",").length;
		
		public void update(List<Price> priceList, String symbol, String line) {
			if (line.equals(HEADER)) return;
			
			String[] values = line.split(",");
			
			if (!(values.length == LENGTH || values.length == (LENGTH + 1))) {
				UpdatePrice.logger.error("Unexpected line  {}  {} = {}", symbol, values.length, line);
				throw new SecuritiesException("Unexpected line");
			}
			
			// Special when field (open, high and low) is empty
			if (values[1].equals("")) {
				values[1] = values[3];
			}
			if (values[2].equals("")) {
				values[2] = values[3];
			}

			String date   = values[0];
			double high   = DoubleUtil.round(Double.valueOf(values[1]), 2);
			double low    = DoubleUtil.round(Double.valueOf(values[2]), 2);
			double close  = DoubleUtil.round(Double.valueOf(values[3]), 2);
			long   volume = Long.valueOf(values[4]);
			
			//
			double open   = close;
			
			priceList.add(new Price(date, symbol, open, high, low, close, volume));
		}
	}

	private static final class PricessHeader4 implements UpdateProviderIEX.UpdatePriceList {
		public static final String HEADER = "date,close,volume,unadjustedVolume,change,changePercent,label,changeOverTime";
		public static final int    LENGTH = HEADER.split(",").length;
		
		public void update(List<Price> priceList, String symbol, String line) {
			if (line.equals(HEADER)) return;
			
			String[] values = line.split(",");
			
			if (!(values.length == LENGTH || values.length == (LENGTH + 1))) {
				UpdatePrice.logger.error("Unexpected line  {}  {} = {}", symbol, values.length, line);
				throw new SecuritiesException("Unexpected line");
			}
			

			String date   = values[0];
			double close  = DoubleUtil.round(Double.valueOf(values[1]), 2);
			long   volume = Long.valueOf(values[2]);
			
			//
			double open   = close;
			double high   = close;
			double low    = close;
			
			priceList.add(new Price(date, symbol, open, high, low, close, volume));
		}
	}

	
	private static final Map<String, UpdateProviderIEX.UpdatePriceList> updatePriceListMap = new TreeMap<>();
	static {
		updatePriceListMap.put(PricessHeader1.HEADER, new PricessHeader1());
		updatePriceListMap.put(PricessHeader2.HEADER, new PricessHeader2());
		updatePriceListMap.put(PricessHeader3.HEADER, new PricessHeader3());
		updatePriceListMap.put(PricessHeader4.HEADER, new PricessHeader4());
	}

	public Pause getPause() {
		return PAUSE;
	}
	
	public String getRootPath() {
		return UpdatePrice.PATH_DIR;
	}
	public String getName() {
		return IEX;
	}
	
	public File getFile(String symbol) {
		String path = String.format("%s-%s/%s.csv", UpdatePrice.PATH_DIR, getName(), symbol);
		File file = new File(path);
		return file;
	}

	public boolean updateFile(String symbol, boolean newFile, LocalDate dateFirst, LocalDate dateLast) {
		// Convert from '.PR.' to  '-' in symbol for google
		Stock stock = StockUtil.get(symbol.replace(".PR.", "-"));
		return updateFile(stock.exchange, stock.symbol, stock.symbolGoogle, newFile, dateFirst, dateLast);
	}
	
	public boolean updateFile(String exch, String symbol, String symbolURL, boolean newFile, LocalDate dateFirst, LocalDate dateLast) {
//			logger.debug("update {}", symbol);
		File file = getFile(symbol);
		
		// FIXME Assume period is 1 year
		// https://api.iextrading.com/1.0/stock/AA/chart/1y?format=csv
		String encodedSymbol;
		try {
			encodedSymbol = URLEncoder.encode(symbol, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			UpdatePrice.logger.error("UnsupportedEncodingException {}", e.toString());
			throw new SecuritiesException("UnsupportedEncodingException");
		}
		String url = String.format("https://api.iextrading.com/1.0/stock/%s/chart/1y?format=csv",encodedSymbol);

		String content = HttpUtil.downloadAsString(url);
		if (content == null) {
			// cannot get content
			file.delete();
			return false;
		}
		
		String[] lines = content.split("\r\n");
//			logger.info("lines {}", lines.length);
		if (lines.length <= 1) {
			// only header
			file.delete();
			return false;
		}
		
		// Sanity check
		List<Price> priceList   = new ArrayList<>();
		
		String header = lines[0];

		updatePriceListMap.containsKey(header);
		if (updatePriceListMap.containsKey(header)) {
			
			UpdateProviderIEX.UpdatePriceList updatePriceList = updatePriceListMap.get(header);
			for(String line: lines) {
				if (line.equals(header)) continue;
				updatePriceList.update(priceList, symbol, line);
			}

		} else {
			UpdatePrice.logger.error("Unexpected header  {}  {}", symbol, header);
			throw new SecuritiesException("Unexpected header");
		}
		
		{
			String      targetDate  = DATE_LAST.toString();
			boolean     targetFound = false;
			
			for(Price price: priceList) {
				if (price.date.equals(targetDate)) {
					targetFound = true;
					break;
				}
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
}