package yokwe.finance.etf.update;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import yokwe.finance.etf.Scrape;
import yokwe.finance.etf.util.CSVUtil;

public class YahooProfile {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(YahooProfile.class);
	
	private static final String DIR_PATH = "tmp/fetch/yahoo-profile";
	private static final String CSV_PATH = "tmp/sqlite/yahoo-profile.csv";
	
	public enum Field {
		SYMBOL, NAME, CATEGORY, FAMILY, NET_ASSETS, INCEPTION_DATE, EXPENSE_RATIO,
	}
	
	public static class ScrapeYahooProfile extends Scrape<Field> {
		public void init() {
			// <div class="title"><h2>AdvisorShares WCM/BNY MlnFcsd GR ADR ETF (AADR)</h2>
			// <div class="title"><h2>Lattice Developed Mkts (ex-US) Stra ETF (RODM)</h2>
			add(Field.SYMBOL,
				"<div class=\"title\"><h2>.+? \\(([A-Z]+?)\\)</h2>",
				"<div class=\"title\">");
			
			// <div class="title"><h2>AdvisorShares WCM/BNY MlnFcsd GR ADR ETF (AADR)</h2>
			add(Field.NAME,
				"<div class=\"title\"><h2>(.+?) \\([A-Z]+?\\)</h2>",
				"<div class=\"title\">");
			
			// <tr><td class="yfnc_tablehead1" width="$width">Category:
            //   </td><td class="yfnc_tabledata1"><a href="/etf/lists/?mod_id=mediaquotesetf&amp;tab=tab6&amp;cat=%24FECA%24FG%24%24">Foreign Large Growth</a></td></tr>
			//
			// <a href="/etf/lists/?mod_id=mediaquotesetf&amp;tab=tab6&amp;cat=%24FECA%24FG%24%24">Foreign Large Growth</a>
		    add(Field.CATEGORY,
	    		"<a href=\"/etf/lists/\\?mod_id=mediaquotesetf&amp;tab=tab6&amp;cat=[^>]+>(.+?)</a>",
				"<td class=\"yfnc_tabledata1\"><a href=\"/etf/lists/?mod_id=mediaquotesetf&amp;tab=tab6&amp;cat=");
		    
		    // <tr><td class="yfnc_tablehead1" width="$width">Fund Family:
            //   </td><td class="yfnc_tabledata1"><a href="/etf/lists?mod_id=mediaquotesetf&amp;tab=tab6&amp;ff=0C00004AZV">iShares</a></td></tr>
		    //
		    // <a href="/etf/lists?mod_id=mediaquotesetf&amp;tab=tab6&amp;ff=0C00004HZR">AdvisorShares</a>
		    add(Field.FAMILY,
	    		"<a href=\"/etf/lists\\?mod_id=mediaquotesetf&amp;tab=tab6&amp;ff=[^>]+>(.+?)</a>",
				"<td class=\"yfnc_tabledata1\"><a href=\"/etf/lists?mod_id=mediaquotesetf&amp;tab=tab6&amp;ff=");
			
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

	private static ScrapeYahooProfile scrape = new ScrapeYahooProfile();
	
	public static void save(String path) {
		List<Map<Field, String>> values = scrape.readDirectory(DIR_PATH);
		//
		CSVUtil.save(new File(path), values);
	}
	public static void save() {
		save (CSV_PATH);
	}

	public static void main(String[] args) throws IOException {
		logger.info("START");
		//
		save();
		//
		logger.info("STOP");
	}
}
