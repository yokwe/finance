package yokwe.finance.securities.quandl;

import java.io.File;

import yokwe.finance.securities.util.FileUtil;

public final class Quandl {
	public static final String URL_BASE = "https://www.quandl.com/api/v3";
	public static final String PATH_BASE = "tmp/fetch/quandl";
	
	private static final String PATH_API_KEY = "tmp/fetch/quandl/api_key";
	private static final String API_KEY;
	static {
		API_KEY = FileUtil.read(new File(PATH_API_KEY));
	}
	
	public static String getURL(String path) {
		return String.format("%s/%s?api_key=%s", URL_BASE, path, API_KEY);
	}
	
	public static String getURL(String path, String query) {
		return String.format("%s/%s?api_key=%s&%s", URL_BASE, path, API_KEY, query);
	}
	
	public static String getPath(String path) {
		return String.format("%s/%s", PATH_BASE, path);
	}
}
