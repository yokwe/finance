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

public class YahooQuery {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(YahooQuery.class);

	private static final String HEADER_SET_COOKIE = "Set-Cookie";
	private static final String PATTERN_CRUMB     = "CrumbStore\\\":\\{\"crumb\":\\\"([^\"]+)\\\"\\}";
	
	private static String cookie = null;
	private static String crumb = null;
	
	private static void fetch(LocalDate dateFrom, LocalDate dateTo, String symbol) {
		//https://query1.finance.yahoo.com/v7/finance/download/AAPL?period1=1492438581&period2=1495030581&interval=1d&events=history&crumb=XXXXXXX
		long period1 = dateFrom.atStartOfDay(Market.ZONE_ID).plusHours(10).toEpochSecond();
		long period2 = dateTo.atStartOfDay(Market.ZONE_ID).plusHours(10).toEpochSecond();
		
		String url = String.format("https://query1.finance.yahoo.com/v7/finance/download/%s?period1=%d&period2=%d&interval=1d&events=history&crumb=%s", symbol, period1, period2, crumb);
		logger.info("url {}", url);
		
		HttpGet httpGet = new HttpGet(url);
		httpGet.setHeader("User-Agent", "Mozilla");
		httpGet.setHeader("Cookie", cookie);
		
		try (CloseableHttpClient httpClient = HttpClients.createDefault();
				CloseableHttpResponse response = httpClient.execute(httpGet)) {
				final int code = response.getStatusLine().getStatusCode();
				logger.info("code {}", code);
				
			    HttpEntity entity = response.getEntity();
			    if (entity != null) {
			    	logger.info("====");
			    	try (BufferedReader br = new BufferedReader(new InputStreamReader(entity.getContent(), "UTF-8"))) {
			    		for(;;) {
			    			String line = br.readLine();
			    			if (line == null) break;
			    			logger.info("line !{}!", line);
			    		}
			    	}
			    	logger.info("====");
			    } else {
					logger.error("entity is null");
					throw new SecuritiesException("entity is null");
			    }
		} catch (UnsupportedOperationException e) {
			logger.error("UnsupportedOperationException {}", e.toString());
			throw new SecuritiesException("UnsupportedOperationException");
		} catch (IOException e) {
			logger.error("IOException {}", e.toString());
			throw new SecuritiesException("IOException");
		}

	}
	
	private static void initialize() {
		final String urlHistoryYahoo = "https://finance.yahoo.com/quote/YHOO/history";
		
		HttpGet httpGet = new HttpGet(urlHistoryYahoo);
		httpGet.setHeader("User-Agent", "Mozilla");
		
		try (CloseableHttpClient httpClient = HttpClients.createDefault();
				CloseableHttpResponse response = httpClient.execute(httpGet)) {
				final int code = response.getStatusLine().getStatusCode();
				logger.info("code {}", code);
				
				if (code == HttpStatus.SC_OK) {
					if (response.containsHeader(HEADER_SET_COOKIE)) {
						cookie = response.getFirstHeader(HEADER_SET_COOKIE).getValue();
						logger.info("cookie = {}", cookie);
						
						for(String nameValuePair: cookie.split(";")) {
							int pos = nameValuePair.indexOf("=");
							logger.info("cookie {} = {}!", nameValuePair.substring(0, pos), nameValuePair.substring(pos + 1));
						}
					}
					
				    HttpEntity entity = response.getEntity();
				    if (entity != null) {
				    	Matcher matcher = Pattern.compile(PATTERN_CRUMB, 0).matcher("");
				    	
				    	try (BufferedReader br = new BufferedReader(new InputStreamReader(entity.getContent(), "UTF-8"))) {
				    		for(;;) {
				    			String line = br.readLine();
				    			if (line == null) break;
				    			
						    	{
						    		int pos = line.indexOf("CrumbStore");
						    		if (0 <= pos) {
								    	logger.info("pos = {}", pos);
								    	logger.info("CrumbStore = {}", line.substring(pos, pos + 50));						    			
						    		}
						    	}

				    			if (matcher.reset(line).find()) {
				    				crumb = matcher.group(1);
				    				logger.info("crumb = {}", crumb);
				    			}
				    		}
				    	}
				    } else {
						logger.error("entity is null");
						throw new SecuritiesException("entity is null");
				    }
				}
		} catch (UnsupportedOperationException e) {
			logger.error("UnsupportedOperationException {}", e.toString());
			throw new SecuritiesException("UnsupportedOperationException");
		} catch (IOException e) {
			logger.error("IOException {}", e.toString());
			throw new SecuritiesException("IOException");
		}
	}
	
	public static void main(String[] args) {
		initialize();
		LocalDate dateTo = Market.getLastTradingDate();
//		LocalDate dateFrom = dateTo.minusYears(1);
		LocalDate dateFrom = dateTo.minusDays(10);
		fetch(dateFrom, dateTo, "IBM");
		fetch(dateFrom, dateTo, "NYT");
	}
}
