package yokwe.finance.securities.eod;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.DoubleUtil;
import yokwe.finance.securities.util.HttpUtil;
import yokwe.finance.securities.util.Pause;

public final class UpdateProviderGoogle implements UpdateProvider {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateProviderGoogle.class);

	private static final long   MIN_SLEEP_INTERVAL = 600; // 600 milliseconds = 0.6 sec
	private static final Pause  PAUSE              = Pause.getInstance(MIN_SLEEP_INTERVAL);

	public Pause getPause() {
		return PAUSE;
	}
	
	public String getRootPath() {
		return UpdatePrice.PATH_DIR;
	}
	public String getName() {
		return GOOGLE;
	}
	
	public File getFile(String symbol) {
		String path = String.format("%s-%s/%s.csv", UpdatePrice.PATH_DIR, getName(), symbol);
		File file = new File(path);
		return file;
	}

	private static final DateTimeFormatter DATE_FORMAT_URL    = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);
	private static final DateTimeFormatter DATE_FORMAT_PARSE  = DateTimeFormatter.ofPattern("d-MMM-yy");
	private static final DateTimeFormatter DATE_FORMAT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	public boolean updateFile(String symbol, boolean newFile, LocalDate dateFirst, LocalDate dateLast) {
		// Convert from '.PR.' to  '-' in symbol for google
		Stock stock = StockUtil.get(symbol.replace(".PR.", "-"));
		return updateFile(stock.exchange, stock.symbol, stock.symbolGoogle, newFile, dateFirst, dateLast);
	}
	
	public boolean updateFile(String exch, String symbol, String symbolURL, boolean newFile, LocalDate dateFirst, LocalDate dateLast) {
		File file = getFile(symbol);
		
		String dateFrom = dateFirst.format(DATE_FORMAT_URL).replace(" ", "%20").replace(",", "%2C");
		String dateTo   = dateLast.format(DATE_FORMAT_URL).replace(" ", "%20").replace(",", "%2C");
		
		// http://www.google.com/finance/historical?q=NASDAQ:ACGLP&startdate=Mar%2027%2C%202016&enddate=Mar+27,+2017&output=csv
		// TODO need to update for new URL
		//   1)Retrieve stock page
		//     https://finance.google.com/finance?q=NYSE:PCI
		//   2)Find character sequence "_chartConfigObject.companyId = '702671128483068';" from stock page and extract companyId and use as cid.
		//   3)Generate URL below
		//     http://finance.google.com/finance/historical?cid=702671128483068&startdate=Nov+22%2C+2016&enddate=Nov+21%2C+2017&output=csv
		String url = String.format("https://finance.google.com/finance/historical?q=%s:%s&startdate=%s&enddate=%s&output=csv", exch, symbolURL, dateFrom, dateTo);

		String content = HttpUtil.downloadAsString(url);
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
		String GOOGLE_PRICE_HEADER = "\uFEFFDate,Open,High,Low,Close,Volume";
		String header = lines[0];
		if (!header.equals(GOOGLE_PRICE_HEADER)) {
			UpdatePrice.logger.error("Unexpected header  symbol {}", symbol);
//				logger.error("Unexpected header  url    {}", url);
//				logger.error("Unexpected header  header {}", header);
//				throw new SecuritiesException("Unexpected header");
			file.delete();
			return false;
		}

		String      targetDate  = DATE_LAST.toString();
		boolean     targetFound = false;
		List<Price> priceList   = new ArrayList<>();
		
		for(String line: lines) {
			if (line.startsWith("\uFEFFDate,Open,High,Low,Close,Volume")) continue;
			
			String[] values = line.split(",");
			if (values.length != 6) {
				UpdatePrice.logger.error("Unexpected line  {}", line);
				throw new SecuritiesException("Unexpected header");
			}
			
			// Fix format of date  02-Jan-14 => 2014-01-02
			// Fix year of date    02-Jan-80 => 1980-01-02
			{
				LocalDate localDate = LocalDate.from(DATE_FORMAT_PARSE.parse(values[0]));
				if (dateLast.getYear() < localDate.getYear()) localDate = localDate.minusYears(100);
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
			double open   = DoubleUtil.round(Double.valueOf(values[1]), 2);
			double high   = DoubleUtil.round(Double.valueOf(values[2]), 2);
			double low    = DoubleUtil.round(Double.valueOf(values[3]), 2);
			double close  = DoubleUtil.round(Double.valueOf(values[4]), 2);
			long   volume = Long.valueOf(values[5]);
			
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