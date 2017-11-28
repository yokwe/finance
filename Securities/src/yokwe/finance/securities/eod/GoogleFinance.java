package yokwe.finance.securities.eod;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class GoogleFinance {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(GoogleFinance.class);

	private static final String USER_AGENT = "Mozilla";
	private static final String HEADER_SET_COOKIE = "Set-Cookie";
	private static final String URL_GOOGLE_FINANCE = "https://finance.google.com/finance";
	private static final Match EI = new Match("_setStickyUrlParams\\(\"ei=(.+?)\"\\);");
	private static final Match COOKIE = new Match("(.+?=.+?);");
	
	private static CloseableHttpClient httpClient = HttpClients.createSystem();
	private static String cookie;
	private static String ei;
	
	static {
		init();
	}
	
	private static class Match {
		final Matcher matcher;
		
		Match(String pattern) {
			matcher = Pattern.compile(pattern).matcher("");
		}
		String getValue(String string) {
			boolean found = matcher.reset(string).find();
			if (!found) {
				logger.error("No match {}", matcher);
				throw new SecuritiesException("No match");
			}
			String ret = matcher.group(1);
			return ret;
		}
	}
	
	private static void init() {
		HttpGet httpGet = new HttpGet(URL_GOOGLE_FINANCE);
		httpGet.setHeader("User-Agent", USER_AGENT);
		
		try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
			final int code = response.getStatusLine().getStatusCode();
			
			if (code != HttpStatus.SC_OK) {
				logger.error("Unexpected code {}", code);
				throw new SecuritiesException("Unexpected code");
			}
			
			if (!response.containsHeader(HEADER_SET_COOKIE)) {
				logger.error("Unexpected no set-cookie");
				throw new SecuritiesException("Unexpected no set-cookie");
			}
			
			{
				String setCookie = response.getFirstHeader(HEADER_SET_COOKIE).getValue();
//				logger.info("setCookie = {}", setCookie);
				cookie = COOKIE.getValue(setCookie);
				logger.info("cookie = {}", cookie);
			}
			
			{
			    HttpEntity entity = response.getEntity();
			    if (entity == null) {
					logger.error("entity is null");
					throw new SecuritiesException("entity is null");
			    }

			    StringBuilder ret = new StringBuilder();
		    	char[] cbuf = new char[1024 * 64];
		    	try (InputStreamReader isr = new InputStreamReader(entity.getContent(), "UTF-8")) {
		    		for(;;) {
		    			int len = isr.read(cbuf);
		    			if (len == -1) break;
		    			ret.append(cbuf, 0, len);
		    		}
		    	}
			    String contents = ret.toString();
//		    	logger.info("contents {}", contents);
				ei = EI.getValue(contents);
			}
			logger.info("ei = {}", ei);
		} catch (UnsupportedOperationException e) {
			logger.error("UnsupportedOperationException {}", e.toString());
			throw new SecuritiesException("UnsupportedOperationException");
		} catch (IOException e) {
			logger.error("IOException {}", e.toString());
			throw new SecuritiesException("IOException");
		}
	}
	
	
	private static String getContents(String url) {
		logger.info("url = {}", url);
		
		HttpGet httpGet = new HttpGet(url);
		httpGet.setHeader("User-Agent", USER_AGENT);
		httpGet.setHeader("Cookie", cookie);
		
		try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
			final int code = response.getStatusLine().getStatusCode();
			
			logger.info("code = {}", code);
			
			if (code != HttpStatus.SC_OK) {
				logger.error("Unexpected code {}", code);
				throw new SecuritiesException("Unexpected code");
			}
			
			if (response.containsHeader(HEADER_SET_COOKIE)) {
				String setCookie = response.getFirstHeader(HEADER_SET_COOKIE).getValue();
				logger.info("setCookie = {}", setCookie);
				cookie = COOKIE.getValue(setCookie);
				logger.info("cookie = {}", cookie);
			}
			
			{
			    HttpEntity entity = response.getEntity();
			    if (entity == null) {
					logger.error("entity is null");
					throw new SecuritiesException("entity is null");
			    }

			    StringBuilder ret = new StringBuilder();
		    	char[] cbuf = new char[1024 * 64];
		    	try (InputStreamReader isr = new InputStreamReader(entity.getContent(), "UTF-8")) {
		    		for(;;) {
		    			int len = isr.read(cbuf);
		    			if (len == -1) break;
		    			ret.append(cbuf, 0, len);
		    		}
		    	}
		    	logger.info("ret = {}", ret.length());
		    	return ret.toString();
			}
		} catch (UnsupportedOperationException e) {
			logger.error("UnsupportedOperationException {}", e.toString());
			throw new SecuritiesException("UnsupportedOperationException");
		} catch (IOException e) {
			logger.error("IOException {}", e.toString());
			throw new SecuritiesException("IOException");
		}
	}
	
	private static Map<String, CID> cidMap = new TreeMap<>();
	static {
		for(CID cid: CID.load()) {
			cidMap.put(cid.symbol, cid);
		}
	}

	private static final DateTimeFormatter DATE_FORMAT_URL    = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);
	
	public static String downloadPrice(String exchange, String symbolGoogle, LocalDate dateFirst, LocalDate dateLast) {
		String dateFrom = dateFirst.format(DATE_FORMAT_URL).replace(" ", "%20").replace(",", "%2C");
		String dateTo   = dateLast.format(DATE_FORMAT_URL).replace(" ", "%20").replace(",", "%2C");
		
		if (!cidMap.containsKey(symbolGoogle)) {
			logger.error("No cid  symbolGoogle = {}", symbolGoogle);
			throw new SecuritiesException("No cid");
		}
		CID cid = cidMap.get(symbolGoogle);
		if (!cid.exchange.equals(exchange)) {
			logger.error("Not same exchange  {} - {}", exchange, cid.exchange);
			throw new SecuritiesException("Not same exchange");
		}
		
		// http://finance.google.com/finance/historical?cid=702671128483068&startdate=Nov+22%2C+2016&enddate=Nov+21%2C+2017&output=csv
		String url = String.format("%s/historical?cid=%s&startdate=%s&enddate=%s&output=csv&ei=%s", URL_GOOGLE_FINANCE, cid.cid, dateFrom, dateTo, ei);
		String contents = getContents(url);
		
		logger.info("contents = {}", contents);
		
		return null;
	}

	
	public static void main(String[] args) {
		logger.info("START");
		{
//			String contents = getContents("NYSE", "IBM");
//			logger.info("{}", contents);
			
			LocalDate dateTo = Market.getLastTradingDate();
			LocalDate dateFrom = dateTo.minusYears(1);
//			LocalDate dateFrom = dateTo.minusDays(10);

			downloadPrice("NASDAQ", "GMLPP", dateFrom, dateTo);
		}
		logger.info("STOP");
	}
}
