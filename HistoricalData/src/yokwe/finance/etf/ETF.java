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
	private final Extract.Set extract = new Extract.Set();
	
	private void extractInfo(File file) {
//		logger.debug("{}", file.getName());
		
		extract.reset(file);;
		
		String symbol           = extract.getValue("SYMBOL");
		String name             = extract.getValue("NAME");
		String inception_MM     = extract.getValue("INCEPTION_DATE", 1);
		String inception_DD     = extract.getValue("INCEPTION_DATE", 2);
		String inception_YY     = extract.getValue("INCEPTION_DATE", 3);
		String expenseRatio     = extract.getValue("EXPENSE_RATIO");
		String issuer           = extract.getValue("ISSUER");
		String homePage         = extract.getValue("HOME_PAGE");
		String aum              = extract.getValue("AUM");
		String indexTracked     = extract.getValue("INDEX_TRACKED");
		
		String inceptionDate = String.format("20%s-%s-%s", inception_YY, inception_MM, inception_DD);
		
		indexTracked = indexTracked.replace("&amp;", "&");
		indexTracked = indexTracked.replace("&gt;",  ">");
		indexTracked = indexTracked.replace("<br />", "");
		indexTracked = indexTracked.replace("<br>",   "");
		indexTracked = indexTracked.replace("<p>",    "");
		indexTracked = indexTracked.replace("</p>",   "");
		indexTracked = indexTracked.replaceAll("\\p{javaWhitespace}+", " ");
		indexTracked = indexTracked.trim();
		
		map.put(symbol, new Element(symbol, name, inceptionDate, expenseRatio, issuer, homePage, aum, indexTracked));
		
		logger.debug("{}", String.format("%-8s %s", symbol, indexTracked));
	}
	
	public ETF(String path) {
		// GAInfoFundName = "PowerShares QQQ";
		extract.add("NAME", 1,
				"\\p{javaWhitespace}+GAInfoFundName = \"(.+)\";");
		
		// GAInfoTicker = "QQQ";
		extract.add("SYMBOL", 1,
				"\\p{javaWhitespace}+GAInfoTicker = \"(.+)\";");
		
		// <span id="IssuerSpan">				ProShares			</span>
		extract.add("ISSUER", 1,
				"<span id=\"IssuerSpan\">\\p{javaWhitespace}+(.+)\\p{javaWhitespace}+</span>");
		
		// <span id="InceptionDateSpan">				03/10/99			</span>
		extract.add("INCEPTION_DATE", 3,
				"<span id=\"InceptionDateSpan\">\\p{javaWhitespace}+([0-9]{2})/([0-9]{2})/([0-9]{2})\\p{javaWhitespace}+</span>");
		
		// <span id="ExpenseRatioSpan">				0.20%			</span>
		// <span id="ExpenseRatioSpan">				--			</span>
		extract.add("EXPENSE_RATIO", 1,
				"<span id=\"ExpenseRatioSpan\">\\p{javaWhitespace}+(.+)\\p{javaWhitespace}+</span>");
		
		// <a href="http://www.proshares.com/funds/ubio.html" title="Fund Home Page" target="_blank" id="fundHomePageLink">Fund Home Page</a>
		extract.add("HOME_PAGE", 1,
				"<a href=\"(.+)\" title=\"Fund Home Page\" target=\"_blank\" id=\"fundHomePageLink\">Fund Home Page</a>");
		
		// <span id="AssetsUnderManagementSpan">				$14.23 M			</span>
		extract.add("AUM", 1,
				"<span id=\"AssetsUnderManagementSpan\">\\p{javaWhitespace}+(.+)\\p{javaWhitespace}+</span>");
		
		// <span id="IndexTrackedSpan">				NASDAQ-100 Index			</span>
		extract.add("INDEX_TRACKED", 1,
				"(?s)<span id=\"IndexTrackedSpan\">(.+?)</span>",
				"IndexTrackedSpan");

		
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
		ETF nameList = new ETF(DIR_PATH);
		logger.info("map = {}", nameList.map.size());
		logger.info("STOP");
	}
}
