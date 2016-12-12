package yokwe.finance.securities.quandl;

import java.io.File;

import yokwe.finance.securities.util.FileUtil;

public final class Quandl {
	public static final String URL_BASE = "https://www.quandl.com/api/v3";
	
	private static final String PATH_API_KEY = "tmp/fetch/quandl/api_key";
	private static final String API_KEY;
	static {
		API_KEY = FileUtil.getContents(new File(PATH_API_KEY));
	}
		
	public static String getURL(String name, String format, int page) {
		return String.format("%s/%s.%s?page=%d&api_key=%s", URL_BASE, name, format, page, API_KEY);
	}
}
