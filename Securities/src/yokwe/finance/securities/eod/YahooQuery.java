package yokwe.finance.securities.eod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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

	private static final int    MAX_RETRY_COUNT   = 10;
	private static final String HEADER_SET_COOKIE = "Set-Cookie";
	private static final String PATTERN_CRUMB     = "CrumbStore\\\":\\{\"crumb\":\\\"([^\"]+)\\\"\\}";
		
	private String cookie;
	private String crumb;
	
	private YahooQuery() {
		cookie    = "A=A";
		crumb     = "aabbccdee";
	}
	
	public static YahooQuery getInstance() {
		YahooQuery yahooQuery = new YahooQuery();
		return yahooQuery;
	}
	
	public String downloadPrice(LocalDate dateFrom, LocalDate dateTo, String symbol) {
		return download(dateFrom, dateTo, symbol, "history");
	}
	public String downloadDividend(LocalDate dateFrom, LocalDate dateTo, String symbol) {
		return download(dateFrom, dateTo, symbol, "dividend");
	}
	public String download(LocalDate dateFrom, LocalDate dateTo, String symbol, String event) {
		int retryCount = 0;
		String symbolURL = StockUtil.get(symbol).symbolYahoo;
		for(;;) {
			String url = getURL(dateFrom, dateTo, symbolURL, event);
			HttpGet httpGet = new HttpGet(url);
			httpGet.setHeader("User-Agent", "Mozilla");
			httpGet.setHeader("Cookie", cookie);
			
			try (CloseableHttpClient httpClient = HttpClients.createDefault();
				CloseableHttpResponse response = httpClient.execute(httpGet)) {
				final int code = response.getStatusLine().getStatusCode();
				
				if (code == HttpStatus.SC_UNAUTHORIZED) {
					if (retryCount < MAX_RETRY_COUNT) {
						retryCount++;
//						logger.warn("retry {} {} {}  {}", retryCount, code, reasonPhrase, url);
						logger.info("retry {} - {}", retryCount, symbol);
						Thread.sleep(1000 * retryCount * retryCount); // sleep 1 * retryCount * retryCount sec
						init();
						continue;
					} else {
						logger.error("Exceeds max retry count {}", MAX_RETRY_COUNT);
						throw new SecuritiesException("Exceeds max retry count");
					}
				}
				if (code == HttpStatus.SC_NOT_FOUND) { // 404
//					logger.warn("{} {}  {}", code, reasonPhrase, url);
					return null;
				}
				if (code == HttpStatus.SC_OK) {
				    HttpEntity entity = response.getEntity();
				    if (entity != null) {
						StringBuilder ret = new StringBuilder();
				    	char[] cbuf = new char[1024 * 64];
				    	try (InputStreamReader isr = new InputStreamReader(entity.getContent(), "UTF-8")) {
				    		for(;;) {
				    			int len = isr.read(cbuf);
				    			if (len == -1) break;
				    			ret.append(cbuf, 0, len);
				    		}
				    	}
				    	return ret.toString();
				    } else {
						logger.error("entity is null");
						throw new SecuritiesException("entity is null");
				    }
				}
				
				// Other code
				logger.error("statusLine = {}", response.getStatusLine().toString());
				logger.error("url {}", url);
				logger.error("code {}", code);
				throw new SecuritiesException("download");

			} catch (UnsupportedOperationException e) {
				logger.error("UnsupportedOperationException {}", e.toString());
				throw new SecuritiesException("UnsupportedOperationException");
			} catch (IOException e) {
				logger.error("IOException {}", e.toString());
				throw new SecuritiesException("IOException");
			} catch (InterruptedException e) {
				logger.error("InterruptedException {}", e.toString());
				throw new SecuritiesException("InterruptedException");
			}
		}
	}
	
	public String getURL(LocalDate dateFrom, LocalDate dateTo, String symbol, String event) {
		try {
			long period1 = dateFrom.atStartOfDay(Market.ZONE_ID).plusHours(10).toEpochSecond();
			long period2 = dateTo.atStartOfDay(Market.ZONE_ID).plusHours(10).toEpochSecond();
			String encodedSymbol = URLEncoder.encode(symbol, "UTF-8");
			String url = String.format("https://query1.finance.yahoo.com/v7/finance/download/%s?period1=%d&period2=%d&interval=1d&events=%s&crumb=%s",
					encodedSymbol, period1, period2, event, crumb);
			return url;
		} catch (UnsupportedEncodingException e) {
			logger.error("UnsupportedEncodingException {}", e.toString());
			throw new SecuritiesException("UnsupportedEncodingException");
		}
	}

	
	private void init() {
		final String urlHistoryYahoo = "https://finance.yahoo.com/lookup?s=YHOO";
		
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
			logger.info("cookie = {}", cookie);
			logger.info("crumb  = {}", crumb);
		} catch (UnsupportedOperationException e) {
			logger.error("UnsupportedOperationException {}", e.toString());
			throw new SecuritiesException("UnsupportedOperationException");
		} catch (IOException e) {
			logger.error("IOException {}", e.toString());
			throw new SecuritiesException("IOException");
		}
	}

	
	public static void main(String[] args) {
		logger.info("START");
		LocalDate dateTo = Market.getLastTradingDate();
//		LocalDate dateFrom = dateTo.minusYears(1);
		LocalDate dateFrom = dateTo.minusDays(10);
		YahooQuery yq = YahooQuery.getInstance();
		for(int i = 0; i < 2000; i++) {
			String price = yq.downloadPrice(dateFrom, dateTo, "IBM");
			logger.info("i = {}  {}", i, price.length());
		}
		logger.info("STOP");
	}
}
