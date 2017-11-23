package yokwe.finance.securities.eod;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.FileUtil;
import yokwe.finance.securities.util.HttpUtil;

public class UpdateCID {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateCID.class);
	
	public static final String PATH_CID = "tmp/eod/cid.csv";
	public static final String PATH_DIR = "tmp/eod/cid";
	
	private static String getURL(String exchange, String symbolGoogle) {
		// https://finance.google.com/finance?q=NYSE%3AESV
		return String.format("https://finance.google.com/finance?q=%s:%s", exchange, symbolGoogle);
	}
	
	private static class Match {
		final Matcher matcher;
		
		Match(String pattern) {
			matcher = Pattern.compile(pattern, Pattern.DOTALL).matcher("");
		}
		String getValue(String string) {
			boolean found = matcher.reset(string).find();
			if (!found) {
				logger.error("No match {}", matcher);
				throw new SecuritiesException("No match");
			}
			String ret = matcher.group(1);
			return ret;
		}
	}
	
	private static final Match NAME     = new Match("<meta itemprop=\"name\".+?content=\"(.+?)\" />");
	private static final Match SYMBOL   = new Match("<meta itemprop=\"tickerSymbol\".+?content=\"(.+?)\" />");
	private static final Match EXCHANGE = new Match("<meta itemprop=\"exchange\".+?content=\"(.+?)\" />");
	private static final Match URL_CID  = new Match("<meta itemprop=\"url\".+?content=\".+?cid=(.+?)\" />");
	
	public static String getContents(String exchange, String symbolGoogle) {
		String url = getURL(exchange, symbolGoogle);
		String ret = HttpUtil.downloadAsString(url);
		return ret;
	}
	
	public static CID getCID(String contents) {
		CID ret = null;
		
		if (contents.contains("itemprop")) {
			String mySymbol   = SYMBOL.getValue(contents);
			String myExchange = EXCHANGE.getValue(contents);
			String myName     = NAME.getValue(contents);
			String myCID      = URL_CID.getValue(contents);
			
			ret = new CID(mySymbol, myExchange, myName, myCID);
		}
		
		return ret;
	}
	
	private static String getKey(CID cid) {
		return String.format("%s:%s", cid.exchange, cid.symbol);
	}
	private static String getKey(Stock stock) {
		return String.format("%s:%s", stock.exchange, stock.symbolGoogle);
	}
	
	public static List<CID> getCIDList() {
		Map<String, CID> map = new TreeMap<>();
		List<CID>        ret = new ArrayList<>();
		
		File dir = new File(PATH_DIR);
		for(File file: dir.listFiles()) {
			String contents = FileUtil.read(file);
			CID cid = getCID(contents);
			
			String key = cid.cid;
			if (map.containsKey(key)) {
				CID oldCID = map.get(key);
				
				if (!cid.isEqual(oldCID)) {
					logger.warn("DIFFERENT");
					logger.warn("  OLD {}", oldCID);
					logger.warn("  NEW {}", cid);
				}
			} else {
				ret.add(cid);
				map.put(cid.cid, cid);
			}
		}

		return ret;
	}

	public static void main(String[] args) {
		logger.info("START");
		
		// Build existing cidList
		Map<String, CID> cidMap = new TreeMap<>();
		for(CID cid: getCIDList()) {
			cidMap.put(getKey(cid), cid);
		}

		
		List<Stock>	stockList = Stock.load();
		
		int symbolSize   = stockList.size();
		int showInterval = (symbolSize < 100) ? 1 : 100;
		int count = 0;
		int lastOutputCount = -1;
		
		showInterval = 1;
		
		for(Stock stock: stockList) {
			int outputCount = count / showInterval;
			boolean showOutput;
			if (outputCount != lastOutputCount) {
				showOutput = true;
				lastOutputCount = outputCount;
			} else {
				showOutput = false;
			}
			count++;
			
			String key = getKey(stock);
			if (cidMap.containsKey(key)) {
				if (showOutput) logger.info("{}  skip   {}", String.format("%4d / %4d",  count, symbolSize), stock.symbol);				
			} else {
				String contents = getContents(stock.exchange, stock.symbolGoogle);
				CID cid = getCID(contents);
				
				if (cid != null) {
					String timeStamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());

					File cidFile = new File(String.format("%s/%s-%s", PATH_DIR, timeStamp, cid.symbol));
					// create new cid file
					FileUtil.write(cidFile, contents);
					
					if (showOutput) logger.info("{}  new    {}", String.format("%4d / %4d",  count, symbolSize), stock.symbol);
				} else {
					// no data in google
					if (showOutput) logger.info("{}  none   {}", String.format("%4d / %4d",  count, symbolSize), stock.symbol);
				}
			}
		}
		
		logger.info("SAVE");
		CID.save(getCIDList());
		
		logger.info("STOP");
	}
}
