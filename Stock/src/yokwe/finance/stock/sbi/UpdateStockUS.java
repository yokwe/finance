package yokwe.finance.stock.sbi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.stock.UnexpectedException;
import yokwe.finance.stock.util.CSVUtil;
import yokwe.finance.stock.util.HttpUtil;

public class UpdateStockUS {
	private static final Logger logger = LoggerFactory.getLogger(UpdateStockUS.class);

	public static final String SOURCE_URL       = "https://search.sbisec.co.jp/v2/popwin/info/stock/pop6040_usequity_list.html";
	public static final String SOURCE_ENCODING  = "SHIFT_JIS";
	
	public static final String PATH_SBI_US = "tmp/sbi/sbi-stock-us.csv";

	public static void save(List<StockUS> usSecurityList) {
		CSVUtil.saveWithHeader(usSecurityList, PATH_SBI_US);
	}
	public static List<StockUS> load() {
		return CSVUtil.loadWithHeader(PATH_SBI_US, StockUS.class);
	}
	
	private static final String PAT_TABLE_STRING = "<div class=\"accTbl01\">\\s+<table [^>]+>(.+?)</table>";
	private static final String PAT_TR_STRING    = "<tr>(.+?)</tr>";
	private static final String PAT_STOCK_STRING = "<th>(.+?)<td>(.+?)<br>(.+?)<td>(.+?)<td>(.+)";
	private static final String PAT_ETF_STRING   = "<th>(.+?)<td>(.+?)<br>(.+?)<td>(.+)";
	private static final String PAT_CATEGORY_STRING = "class=\"ttlM\">(.+?)</h";

	private static final Pattern PAT_TABLE = Pattern.compile(PAT_TABLE_STRING, (Pattern.MULTILINE | Pattern.DOTALL));
	private static final Pattern PAT_TR    = Pattern.compile(PAT_TR_STRING,    (Pattern.MULTILINE | Pattern.DOTALL));
	private static final Pattern PAT_STOCK = Pattern.compile(PAT_STOCK_STRING, (Pattern.MULTILINE | Pattern.DOTALL));
	private static final Pattern PAT_ETF   = Pattern.compile(PAT_ETF_STRING,   (Pattern.MULTILINE | Pattern.DOTALL));
	private static final Pattern PAT_CATEGORY = Pattern.compile(PAT_CATEGORY_STRING,   (Pattern.MULTILINE | Pattern.DOTALL));

	public static void main(String[] args) {
		logger.info("START");
		
		List<StockUS> stockUSList = new ArrayList<>();
		int countStock = 0;
		int countETF = 0;
		
		String content = HttpUtil.downloadAsString(SOURCE_URL, SOURCE_ENCODING);
		
		Matcher mt = PAT_TABLE.matcher(content);
		for(;;) {
			if (!mt.find()) break;
			String table = mt.group(1);
			table = table.replaceAll("<([a-z]+) .+?>" , "<$1>");
			table = table.replaceAll("<(div|/div|p|/p)>" , "");
			table = table.replaceAll("(\n|\r)+" , "\n");
//			logger.info("=====<<");
//			logger.info("  {}", table);
//			logger.info("=====>>");
			
			String category;
			{
				int startPos = mt.start();
				int pos = content.lastIndexOf("class=\"ttlM\">", startPos);
				if (pos != -1) {
					Matcher mc = PAT_CATEGORY.matcher(content.substring(pos - 6, startPos));
					if (mc.find()) {
						category = mc.group(1);
						category = category.replace("普通株式一覧", "普通株式");
						category = category.replace("海外ETF一覧", "ETF");
						
//						logger.info("@@@@ category {}", category);
					} else {
						logger.error("Unexpected {} {}", startPos, pos);
						throw new UnexpectedException("Unexpected");
					}
				} else {
					logger.error("Unexpected {} {}", startPos, pos);
					throw new UnexpectedException("Unexpected");
				}
			}
			
			StockUS stockUS = null;
			Matcher mr = PAT_TR.matcher(table);
			for(;;) {
				if (!mr.find()) break;
				String row = mr.group(1);
				row = row.replaceAll("(\r|\n)+", "");
				row = row.replaceAll("</.+?>", "");
//				logger.info("    !{}!", row);
				
				if (row.startsWith("<th>ティッカー")) continue;
				
				Matcher ms = PAT_STOCK.matcher(row);
				Matcher me = PAT_ETF.matcher(row);
				if (ms.find()) {
					if (ms.groupCount() != 5) {
						logger.error("Unexpected row = {}", row);
						throw new UnexpectedException("Unexpected");
					}
					String ticker = ms.group(1);
					String name   = ms.group(2);
					String nameJP = ms.group(3);
					String description = ms.group(4);
					String market = ms.group(5);
										
//					logger.info("++++ {}={}={}={}={}=", ticker, name, nameJP, description, market);
					stockUS = new StockUS(ticker, name, nameJP, description, market, category);
					
					countStock++;
				} else if (me.find()) {
					if (me.groupCount() != 4) {
						logger.error("Unexpected row = {}", row);
						throw new UnexpectedException("Unexpected");
					}
					String ticker = me.group(1);
					String name   = me.group(2);
					String nameJP = me.group(3);
					String market = me.group(4);
					
//					logger.info("**** {}={}={}={}=", ticker, name, nameJP, market);
					stockUS = new StockUS(ticker, name, nameJP, "ETF", market, category);
					
					countETF++;
				} else {
					logger.error("Unexpected row = {}", row);
					throw new UnexpectedException("Unexpected");
				}
				
				if (stockUS != null) {
					stockUS.name = stockUS.name.replaceAll("&amp;", "&");
					stockUS.nameJP = stockUS.nameJP.replaceAll("&amp;", "&");
					stockUS.nameJP = stockUS.nameJP.replaceAll("&#37569;", "鋁");
					stockUS.description = stockUS.description.replaceAll("&amp;", "&");
					stockUSList.add(stockUS);
				}
			}
		}
		
		Collections.sort(stockUSList);

		logger.info("URL    = {}", SOURCE_URL);
		logger.info("OUTPUT = {}", PATH_SBI_US);
		save(stockUSList);

		logger.info("ETF    = {}", String.format("%5d", countETF));
		logger.info("STOCK  = {}", String.format("%5d", countStock));
		logger.info("TOTAL  = {}", String.format("%5d", countETF + countStock));
		
		logger.info("STOP");		
	}
}
