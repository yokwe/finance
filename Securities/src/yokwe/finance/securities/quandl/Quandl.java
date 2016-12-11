package yokwe.finance.securities.quandl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.app.Fetch;
import yokwe.finance.securities.util.CSV;
import yokwe.finance.securities.util.FileUtil;

public class Quandl {
	private static final Logger logger = LoggerFactory.getLogger(Quandl.class);

	// https://www.quandl.com/api/v3/databases.csv?page=1&api_key=BFLucSVjv5FBWkrSwFsH
	public static String URL_BASE = "https://www.quandl.com/api/v3/";
	
	public static String getURL(String name, String format, int page, String apiKey) {
		return String.format("%s/%s.%s?page=%d&api_key=%s", URL_BASE, name, format, page, apiKey);
	}

	// id,name,database_code,description,datasets_count,downloads,premium,image,favorite,url_name
	public static List<List<String>> parse(String content, String headerString) {
		String[] headers = headerString.split(",");
		
		List<List<String>> ret = CSV.parse(content);
		
		// sanity check
		{
			// first record must match header
			String[] firstRecord = ret.get(0).toArray(new String[0]);
			
			if (firstRecord.length != headers.length) {
				logger.error("Unexpected number of record in header  firstRecord {}  header {}", firstRecord.length, headers.length);
				throw new SecuritiesException("Unexpected number of record in header");
			}
			boolean errorFlag = false;
			for(int i = 0; i < firstRecord.length; i++) {
				if (firstRecord[i].equals(headers[i])) continue;
				errorFlag = true;
				logger.error("Header record didn't match  firstRecord {}  header {}", firstRecord[i], headers[i]);
			}
			if (errorFlag) {
				throw new SecuritiesException("Header record didn't match");
			}
		}
		//
		
		return ret;
	}
	
	public static void main(String[] args) {
		String apiKey = args[0];
		String pathBase = "tmp/fetch/quandl";
		String fileName = "databases";
		String fileFormat = "csv";
		{
			
			for(int i = 1; i < 6; i++) {
				String url = getURL(fileName, fileFormat, i, apiKey);
				String path = String.format("%s/%s-%d.csv", pathBase, fileName, i, fileFormat);
				List<String> messageList = new ArrayList<>();
				
				logger.info("url = {}", url);
				logger.info("path = {}", path);
				//Fetch.download(url, path, messageList);
				logger.info("messageList = {}", messageList);
				
				String content = FileUtil.getContents(new File(path));
				List<List<String>> records = CSV.parse(content);
				if (content.length() != 0) {
					logger.info("records {} - {}", records.size(), records.get(0).size());
					logger.info("{}", records);
				} else {
					logger.info("records {}", records.size());
					logger.info("{}", records);
				}
			}
		}
	}
}
