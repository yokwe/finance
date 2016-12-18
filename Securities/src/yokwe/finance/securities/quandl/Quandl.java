package yokwe.finance.securities.quandl;

import java.io.File;

import yokwe.finance.securities.util.FileUtil;

public final class Quandl {
	public static final String URL_BASE     = "https://www.quandl.com/api/v3";
	
	public static final String PATH_BASE    = "tmp/fetch/quandl";
	public static final String PATH_API_KEY = PATH_BASE + "/api_key";
	
	public static final String API_KEY      = FileUtil.read(new File(PATH_API_KEY));
	
	public static final int    PER_PAGE    = 100;
	public static final int    BUFFER_SIZE = 64 * 1024;
	

	public static String getURL(String path) {
		return String.format("%s/%s?api_key=%s", URL_BASE, path, API_KEY);
	}
	
	public static String getURL(String path, String query) {
		return String.format("%s/%s?api_key=%s&%s", URL_BASE, path, API_KEY, query);
	}
	
	public static String getPath(String path) {
		return String.format("%s/%s", PATH_BASE, path);
	}
	
	
	public static class Meta {
		public String query;
		public int    per_page;
		public int    current_page;
		public int    prev_page;
		public int    total_pages;
		public int    total_count;
		public int    next_page;
		public int    current_first_item;
		public int    current_last_item;
		
		@Override
		public String toString() {
			return String.format("{%d-%d-%d  %d-%d}", total_count, current_first_item, current_last_item, total_pages, current_page);
		}
	}

}
