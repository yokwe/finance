package yokwe.finance.stock.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.stock.UnexpectedException;

public class HttpUtil {
	private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);
	
	private static final String USER_AGENT = "Mozilla";
	
	private static final String DEFAULT_ENCODING = "UTF-8";

	private static final int CONNECTION_POOLING_MAX_TOTAL = 5;

	private static CloseableHttpClient httpClient;
	static {
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setDefaultMaxPerRoute(1); // Single Thread
		connectionManager.setMaxTotal(CONNECTION_POOLING_MAX_TOTAL);
		
		HttpClientBuilder httpClientBuilder = HttpClients.custom();
		httpClientBuilder.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build());
		httpClientBuilder.setConnectionManager(connectionManager);
		
		httpClient = httpClientBuilder.build();
	}

	public static String downloadAsString(String url) {
		return downloadAsString(url, DEFAULT_ENCODING, null);
	}
	
	public static String downloadAsString(String url, String encoding) {
		return downloadAsString(url, encoding, null);
	}
	
	public static String downloadAsString(String url, String encoding, String cookie) {
		HttpGet httpGet = new HttpGet(url);
		httpGet.setHeader("User-Agent", USER_AGENT);
		if (cookie != null) {
			httpGet.setHeader("Cookie", cookie);
		}

		int retryCount = 0;
		for(;;) {
			try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
				final int code = response.getStatusLine().getStatusCode();
				final String reasonPhrase = response.getStatusLine().getReasonPhrase();
				
				if (code == 429) { // 429 Too Many Requests
					if (retryCount < 10) {
						retryCount++;
						logger.warn("retry {} {} {}  {}", retryCount, code, reasonPhrase, url);
						Thread.sleep(1000 * retryCount * retryCount); // sleep 1 * retryCount * retryCount sec
						continue;
					}
				}
				retryCount = 0;
				if (code == HttpStatus.SC_NOT_FOUND) { // 404
					logger.warn("{} {}  {}", code, reasonPhrase, url);
					return null;
				}
				if (code == HttpStatus.SC_BAD_REQUEST) { // 400
					logger.warn("{} {}  {}", code, reasonPhrase, url);
					return null;
				}
				if (code == HttpStatus.SC_OK) {
					return getContent(response.getEntity(), encoding);
				}
				
				// Other code
				logger.error("statusLine = {}", response.getStatusLine().toString());
				logger.error("url {}", url);
				logger.error("code {}", code);
				HttpEntity entity = response.getEntity();
				if (entity != null) {
			    	logger.error("entity {}", getContent(entity, encoding));
				}
				throw new UnexpectedException("download");
			} catch (ClientProtocolException e) {
				logger.error("ClientProtocolException {}", e.toString());
				throw new UnexpectedException("ClientProtocolException");
			} catch (IOException e) {
				logger.error("IOException {}", e.toString());
				throw new UnexpectedException("IOException");
			} catch (InterruptedException e) {
				logger.error("InterruptedException {}", e.toString());
				throw new UnexpectedException("InterruptedException");
			}
		}
	}
	
	private static String getContent(HttpEntity entity, String encoding) {
		if (entity == null) {
			logger.error("entity is null");
			throw new UnexpectedException("entity is null");
		}
		if (encoding == null) {
			logger.error("encoding is null");
			throw new UnexpectedException("encoding is null");
		}
    	try (InputStreamReader isr = new InputStreamReader(entity.getContent(), encoding)) {
     		char[]        cbuf = new char[1024 * 64];
       		StringBuilder ret  = new StringBuilder();
       		for(;;) {
    			int len = isr.read(cbuf);
    			if (len == -1) break;
    			ret.append(cbuf, 0, len);
    		}
    	   	return ret.toString();
    	} catch (UnsupportedEncodingException e) {
			logger.error("UnsupportedEncodingException {}", e.toString());
			throw new UnexpectedException("UnsupportedEncodingException");
		} catch (UnsupportedOperationException e) {
			logger.error("UnsupportedOperationException {}", e.toString());
			throw new UnexpectedException("UnsupportedOperationException");
		} catch (IOException e) {
			logger.error("IOException {}", e.toString());
			throw new UnexpectedException("IOException");
		}
 	}
	
	public static void download(String url, String path) {
		download(url, path, DEFAULT_ENCODING);
	}
	public static void download(String url, String path, String encoding) {
		String content = downloadAsString(url, encoding);
		
		if (content != null) {
			File file = new File(path);
			
			File fileParent = file.getParentFile();
			if (!fileParent.exists()) fileParent.mkdirs();
			
			FileUtil.write(file, content);
		}
	}
	
	public static byte[] downloadAsByteArray(String url) {
		HttpGet httpGet = new HttpGet(url);
		httpGet.setHeader("User-Agent", USER_AGENT);
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
				throw new UnexpectedException("download");
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
				throw new UnexpectedException("entity is null");
		    }
		} catch (UnsupportedOperationException e) {
			logger.error("UnsupportedOperationException {}", e.toString());
			throw new UnexpectedException("UnsupportedOperationException");
		} catch (IOException e) {
			logger.error("IOException {}", e.toString());
			throw new UnexpectedException("IOException");
		}
	}
}
