package yokwe.finance.etf;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

public class ETF {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(ETF.class);
	
	private static final String DIR_PATH = "tmp/fetch/etf/etf";	
	private static final String CSV_PATH = "tmp/sqlite/etf-etf.csv";
	
	public enum Field {
		SYMBOL, NAME, INCEPTION_DATE, EXPENSE_RATIO, ISSUER, HOME_PAGE, AUM, INDEX_TRACKED,
	}
	
	public static class ScrapeETF extends Scrape<Field> {
		public void init() {
			// GAInfoFundName = "PowerShares QQQ";
			add(Field.NAME,
				"\\p{javaWhitespace}+GAInfoFundName = \"(.+)\";");
			
			// GAInfoTicker = "QQQ";
			add(Field.SYMBOL,
				"\\p{javaWhitespace}+GAInfoTicker = \"(.+)\";");
			
			// <span id="IssuerSpan">				ProShares			</span>
			add(Field.ISSUER,
				"<span id=\"IssuerSpan\">\\p{javaWhitespace}+(.+)\\p{javaWhitespace}+</span>");
			
			// <span id="InceptionDateSpan">				03/10/99			</span>
			add(Field.INCEPTION_DATE,
				"<span id=\"InceptionDateSpan\">\\p{javaWhitespace}+([0-9]{2}/[0-9]{2}/[0-9]{2})\\p{javaWhitespace}+</span>");
			
			// <span id="ExpenseRatioSpan">				0.20%			</span>
			// <span id="ExpenseRatioSpan">				--			</span>
			add(Field.EXPENSE_RATIO,
				"<span id=\"ExpenseRatioSpan\">\\p{javaWhitespace}+(.+)\\p{javaWhitespace}+</span>");
			
			// <a href="http://www.proshares.com/funds/ubio.html" title="Fund Home Page" target="_blank" id="fundHomePageLink">Fund Home Page</a>
			add(Field.HOME_PAGE,
				"<a href=\"(.+)\" title=\"Fund Home Page\" target=\"_blank\" id=\"fundHomePageLink\">Fund Home Page</a>");
			
			// <span id="AssetsUnderManagementSpan">				$14.23 M			</span>
			add(Field.AUM,
				"<span id=\"AssetsUnderManagementSpan\">\\p{javaWhitespace}+(.+)\\p{javaWhitespace}+</span>");
			
			// <span id="IndexTrackedSpan">				NASDAQ-100 Index			</span>
			add(Field.INDEX_TRACKED,
				"(?s)<span id=\"IndexTrackedSpan\">(.+?)</span>",
				"IndexTrackedSpan");
		}
	}
	
	private static ScrapeETF scrape = new ScrapeETF();
	
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
