package yokwe.finance.securities.tax;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

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

	private static String getData(String symbol, String startdate, String enddate) {
		NasdaqTable nasdaq = nasdaqMap.get(symbol);
		String url = String.format("https://www.google.com/finance/historical?q=%s:%s&startdate=%s&enddate=%s&output=csv", nasdaq.exchange, nasdaq.google, startdate, enddate);
		logger.info("url {}", url);
		
		String content = HttpUtil.downloadAsString(url);
		String[] lines = content.split("\n");
		logger.info("lines {}", lines.length);
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
		return lines[1];
	}
	private static String getLastTradingDate() {
		LocalDate dateTo   = LocalDate.now(ZONEID_NEW_YORK);
		LocalDate dateFrom = dateTo.minusDays(5);
		
		String startdate = dateFrom.format(dateFormatter).replace(" ", "%20");
		String enddate   = dateTo.format(dateFormatter).replace(" ", "%20");

		String symbol = "IBM";
		String line = getData(symbol, startdate, enddate);

		logger.info("line {}", line);
		return line;
	}
	
	public static void main(String[] args) {
		logger.info("START");
		
		getLastTradingDate();

		logger.info("STOP");
	}
}
