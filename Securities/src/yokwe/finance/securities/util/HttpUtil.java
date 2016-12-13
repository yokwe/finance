package yokwe.finance.securities.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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

	public static String downloadAsString(String url) {
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
		} catch (UnsupportedOperationException e) {
			logger.error("UnsupportedOperationException {}", e.toString());
			throw new SecuritiesException("UnsupportedOperationException");
		} catch (IOException e) {
			logger.error("IOException {}", e.toString());
			throw new SecuritiesException("IOException");
		}
	}
	
	public static byte[] downloadAsByteArray(String url) {
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
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
		    	byte[] buf = new byte[1024 * 64];
		    	try (InputStream is = entity.getContent()) {
		    		for(;;) {
		    			int len = is.read(buf);
		    			if (len == -1) break;
		    			baos.write(buf, 0, len);
		    		}
		    	}
		    	return baos.toByteArray();
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
}
