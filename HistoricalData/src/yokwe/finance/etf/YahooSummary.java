package yokwe.finance.etf;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

public class YahooSummary {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(YahooSummary.class);
	
	private static final String DIR_PATH = "tmp/fetch/etf/yahoo-summary";
	
	// <div class="title"><h2>AdvisorShares WCM/BNY MlnFcsd GR ADR ETF (AADR)</h2>
	private static Extract extractTitle = new Extract.Simple("TITLE", 2, "<div class=\"title\"><h2>(.+) \\((.+)\\)</h2>");
	
	// <span class="rtq_exch"><span class="rtq_dash">-</span>NYSEArca  </span>
	//private static Extract extractExchange = new Extract.Simple("EXCHANGE", 1, "<span class=\"rtq_exch\"><span class=\"rtq_dash\">-</span>(.+?) +</span>");
	
	// <tr><th scope="row" width="$width">Category:</th><td class="yfnc_tabledata1"><a href="/etf/lists/?mod_id=mediaquotesetf&amp;cat=%24FECA%24FG%24%24">Foreign Large Growth</a></td></tr>
	//private static Extract extractCategory = new Extract.Simple("CATEGORY", 1, "<tr><th scope=\"row\" width=\"\\$width\">Category:</th><td class=\"yfnc_tabledata1\"><a [^>]+?>([^<]+)</a></td></tr>");
	
	// <tr><th scope="row" width="48%">Net Assets²:</th><td class="yfnc_tabledata1">176.92B</td></tr>
	private static Extract extractNetAssets = new Extract.Simple("NET_ASSETS", 1,
			"<tr><th scope=\"row\" width=\"48%\">Net Assets[^:]*:</th><td class=\"yfnc_tabledata1\">(.+?)</td></tr>");
	
	// <tr><th scope="row" width="48%">Avg Vol <span class="small">(3m)</span>:</th><td class="yfnc_tabledata1">33,625,100</td></tr>
	// <tr><th scope="row" width="48%">Avg Vol <span class="small">(3m)</span>:</th><td class="yfnc_tabledata1">239,516</td></tr>
	private static Extract extractAvgVol3m = new Extract.Simple("AVG_VOL_3M", 1,
			"<tr><th scope=\"row\" width=\"48%\">Avg Vol <span class=\"small\">\\(3m\\)</span>:</th><td class=\"yfnc_tabledata1\">(.+?)</td></tr>");
	
	public static final class Element {
		public final String symbol;
		public final String name;
		
		public Element(String symbol, String name) {
			this.symbol    = symbol;
			this.name      = name;
		}
	}

	public final Map<String, Element> map          = new TreeMap<>();

	private void extractInfo(File file) {
		String fileName = file.getName();
//		logger.debug("{}", fileName);
		
		String contents = Util.getContents(file);
		
		String name      = extractTitle.getValue(fileName, contents);
		String symbol    = extractTitle.getValue(2);
//		String netAssets = extractNetAssets.getValue(fileName, contents);
		String avgVol3m  = extractAvgVol3m.getValue(fileName, contents);
		
//		map.put(symbol, new Element(symbol, name));
		
		logger.debug("{}", String.format("%-8s %s", symbol, avgVol3m));
	}
	
	public YahooSummary(String path) {
		File root = new File(path);
		if (!root.isDirectory()) {
			logger.error("Not directory  path = {}", path);
			throw new RuntimeException("not directory");
		}
		
		File[] fileList = root.listFiles();
		Arrays.sort(fileList, (a, b) -> a.getName().compareTo(b.getName()));
		logger.info("fileList = {}", fileList.length);
		for(File file: fileList) {
			extractInfo(file);
		}
	}

	public static void main(String[] args) {
		logger.info("START");
		YahooSummary nameList = new YahooSummary(DIR_PATH);
		logger.info("nameList = {}", nameList.map.size());
		logger.info("STOP");
	}
}
