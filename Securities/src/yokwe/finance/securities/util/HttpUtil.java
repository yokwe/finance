package yokwe.finance.securities.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class HttpUtil {
	private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);

	public static List<String> download(String url) {
		List<String> ret = new ArrayList<>();
		
		HttpGet httpGet = new HttpGet(url);
		httpGet.setHeader("User-Agent", "Mozilla");
		try (CloseableHttpClient httpClient = HttpClients.createDefault();
			CloseableHttpResponse response = httpClient.execute(httpGet)) {
			final int code = response.getStatusLine().getStatusCode();
			final String reasonPhrase = response.getStatusLine().getReasonPhrase();
			
			if (code == HttpStatus.SC_NOT_FOUND) { // 404
				logger.warn("{} {}  {}", code, reasonPhrase, url);
				return null;
			}
			if (code == HttpStatus.SC_BAD_REQUEST) { // 400
				logger.warn("{} {}  {}", code, reasonPhrase, url);
				return null;
			}
			if (code != HttpStatus.SC_OK) { // 200
				logger.error("statusLine = {}", response.getStatusLine().toString());
				logger.error("url {}", url);
				logger.error("code {}", code);
				throw new SecuritiesException("download");
			}
			
		    HttpEntity entity = response.getEntity();
		    if (entity != null) {
		    	try (BufferedReader br = new BufferedReader(new InputStreamReader(entity.getContent(), "UTF-8"), 64 * 1024)) {
		    		for(;;) {
		    			String line = br.readLine();
		    			if (line == null) break;
		    			ret.add(line);
		    		}
		    	}
		    } else {
		    	//
		    }
			if (ret.size() == 0) {
				logger.error("statusLine = {}", response.getStatusLine().toString());
				logger.error("url {}", url);
				logger.error("code {}", code);
				throw new SecuritiesException();
			}
		} catch (UnsupportedOperationException e) {
			logger.error("UnsupportedOperationException {}", e.toString());
			throw new SecuritiesException("UnsupportedOperationException");
		} catch (IOException e) {
			logger.error("IOException {}", e.toString());
			throw new SecuritiesException("IOException");
		}
		return ret;
	}
}
