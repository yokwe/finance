package yokwe.finance.etf;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

public class MSN {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(MSN.class);
	
	private static final String DIR_PATH = "tmp/fetch/etf/msn";
	
	// <div class="header-companyname"><h1>AdvisorShares WCM/BNY MlnFcsd GR ADR ETF</h1></div>
	private static Extract extractName   = new Extract.Simple("NAME",   "<div class=\"header-companyname\"><h1>(.+)</h1></div>");

	// <div class="subheader-symbol">(NYSE ARCA: AADR)</div>
	private static Extract extractExchange = new Extract.Simple("EXCHANGE", 2, "<div class=\"subheader-symbol\">\\((.+): (.+)\\)</div>");
	
	// <span class="name"><p class='truncated-string' tabindex = '0' title='Net Assets'>Net Assets</p></span>
	// <span class="value"><p class='truncated-string' tabindex = '0' title='14.57M'>14.57M</p></span>
	private static Extract extractNetAssets= new Extract.Simple("EXCHANGE", "<span class=\"name\"><p class='truncated-string' tabindex = '0' title='Net Assets'>Net Assets</p></span>\\p{javaWhitespace}+<span class=\"value\"><p class='truncated-string' tabindex = '0' title='.+'>(.+)</p></span>");
	
	// <span class="name"><p class='truncated-string' tabindex = '0' title='Vol (3-Month Avg.)'>Vol (3-Month Avg.)</p></span>
	// <span class="value"><p class='truncated-string' tabindex = '0' title='1.48k'>1.48k</p></span>
	private static Extract extractVol3mAvg= new Extract.Simple("VOL3MAVG",
			"<span class=\"name\"><p class='truncated-string' tabindex = '0' title='Vol \\(3-Month Avg\\.\\)'>Vol \\(3-Month Avg\\.\\)</p></span>\\p{javaWhitespace}+" +
			"<span class=\"value\"><p class='truncated-string' tabindex = '0' title='.+'>(.+)</p></span>");

	// <span class="name"><p class='truncated-string' tabindex = '0' title='Category'>Category</p></span>
	// <span class="value baseminus11"><p class='truncated-string' tabindex = '0' title='Foreign Large Growth  '>Foreign Large Growth  </p></span>
	private static Extract extractCategory= new Extract.Simple("CATEGORY",
			"<span class=\"name\"><p class='truncated-string' tabindex = '0' title='Category'>Category</p></span>\\p{javaWhitespace}+" +
	        "<span class=\"value.*\"><p class='truncated-string' tabindex = '0' title='.+'>(.+?)\\p{javaWhitespace}*</p></span>");

	public static final class Element {
		public final String exchange;
		public final String symbol;
		public final String name;
		public final String netAssets;
		public final String vol3mAvg;
		public final String category;
		
		public Element(String exchange, String symbol, String name, String netAssets, String vol3mAvg, String category) {
			this.exchange  = exchange;
			this.symbol    = symbol;
			this.name      = name;
			this.netAssets = netAssets;
			this.vol3mAvg  = vol3mAvg;
			this.category  = category;
		}
	}

	public final Map<String, Element> map          = new TreeMap<>();

	private void extractInfo(File file) {
		String fileName = file.getName();
//		logger.debug("{}", fileName);
		
		String contents = Util.getContents(file);
		
		if (contents.contains("<title>: Quote and summary for  - MSN Money</title>")) return;
		
		if (!contents.contains("<p class='truncated-string' tabindex = '0' title='Net Assets'>Net Assets</p>")) return;
		if (!contents.contains("<p class='truncated-string' tabindex = '0' title='Vol (3-Month Avg.)'>Vol (3-Month Avg.)</p>")) return;
		if (!contents.contains("<p class='truncated-string' tabindex = '0' title='Category'>Category</p>")) return;
		
		String name      = extractName.getValue(fileName, contents);
		String exchange  = extractExchange.getValue(fileName, contents);
		String symbol    = extractExchange.getValue(2);
		String netAssets = extractNetAssets.getValue(fileName, contents);
		String vol3mAvg  = extractVol3mAvg.getValue(fileName, contents);
		String category  = extractCategory.getValue(fileName, contents);
		
		// replace encoded string
		name = name.replace("&amp;",  "&");
		name = name.replace("&#174;", ""); // remove ®
		name = name.replace("&#39;",  "'");
		
		// use unified exchange name
		exchange = exchange.replace("NYSE ARCA",      "NYSEARCA");
		exchange = exchange.replace("finance_EX_BAT", "BATS");
		
		map.put(symbol, new Element(exchange, symbol, name, netAssets, vol3mAvg, category));
		
		logger.debug("{}", String.format("%-8s %s", symbol, category));
	}
	
	public MSN(String path) {
		File root = new File(path);
		if (!root.isDirectory()) {
			logger.error("Not directory  path = {}", path);
			throw new RuntimeException("not directory");
		}
		
		File[] fileList = root.listFiles();
		logger.info("fileList = {}", fileList.length);
		for(File file: fileList) {
			extractInfo(file);
		}
	}

	public static void main(String[] args) {
		logger.info("START");
		MSN nameList = new MSN(DIR_PATH);
		logger.info("nameList = {}", nameList.map.size());
		logger.info("STOP");
	}
}
