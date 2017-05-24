package yokwe.finance.securities.eod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
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
import yokwe.finance.securities.util.HttpUtil;

public class YahooQuery {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(YahooQuery.class);
	
	public enum Events {
		HISTORY("history"),
		DIVIDEND("div");
		
		public final String value;
		Events(String value) {
			this.value = value;
		}
	}

	private static final String HEADER_SET_COOKIE = "Set-Cookie";
	private static final String PATTERN_CRUMB     = "CrumbStore\\\":\\{\"crumb\":\\\"([^\"]+)\\\"\\}";
	
	private static String cookie = null;
	private static String crumb  = null;
	
	static {
		init();
	}
	
	private static void init() {
		final String urlHistoryYahoo = "https://finance.yahoo.com/quote/YHOO/history";
		
		HttpGet httpGet = new HttpGet(urlHistoryYahoo);
		httpGet.setHeader("User-Agent", "Mozilla");
		
		try (CloseableHttpClient httpClient = HttpClients.createDefault();
			CloseableHttpResponse response = httpClient.execute(httpGet)) {
			final int code = response.getStatusLine().getStatusCode();
//			logger.info("code {}", code);
			
			if (code == HttpStatus.SC_OK) {
				if (response.containsHeader(HEADER_SET_COOKIE)) {
					String cookieValues = response.getFirstHeader(HEADER_SET_COOKIE).getValue();
//						logger.info("cookieValues = {}", cookieValues);
					
					for(String keyValuePair: cookieValues.split(";")) {
						int pos = keyValuePair.indexOf("=");
						String key = keyValuePair.substring(0, pos);
						
						if (key.equals("B")) {
							cookie = keyValuePair;
//							logger.info("cookie = {}", cookie);
						}
//							logger.info("cookie {} = {}!", keyValuePair.substring(0, pos), keyValuePair.substring(pos + 1));
					}
				}
				
			    HttpEntity entity = response.getEntity();
			    if (entity != null) {
			    	Matcher matcher = Pattern.compile(PATTERN_CRUMB, 0).matcher("");
			    	
			    	try (BufferedReader br = new BufferedReader(new InputStreamReader(entity.getContent(), "UTF-8"))) {
			    		for(;;) {
			    			String line = br.readLine();
			    			if (line == null) break;
			    			
//						    	{
//						    		int pos = line.indexOf("CrumbStore");
//						    		if (0 <= pos) {
//								    	logger.info("pos = {}", pos);
//								    	logger.info("CrumbStore = {}", line.substring(pos, pos + 50));						    			
//						    		}
//						    	}

			    			if (matcher.reset(line).find()) {
			    				crumb = matcher.group(1);
			    				// Change \\u002F to / in crumb
			    				if (crumb.contains("\\u002F")) {
			    					crumb = crumb.replaceAll("\\\\u002F", "/");
			    				}
//			    				logger.info("crumb = {}", crumb);
			    			}
			    		}
			    	}
			    } else {
					logger.error("entity is null");
					throw new SecuritiesException("entity is null");
			    }
			}
			
			// Sanity check
			if (cookie == null) {
				logger.error("cookie is null");
				throw new SecuritiesException("cookie is null");
			}
			if (crumb == null) {
				logger.error("crumb is null");
				throw new SecuritiesException("crumb is null");
			}
			logger.info("cookie = {}  crumb = {}", cookie, crumb);
		} catch (UnsupportedOperationException e) {
			logger.error("UnsupportedOperationException {}", e.toString());
			throw new SecuritiesException("UnsupportedOperationException");
		} catch (IOException e) {
			logger.error("IOException {}", e.toString());
			throw new SecuritiesException("IOException");
		}
	}

	
	public static String getCookie() {
		return cookie;
	}
	
	public static String getCrumb() {
		return crumb;
	}
	
	public static String getURLPrice(LocalDate dateFrom, LocalDate dateTo, String symbol) {
		return getURL(dateFrom, dateTo, symbol, Events.HISTORY);
	}
	
	public static String getURLDividend(LocalDate dateFrom, LocalDate dateTo, String symbol) {
		return getURL(dateFrom, dateTo, symbol, Events.DIVIDEND);
	}
	
	public static String getURL(LocalDate dateFrom, LocalDate dateTo, String symbol, Events events) {
		long period1 = dateFrom.atStartOfDay(Market.ZONE_ID).plusHours(10).toEpochSecond();
		long period2 = dateTo.atStartOfDay(Market.ZONE_ID).plusHours(10).toEpochSecond();
		String eventString = events.value;
		
		String url = String.format("https://query1.finance.yahoo.com/v7/finance/download/%s?period1=%d&period2=%d&interval=1d&events=%s&crumb=%s",
				symbol, period1, period2, eventString, crumb);
		return url;
	}
	
	public static String downloadAsString(String url) {
		String content = HttpUtil.downloadAsString(url, getCookie());
		return content;
	}
	
	private static void fetch(LocalDate dateFrom, LocalDate dateTo, String symbol, Events events) {
		String url = getURL(dateFrom, dateTo, symbol, events);
		String content = HttpUtil.downloadAsString(url, getCookie());
		logger.info("content {}!", content);
	}
	
	public static void main(String[] args) {
		logger.info("START");
		LocalDate dateTo = Market.getLastTradingDate();
//		LocalDate dateFrom = dateTo.minusYears(1);
		LocalDate dateFrom = dateTo.minusDays(10);
		for(int i = 0; i < 5; i++) {
			logger.info("i = {}", i);
//			init();
			fetch(dateFrom, dateTo, "IBM", Events.HISTORY);
			fetch(dateFrom, dateTo, "IBM", Events.DIVIDEND);
		}
		logger.info("STOP");
	}
}
