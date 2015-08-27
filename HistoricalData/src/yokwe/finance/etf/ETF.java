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
	private Extract extractName   = new Extract.Simple("NAME",   "\\p{javaWhitespace}+GAInfoFundName = \"(.+)\";");
	
	// GAInfoTicker = "QQQ";
	private Extract extractSymbol = new Extract.Simple("SYMBOL", "\\p{javaWhitespace}+GAInfoTicker = \"(.+)\";");
	
	// <span id="IssuerSpan">				ProShares			</span>
	private Extract extractIssuer = new Extract.Simple("ISSUER", "<span id=\"IssuerSpan\">\\p{javaWhitespace}+(.+)\\p{javaWhitespace}+</span>");
	
	// <span id="InceptionDateSpan">				03/10/99			</span>
	private Extract extractInceptionDate = new Extract.MMDDYY("INCEPTION_DATE", "<span id=\"InceptionDateSpan\">\\p{javaWhitespace}+([0-9]{2})/([0-9]{2})/([0-9]{2})\\p{javaWhitespace}+</span>");
	
	// <span id="ExpenseRatioSpan">				0.20%			</span>
	// <span id="ExpenseRatioSpan">				--			</span>
	private Extract extractExpenseRatio = new ExtractExpenseRatio("<span id=\"ExpenseRatioSpan\">\\p{javaWhitespace}+([0-9]\\.[0-9]{2}%|--)\\p{javaWhitespace}+</span>");
	
	// <a href="http://www.proshares.com/funds/ubio.html" title="Fund Home Page" target="_blank" id="fundHomePageLink">Fund Home Page</a>
	

	public final Map<String, String> mapName          = new TreeMap<>();
	public final Map<String, String> mapInceptionDate = new TreeMap<>();
	public final Map<String, String> mapExpenseRatio  = new TreeMap<>();
	public final Map<String, String> mapIssuer        = new TreeMap<>();
	
	private void extractInfo(File file) {
		String fileName = file.getName();
		//logger.debug("{}", fileName);
		
		String contents = Util.getContents(file);
		
		String symbol        = extractSymbol.getValue(fileName, contents);
		String name          = extractName.getValue(fileName, contents);
		String inceptionDate = extractInceptionDate.getValue(fileName, contents);
		String expenseRatio  = extractExpenseRatio.getValue(fileName, contents);
		String issuer        = extractIssuer.getValue(fileName, contents);
		
		mapName.put(symbol, name);
		mapInceptionDate.put(symbol, inceptionDate);
		mapExpenseRatio.put(symbol, expenseRatio);
		mapIssuer.put(symbol, issuer);
		
//		logger.debug("{} {}", symbol, issuer);
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
		logger.info("mapName          = {}", nameList.mapName.size());
		logger.info("mapInceptionDate = {}", nameList.mapInceptionDate.size());
		logger.info("mapExpenseRatio  = {}", nameList.mapExpenseRatio.size());
		logger.info("mapIssuer        = {}", nameList.mapIssuer.size());
		logger.info("STOP");
	}
}
