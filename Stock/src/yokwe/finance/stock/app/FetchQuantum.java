package yokwe.finance.stock.app;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.stock.UnexpectedException;
import yokwe.finance.stock.util.FileUtil;
import yokwe.finance.stock.util.HttpUtil;

public class FetchQuantum {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FetchQuantum.class);

	public static void download(String url, File file) {
		if (file.exists()) return;
		
		String contents = HttpUtil.downloadAsString(url);
		if (contents == null) {
			// Create empty file to prevent process again in next run.
			try {
				file.createNewFile();
			} catch (IOException e) {
				logger.error("IOException {}", e);
				throw new UnexpectedException("IOException");
			}
		} else {
			FileUtil.write(file, contents);
		}
	}
	
	public static void download(String url, String path) {
		File file = new File(path);
		download(url, file);
	}
	
	// url
	//   http://quantumonline.com/search.cfm?sopt=symbol&tickersymbol=SSW
	// path
	//   tmp/quantum/SSW.html
	private static final String URL_PATTERN = "http://quantumonline.com/search.cfm?sopt=symbol&tickersymbol=%s";
	
	public static void main(String[] args) {
		logger.info("START");
		
		try {
			List<String> symbolList = UpdateStock.getSymbolList();
			int count = 0;
			for(String symbol: symbolList) {
				String encodedSymbol = URLEncoder.encode(symbol, "UTF-8");
				count++;
								
				String url  = String.format(URL_PATTERN, encodedSymbol);
				String path = String.format("%s/%s", UpdatePreferred.PATH_DIR, symbol);
				
				File file = new File(path);
				if (file.exists()) {
//					logger.info("{}", String.format("%4d / %4d  %s", count, symbolList.size(), symbol));
				} else {
					logger.info("{}", String.format("%4d / %4d  %s", count, symbolList.size(), symbol));
					download(url, file);
				}
			}
		} catch (UnsupportedEncodingException e) {
			logger.error("UnsupportedEncodingException {}", e);
			throw new UnexpectedException("UnsupportedEncodingException");
		}

		logger.info("STOP");
	}
}
