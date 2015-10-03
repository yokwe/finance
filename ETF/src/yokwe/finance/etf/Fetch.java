package yokwe.finance.etf;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.LoggerFactory;


public class Fetch {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(Fetch.class);

	protected static final CloseableHttpClient httpclient = HttpClients.createDefault();
	
	public static void download(String url, String fileName, List<String> messageList) {
		HttpGet httpget = new HttpGet(url);
		httpget.setHeader("User-Agent", "Mozilla");
		try (CloseableHttpResponse response = httpclient.execute(httpget)) {
			final int code = response.getStatusLine().getStatusCode();
			final String reasonPhrase = response.getStatusLine().getReasonPhrase();
			
			if (code == 404) {
				String message = String.format("%d %s - %s", code, reasonPhrase, fileName);
//				logger.error(message);
				messageList.add(message);
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
	
	static class Entry {
		final String url;
		final String file;
		Entry(String url, String file) {
			this.url = url;
			this.file = file;
		}
	}
	public static void download(String path) {
		// Build entryList
		List<Entry> entryList = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
			for(;;) {
				String line = br.readLine();
				if (line == null) break;
//				logger.info("line {}", line);

				if (matcherComment.reset(line).matches()) continue;
				
				if (!matcherParam.reset(line).matches()) {
					logger.error("unknown line = {}", line);
					throw new ETFException("");
				}
				
				String file = matcherParam.group(1);
				String url = matcherParam.group(2);
				
				entryList.add(new Entry(url, file));
			}
		} catch (IOException e) {
			logger.info("IOException {}", e.toString());
		}
		
		// Process entryList
		List<String> messageList = new ArrayList<>();
		try {
			int totalCount = entryList.size();
			int count = 0;
			long lastDownload = 0;
			for(Entry entry: entryList) {				
				count++;
				{
					File file = new File(entry.file);
					if (file.exists()) continue;
				}
				
				long now = System.currentTimeMillis();
				long waitTime = 1000 - (now - lastDownload);
				if (0 < waitTime) Thread.sleep(waitTime);
				
				String url = entry.url;
				String file = entry.file;
				
				lastDownload = System.currentTimeMillis();
				logger.info("{}", String.format("%5d / %5d  %s", count, totalCount, file));
				download(url, file, messageList);
			}
		} catch (InterruptedException e) {
			logger.info("InterruptedException {}", e.toString());
		}
		
		// Print messageList
		{
			int totalCount = messageList.size();
			int count = 0;
			for(String message: messageList) {
				count++;
				logger.error("{}", String.format("%5d / %5d  %s", count, totalCount, message));
			}
		}
	}
	
	public static void main(String[] args) {
		logger.info("START");
		for(String path: args) download(path);
		logger.info("STOP");
	}
}
