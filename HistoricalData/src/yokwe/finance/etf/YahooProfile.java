package yokwe.finance.etf;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

public class YahooProfile {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(YahooProfile.class);
	
	private static final String DIR_PATH = "tmp/fetch/etf/yahoo-profile";
	
	// <div class="title"><h2>AdvisorShares WCM/BNY MlnFcsd GR ADR ETF (AADR)</h2>
	private static Extract extractTitle = new Extract.Simple("TITLE", 2, "<div class=\"title\"><h2>(.+) \\((.+)\\)</h2>");
	
	// <a href="/etf/lists/?mod_id=mediaquotesetf&amp;tab=tab6&amp;cat=%24FECA%24FG%24%24">Foreign Large Growth</a>
    private static Extract extractCategory = new Extract.Simple("CATEGORY", 1,
    		"<a href=\"/etf/lists/\\?mod_id=mediaquotesetf&amp;tab=tab6&amp;cat=[^>]+>(.+?)</a>");
    
    // <a href="/etf/lists?mod_id=mediaquotesetf&amp;tab=tab6&amp;ff=0C00004HZR">AdvisorShares</a>
    private static Extract extractFamily = new Extract.Simple("FAMILY", 1,
    		"<a href=\"/etf/lists\\?mod_id=mediaquotesetf&amp;tab=tab6&amp;ff=[^>]+>(.+?)</a>");
	
    // <tr><td class="yfnc_tablehead1" width="50%">Net Assets:</td><td class="yfnc_tabledata1">NaN</td></tr>
    private static Extract extractNetAssets = new Extract.Simple("NET_ASSETS", 1,
    		"<tr><td class=\"yfnc_tablehead1\" width=\"50%\">Net Assets:</td><td class=\"yfnc_tabledata1\">(.+?)</td></tr>");
    
    // <tr><td class="yfnc_tablehead1" width="50%">Fund Inception Date:</td><td class="yfnc_tabledata1">Aug 19, 2015</td></tr>
    private static Extract extractInception = new Extract.Simple("INCEPTION", 1,
    		"<tr><td class=\"yfnc_tablehead1\" width=\"50%\">Fund Inception Date:</td><td class=\"yfnc_tabledata1\">(.+?)</td></tr>");
    
    // <tr><td class="yfnc_tablehead1" width="50%">Legal Type:</td><td class="yfnc_tabledata1">Exchange Traded Fund</td></tr>
    //private static Extract extractType = new Extract.Simple("TYPE", 1,
    //		"<tr><td class=\"yfnc_tablehead1\" width=\"50%\">Legal Type:</td><td class=\"yfnc_tabledata1\">(.+?)</td></tr>");
   
    // <tr><td class="yfnc_datamodlabel1">Annual Report Expense Ratio (net)</td><td class="yfnc_datamoddata1" align="right">0.09%</td>
    private static Extract extractExpense = new Extract.Simple("EXPENSE", 1,
    		"<tr><td class=\"yfnc_datamodlabel1\">Annual Report Expense Ratio \\(net\\)</td><td class=\"yfnc_datamoddata1\" align=\"right\">(.+?)</td>");
    
    // <tr><td class="yfnc_datamodlabel1">Total Net Assets</td><td class="yfnc_datamoddata1" align="right">NaN</td>
    //private static Extract extractTotalNetAssets = new Extract.Simple("NET_ASSETS", 1,
    //		"<tr><td class=\"yfnc_datamodlabel1\">Total Net Assets</td><td class=\"yfnc_datamoddata1\" align=\"right\">(.+?)</td>");
    
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
	
	private final DateTimeFormatter parseInceptionDate  = DateTimeFormatter.ofPattern("MMM d, yyyy");
	private final DateTimeFormatter formatInceptionDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private void extractInfo(File file) {
		String fileName = file.getName();
//		logger.debug("{}", fileName);
		
		String contents = Util.getContents(file);
		
		if (!contents.contains("Fund Overview")) return;
		
		String name      = extractTitle.getValue(fileName, contents);
		String symbol    = extractTitle.getValue(2);
		String category  = extractCategory.getValue(fileName, contents);
		String family    = extractFamily.getValue(fileName, contents);
		String netAssets = extractNetAssets.getValue(fileName, contents);
		String inception = extractInception.getValue(fileName, contents);
		String expense   = extractExpense.getValue(fileName, contents);
		
		// replace encoded string
		family = family.replace("&amp;",  "&");
		
		String inceptionDate = formatInceptionDate.format(parseInceptionDate.parse(inception));
		
		map.put(symbol, new Element(symbol, name, category, family, netAssets, inceptionDate, expense));
		logger.debug("{}", String.format("%-8s %s", symbol, name));
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
