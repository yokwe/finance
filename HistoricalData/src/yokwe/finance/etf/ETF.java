package yokwe.finance.etf;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

public class ETF {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ETF.class);
	
	private static final String DIR_PATH = "tmp/fetch/etf/etf";
	
	// GAInfoFundName = "PowerShares QQQ";
	private static final Matcher M_NAME = Pattern.compile("\\p{javaWhitespace}+GAInfoFundName = \"(.+)\";").matcher("");
	
	// GAInfoTicker = "QQQ";
	private static final Matcher M_SYMBOL = Pattern.compile("\\p{javaWhitespace}+GAInfoTicker = \"(.+)\";").matcher("");
	
	// <span id="InceptionDateSpan">				03/10/99			</span>
	private static final Matcher M_INCEPTION_DATE = Pattern.compile("<span id=\"InceptionDateSpan\">\\p{javaWhitespace}+([0-9]{2})/([0-9]{2})/([0-9]{2})\\p{javaWhitespace}+</span>").matcher("");
	
	// <span id="ExpenseRatioSpan">				0.20%			</span>
	// <span id="ExpenseRatioSpan">				--			</span>
	private static final Matcher M_EXPENSE_RATIO = Pattern.compile("<span id=\"ExpenseRatioSpan\">\\p{javaWhitespace}+([0-9]\\.[0-9]{2}%|--)\\p{javaWhitespace}+</span>").matcher("");
	
	// <span id="IssuerSpan">				ProShares			</span>
	private static final Matcher M_ISSUER = Pattern.compile("<span id=\"IssuerSpan\">\\p{javaWhitespace}+(.+)\\p{javaWhitespace}+</span>").matcher("");
	
	// <a href="http://www.proshares.com/funds/ubio.html" title="Fund Home Page" target="_blank" id="fundHomePageLink">Fund Home Page</a>
	

	public final Map<String, String> mapName          = new TreeMap<>();
	public final Map<String, String> mapInceptionDate = new TreeMap<>();
	public final Map<String, String> mapExpenseRatio  = new TreeMap<>();
	public final Map<String, String> mapIssuer        = new TreeMap<>();
	
	private void extractInfo(File file) {
		String fileName = file.getName();
		//logger.debug("{}", fileName);
		
		String contents = Util.getContents(file);
		{
			M_SYMBOL.reset(contents);
			boolean foundSymbol = M_SYMBOL.find();
			
			M_NAME.reset(contents);
			boolean foundName = M_NAME.find();
			
			M_INCEPTION_DATE.reset(contents);
			boolean foundInceptionDate = M_INCEPTION_DATE.find();
			
			M_EXPENSE_RATIO.reset(contents);
			boolean foundExpenseRatio = M_EXPENSE_RATIO.find();
			
			M_ISSUER.reset(contents);
			boolean foundIssuer = M_ISSUER.find();
			
			// Sanity check
			if (!foundSymbol) {
				logger.error("{}  SYMBOL", fileName);
				throw new RuntimeException("SYMBOL");
			}
			if (M_SYMBOL.groupCount() != 1) {
				logger.error("{}  SYMBOL COUNT = {}", fileName, M_SYMBOL.groupCount());
				throw new RuntimeException("SYMBOL COUNT");
			}
			
			if (!foundName) {
				logger.error("{}  NAME", fileName);
				throw new RuntimeException("NAME");
			}
			if (M_NAME.groupCount() != 1) {
				logger.error("{}  NAME COUNT = {}", fileName, M_NAME.groupCount());
				throw new RuntimeException("NAME COUNT");
			}
			
			if (!foundInceptionDate) {
				logger.error("{}  INCEPTION_DATE", fileName);
				throw new RuntimeException("INCEPTION_DATE");
			}
			if (M_INCEPTION_DATE.groupCount() != 3) {
				logger.error("{}  INCEPTION_DATE COUNT = {}", fileName, M_INCEPTION_DATE.groupCount());
				throw new RuntimeException("INCEPTION_DATE COUNT");
			}
			
			if (!foundExpenseRatio) {
				logger.error("{}  EXPENSE_RATIO", fileName);
				throw new RuntimeException("EXPENSE_RATIO");
			}
			if (M_EXPENSE_RATIO.groupCount() != 1) {
				logger.error("{}  EXPENSE_RATIO COUNT = {}", fileName, M_EXPENSE_RATIO.groupCount());
				throw new RuntimeException("EXPENSE_RATIO COUNT");
			}
			
			if (!foundIssuer) {
				logger.error("{}  ISSUER", fileName);
				throw new RuntimeException("ISSUER");
			}
			if (M_ISSUER.groupCount() != 1) {
				logger.error("{}  ISSUER COUNT = {}", fileName, M_ISSUER.groupCount());
				throw new RuntimeException("ISSUER COUNT");
			}

			
			String symbol        = M_SYMBOL.group(1);
			String name          = M_NAME.group(1);
			String mm            = M_INCEPTION_DATE.group(1);
			String dd            = M_INCEPTION_DATE.group(2);
			String yy            = M_INCEPTION_DATE.group(3);
			String expenseRatio  = M_EXPENSE_RATIO.group(1);
			String issuer        = M_ISSUER.group(1);
			
			String expenseRatioValue = null;
			if (expenseRatio.endsWith("%")) {
				expenseRatioValue = expenseRatio.substring(0, 4);
			} else if (expenseRatio.compareTo("--") == 0) {
				expenseRatioValue = "NA";
				logger.warn("EXPENSE_RATIO NA  {}", symbol);
			} else {
				logger.error("{}  EXPENSE_RATIO UNEXPECTED = {}", fileName, expenseRatio);
				throw new RuntimeException("EXPENSE_RATIO UNEXPECTED");
			}
			
			mapName.put(symbol, name);
			mapInceptionDate.put(symbol, String.format("20%s-%s-%s", yy, mm, dd));
			mapExpenseRatio.put(symbol, expenseRatioValue);
			mapIssuer.put(symbol, issuer);
			
			logger.debug("ISSUER {}  {}", symbol, issuer);
		}
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
		ETF nameList = new ETF(DIR_PATH);
		logger.info("mapName          = {}", nameList.mapName.size());
		logger.info("mapInceptionDate = {}", nameList.mapInceptionDate.size());
		logger.info("mapExpenseRatio  = {}", nameList.mapExpenseRatio.size());
		logger.info("mapIssuer        = {}", nameList.mapIssuer.size());
	}

}
