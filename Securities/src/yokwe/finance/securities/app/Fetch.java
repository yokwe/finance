package yokwe.finance.securities.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.HttpUtil;


public class Fetch {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(Fetch.class);

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
	public static void download(final long waitPeriod, final String path) {
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
					throw new SecuritiesException("unknown line");
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
			long lastDownload = System.currentTimeMillis();
			for(Entry entry: entryList) {				
				count++;
				{
					File file = new File(entry.file);
					if (file.exists()) continue;
				}
				
				long now = System.currentTimeMillis();
				long waitTime = waitPeriod - (now - lastDownload);
				if (0 < waitTime) Thread.sleep(waitTime);
				
				String url  = entry.url;
				String file = entry.file;
				
				lastDownload = System.currentTimeMillis();
				logger.info("{}", String.format("%5d / %5d  %s", count, totalCount, file));
				
				for(int retryCount = 5;;retryCount--) {
					if (retryCount == 0) {
						logger.error("retryCount exceed");
						throw new SecuritiesException("retryCount exceed");
					}
					try {
						HttpUtil.download(url, file);
						break;
					} catch (SecuritiesException e) {
						String message = e.getMessage();
						if (message.equals("IOException")) {
							logger.warn("IOException  retryCount {}", retryCount);
							Thread.sleep(3_000);
							continue;
						} else {
							throw e;
						}
					}
				}
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
		final long waitPeriod = Long.valueOf(System.getProperty("waitPeriod", "1000"));
		logger.info("waitPeriod = {}", waitPeriod);
		
		for(String path: args) download(waitPeriod, path);
		logger.info("STOP");
	}
}
