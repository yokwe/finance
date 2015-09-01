package yokwe.finance.etf;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

public class YahooProfile {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(YahooProfile.class);
	
	private static final String DIR_PATH = "tmp/fetch/etf/yahoo-profile";
	
	public static final class Element {
		public final String symbol;
		public final String name;
		public final String category;
		public final String family;
		public final String netAssets;
		public final String inceptionDate;
		public final String expenseRatio;
		
		public Element(String symbol, String name, String category, String family, String netAssets, String inceptionDate, String expenseRatio) {
			this.symbol        = symbol;
			this.name          = name;
			this.category      = category;
			this.family        = family;
			this.netAssets     = netAssets;
			this.inceptionDate = inceptionDate;
			this.expenseRatio  = expenseRatio;
		}
	}

	public final Map<String, Element> map          = new TreeMap<>();
	
	public enum Field {
		SYMBOL, NAME, CATEGORY, FAMILY, NET_ASSETS, INCEPTION_DATE, EXPENSE_RATIO,
	}
	
	public static class ScrapeYahooProfile extends Scrape<Field> {
		public void init() {
			// <div class="title"><h2>AdvisorShares WCM/BNY MlnFcsd GR ADR ETF (AADR)</h2>
			// <div class="title"><h2>Lattice Developed Mkts (ex-US) Stra ETF (RODM)</h2>
			add(Field.SYMBOL,
					"<div class=\"title\"><h2>.+? \\(([A-Z]+?)\\)</h2>",
					"Fund Overview");
			
			// <div class="title"><h2>AdvisorShares WCM/BNY MlnFcsd GR ADR ETF (AADR)</h2>
			add(Field.NAME,
					"<div class=\"title\"><h2>(.+?) \\([A-Z]+?\\)</h2>",
					"Fund Overview");
			
			// <a href="/etf/lists/?mod_id=mediaquotesetf&amp;tab=tab6&amp;cat=%24FECA%24FG%24%24">Foreign Large Growth</a>
		    add(Field.CATEGORY,
		    		"<a href=\"/etf/lists/\\?mod_id=mediaquotesetf&amp;tab=tab6&amp;cat=[^>]+>(.+?)</a>",
					"Fund Overview");
		    
		    // <a href="/etf/lists?mod_id=mediaquotesetf&amp;tab=tab6&amp;ff=0C00004HZR">AdvisorShares</a>
		    add(Field.FAMILY,
		    		"<a href=\"/etf/lists\\?mod_id=mediaquotesetf&amp;tab=tab6&amp;ff=[^>]+>(.+?)</a>",
					"Fund Overview");
			
		    // <tr><td class="yfnc_tablehead1" width="50%">Net Assets:</td><td class="yfnc_tabledata1">NaN</td></tr>
		    add(Field.NET_ASSETS,
		    		"<tr><td class=\"yfnc_tablehead1\" width=\"50%\">Net Assets:</td><td class=\"yfnc_tabledata1\">(.+?)</td></tr>",
					"Fund Overview");
		    
		    // <tr><td class="yfnc_tablehead1" width="50%">Fund Inception Date:</td><td class="yfnc_tabledata1">Aug 19, 2015</td></tr>
		    add(Field.INCEPTION_DATE,
		    		"<tr><td class=\"yfnc_tablehead1\" width=\"50%\">Fund Inception Date:</td><td class=\"yfnc_tabledata1\">(.+?)</td></tr>",
					"Fund Overview");
		    
		    // <tr><td class="yfnc_datamodlabel1">Annual Report Expense Ratio (net)</td><td class="yfnc_datamoddata1" align="right">0.09%</td>
		    add(Field.EXPENSE_RATIO,
		    		"<tr><td class=\"yfnc_datamodlabel1\">Annual Report Expense Ratio \\(net\\)</td><td class=\"yfnc_datamoddata1\" align=\"right\">(.+?)</td>",
					"Fund Operations");
		}
	}

	private ScrapeYahooProfile scrape = new ScrapeYahooProfile();


	private void extractInfo(File file) {
//		logger.debug("{}", file.getName());
		
		scrape.reset(file);
		
		String name      = scrape.getValue(Field.NAME);
		String symbol    = scrape.getValue(Field.SYMBOL);
		String category  = scrape.getValue(Field.CATEGORY);
		String family    = scrape.getValue(Field.FAMILY);
		String netAssets = scrape.getValue(Field.NET_ASSETS);
		String inception = scrape.getValue(Field.INCEPTION_DATE);
		String expense   = scrape.getValue(Field.EXPENSE_RATIO);
		
		if (name.equals(Scrape.NO_VALUE)) return;
		
		map.put(symbol, new Element(symbol, name, category, family, netAssets, inception, expense));
		logger.debug("{}", String.format("%-8s %s", symbol, netAssets));
	}
	
	
	public YahooProfile(String path) {
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
		YahooProfile nameList = new YahooProfile(DIR_PATH);
		logger.info("nameList = {}", nameList.map.size());
		logger.info("STOP");
	}
}
