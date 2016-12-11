package yokwe.finance.securities.quandl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.CSV;

public class Quandl {
	private static final Logger logger = LoggerFactory.getLogger(Quandl.class);

	// https://www.quandl.com/api/v3/databases.csv?page=1&apiKey=BFLucSVjv5FBWkrSwFsH
	public static String URL_BASE = "https://www.quandl.com/api/v3/";
	
	public static String getDownloadURL(String target, int page, String apiKey) {
		return String.format("%s/%s?page=%d&apiKey=%s", URL_BASE, target, page, apiKey);
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
}
