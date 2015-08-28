package yokwe.finance.etf;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

public class ETF {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(ETF.class);
	
	private static final String DIR_PATH = "tmp/fetch/etf/etf";
	
	public static class ExtractExpenseRatio extends Extract {
		public ExtractExpenseRatio(String pattern) {
			super("EXPENSE_RATIO", 1, pattern);
		}
		protected String getValue(String fileName) {
			String group1 = matcher.group(1);
			if (group1.endsWith("%")) {
				return group1.substring(0, 4);
			} else if (group1.compareTo("--") == 0) {
				logger.warn("{}  {}  NA", fileName, name);
				return "NA";
			} else {
				logger.error("{} {}  UNEXPECTED = {}", fileName, name, group1);
				throw new RuntimeException(name + " UNEXPECTED");
			}
		}
	}
	
	// GAInfoFundName = "PowerShares QQQ";
	private static Extract extractName   = new Extract.Simple("NAME",   "\\p{javaWhitespace}+GAInfoFundName = \"(.+)\";");
	
	// GAInfoTicker = "QQQ";
	private static Extract extractSymbol = new Extract.Simple("SYMBOL", "\\p{javaWhitespace}+GAInfoTicker = \"(.+)\";");
	
	// <span id="IssuerSpan">				ProShares			</span>
	private static Extract extractIssuer = new Extract.Simple("ISSUER", "<span id=\"IssuerSpan\">\\p{javaWhitespace}+(.+)\\p{javaWhitespace}+</span>");
	
	// <span id="InceptionDateSpan">				03/10/99			</span>
	private static Extract extractInceptionDate = new Extract.MMDDYY("INCEPTION_DATE", "<span id=\"InceptionDateSpan\">\\p{javaWhitespace}+([0-9]{2})/([0-9]{2})/([0-9]{2})\\p{javaWhitespace}+</span>");
	
	// <span id="ExpenseRatioSpan">				0.20%			</span>
	// <span id="ExpenseRatioSpan">				--			</span>
	private static Extract extractExpenseRatio = new ExtractExpenseRatio("<span id=\"ExpenseRatioSpan\">\\p{javaWhitespace}+([0-9]\\.[0-9]{2}%|--)\\p{javaWhitespace}+</span>");
	
	// <a href="http://www.proshares.com/funds/ubio.html" title="Fund Home Page" target="_blank" id="fundHomePageLink">Fund Home Page</a>
	private static Extract extractHomePage = new Extract.Simple("HOME_PAGE", "<a href=\"(.+)\" title=\"Fund Home Page\" target=\"_blank\" id=\"fundHomePageLink\">Fund Home Page</a>");
	
	// <span id="AssetsUnderManagementSpan">				$14.23 M			</span>
	private static Extract extractAUM = new Extract.Simple("AUM", "<span id=\"AssetsUnderManagementSpan\">\\p{javaWhitespace}+(.+)\\p{javaWhitespace}+</span>");
	
	// <span id="AverageDailyVolumeSpan">				$87.24 K			</span>
	private static Extract extractADV = new Extract.Simple("ADM", "<span id=\"AverageDailyVolumeSpan\">\\p{javaWhitespace}+(.+)\\p{javaWhitespace}+</span>");
	
	
	public static final class Element {
		public final String symbol;
		public final String name;
		public final String inceptionDate;
		public final String expenseRatio;
		public final String issuer;
		public final String homePage;
		public final String assetsUnderManagement;
		public final String averageDailyVolume;
		
		public Element(String symbol, String name, String inceptionDate, String expenseRatio, String iuuser, String homePage, String assetsUnderManagement, String averageDailyVolume) {
			this.symbol                = symbol;
			this.name                  = name;
			this.inceptionDate         = inceptionDate;
			this.expenseRatio          = expenseRatio;
			this.issuer                = iuuser;
			this.homePage              = homePage;
			this.assetsUnderManagement = assetsUnderManagement;
			this.averageDailyVolume    = averageDailyVolume;
		}
	}
	
	public final Map<String, Element> map          = new TreeMap<>();
	
	private void extractInfo(File file) {
		String fileName = file.getName();
		logger.debug("{}", fileName);
		
		String contents = Util.getContents(file);
		
		String symbol           = extractSymbol.getValue(fileName, contents);
		String name             = extractName.getValue(fileName, contents);
		String inceptionDate    = extractInceptionDate.getValue(fileName, contents);
		String expenseRatio     = extractExpenseRatio.getValue(fileName, contents);
		String issuer           = extractIssuer.getValue(fileName, contents);
		String homePage         = extractHomePage.getValue(fileName, contents);
		String aum              = extractAUM.getValue(fileName, contents);
		String adv              = extractADV.getValue(fileName, contents);
		
		map.put(symbol, new Element(symbol, name, inceptionDate, expenseRatio, issuer, homePage, aum, adv));
		
		logger.debug("{}", String.format("%-8s %s", symbol));
	}
	
	public ETF(String path) {
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
		ETF nameList = new ETF(DIR_PATH);
		logger.info("map = {}", nameList.map.size());
		logger.info("STOP");
	}
}
