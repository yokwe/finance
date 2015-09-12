package yokwe.finance.etf;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.LoggerFactory;

public class Downloader {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(Downloader.class);

	protected static final CloseableHttpClient httpclient = HttpClients.createDefault();
	
	public static void download(String url, String fileName) {
		HttpGet httpget = new HttpGet(url);
		try (CloseableHttpResponse response = httpclient.execute(httpget)) {
			final int code = response.getStatusLine().getStatusCode();
			
			if (code == 404) {
				logger.debug("statusLine = {}", response.getStatusLine().toString());
				logger.error("url {}", url);
				return;
			}
			if (code != 200) {
				logger.debug("statusLine = {}", response.getStatusLine().toString());
				logger.error("url {}", url);
				logger.error("code {}", code);
				throw new RuntimeException();
			}
			
		    HttpEntity entity = response.getEntity();
		    if (entity != null) {
		    	int fileSize = 0;
	    		byte[] buffer = new byte[65536 * 2];
		    	try (BufferedInputStream bis = new BufferedInputStream(entity.getContent(), buffer.length);
		    			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileName), buffer.length)) {
		    		for(;;) {
			    		int len = bis.read(buffer);
			    		if (len == -1) break;
			    		bos.write(buffer, 0, len);
			    		fileSize += len;
		    		}
		    	}
				if (fileSize == 0) {
					logger.debug("statusLine = {}", response.getStatusLine().toString());
					logger.error("url {}", url);
					logger.error("code {}", code);
					throw new RuntimeException();
				}
		    }
		} catch (UnsupportedOperationException e) {
			logger.info("UnsupportedOperationException {}", e.toString());
		} catch (IOException e) {
			logger.info("IOException {}", e.toString());
		}
	}
	
	private static final Matcher matcherParam = Pattern.compile("^([^ ]+) +([^ ]+)$").matcher("");
	private static final Matcher matcherComment = Pattern.compile("^#.+$").matcher("");
	
	public static void download(String path) {
		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
			int count = 0;
			long lastDownload = 0;
			for(;;) {
				String line = br.readLine();
				if (line == null) break;
//				logger.info("line {}", line);

				if (matcherComment.reset(line).matches()) continue;
				
				if (!matcherParam.reset(line).matches()) {
					logger.error("unknown line = {}", line);
					throw new ETFException("");
				}
				
				count++;
				long now = System.currentTimeMillis();
				long waitTime = 1000 - (now - lastDownload);
				if (0 < waitTime) Thread.sleep(waitTime);
				
				String file = matcherParam.group(1);
				String url = matcherParam.group(2);
				
				lastDownload = System.currentTimeMillis();
				logger.info("{}", String.format("%5d %s", count, file));
				download(url, file);
			}
		} catch (UnsupportedOperationException e) {
			logger.info("UnsupportedOperationException {}", e.toString());
		} catch (IOException e) {
			logger.info("IOException {}", e.toString());
		} catch (InterruptedException e) {
			logger.info("InterruptedException {}", e.toString());
		}
	}
	
	public static void main(String[] args) {
		logger.info("START");
		for(String path: args) download(path);
		logger.info("STOP");
	}
}
