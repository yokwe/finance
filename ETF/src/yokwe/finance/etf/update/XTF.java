package yokwe.finance.etf.update;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import yokwe.finance.etf.Scrape;
import yokwe.finance.etf.util.CSVUtil;

public class XTF {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(XTF.class);
	
	private static final String DIR_PATH = "tmp/fetch/xtf";	
	private static final String CSV_PATH = "tmp/sqlite/xtf.csv";
	
	public enum Field {
		SYMBOL, NAME, PRICE, RATING,
	}
	
	public static class ScrapeXTF extends Scrape<Field> {
		public void init() {
			// <td class="etfSymbol fn" align="left" nowrap="nowrap" style="white-space: nowrap">QQQ</td>
			add(Field.SYMBOL,
				"<td class=\"etfSymbol fn\".*?>(.+?)</td>");
			
			// <td class="etfName" align="left" nowrap="nowrap" style="white-space: nowrap">PowerShares QQQ</td>
			add(Field.NAME,
				"<td class=\"etfName\".*?>(.+?)</td>");
			
			// <span class="QLableP">Price:</span><span class="qDataP">$19.06</span>
			add(Field.PRICE,
				"<span class=\"qDataP\">(.+?)</span>");
			
			// <span class="rating"><img class='value' alt='nr' 
			add(Field.RATING,
				"<span class=\"rating\"><img class='value' alt='(.+?)' ");
		}
	}
	
	private static ScrapeXTF scrape = new ScrapeXTF();
	
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
