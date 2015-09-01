package yokwe.finance.etf;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

public class ETF {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(ETF.class);
	
	private static final String DIR_PATH = "tmp/fetch/etf/etf";	
	
	public static final class Element {
		public final String symbol;
		public final String name;
		public final String inceptionDate;
		public final String expenseRatio;
		public final String issuer;
		public final String homePage;
		public final String assetsUnderManagement;
		public final String indexTracked;
		
		public Element(String symbol, String name, String inceptionDate, String expenseRatio, String iuuser, String homePage, String assetsUnderManagement, String indexTracked) {
			this.symbol                = symbol;
			this.name                  = name;
			this.inceptionDate         = inceptionDate;
			this.expenseRatio          = expenseRatio;
			this.issuer                = iuuser;
			this.homePage              = homePage;
			this.assetsUnderManagement = assetsUnderManagement;
			this.indexTracked          = indexTracked;
		}
	}
	
	public final Map<String, Element> map = new TreeMap<>();
	
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
	
	private ScrapeETF scrape = new ScrapeETF();
	
	private void scrapeInfo(File file) {
//		logger.debug("{}", file.getName());
		
		scrape.reset(file);
		
		String symbol           = scrape.getValue(Field.SYMBOL);
		String name             = scrape.getValue(Field.NAME);
		String inceptionDate    = scrape.getValue(Field.INCEPTION_DATE);
		String expenseRatio     = scrape.getValue(Field.EXPENSE_RATIO);
		String issuer           = scrape.getValue(Field.ISSUER);
		String homePage         = scrape.getValue(Field.HOME_PAGE);
		String aum              = scrape.getValue(Field.AUM);
		String indexTracked     = scrape.getValue(Field.INDEX_TRACKED);
		
		map.put(symbol, new Element(symbol, name, inceptionDate, expenseRatio, issuer, homePage, aum, indexTracked));
		
		logger.debug("{}", String.format("%-8s %s", symbol, expenseRatio));
	}
	
	public ETF(String path) {
		File root = new File(path);
		if (!root.isDirectory()) {
			logger.error("Not directory  path = {}", path);
			throw new RuntimeException("not directory");
		}
		
		File[] fileList = root.listFiles();
		Arrays.sort(fileList, (a, b) -> a.getName().compareTo(b.getName()));
		logger.info("fileList = {}", fileList.length);
		for(File file: fileList) {
			scrapeInfo(file);
		}
	}

	public static void main(String[] args) {
		logger.info("START");
		ETF nameList = new ETF(DIR_PATH);
		logger.info("map = {}", nameList.map.size());
		logger.info("STOP");
	}
}
