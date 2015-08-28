package yokwe.finance.etf;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.etf.ETF.Element;

public class Google {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Google.class);
	
	private static final String DIR_PATH = "tmp/fetch/etf/google";

	public final Map<String, Element> map          = new TreeMap<>();
	
	// <title>Exchange Listed Funds Trust GaveKal Knowledge Leaders Developed World ETF: NYSEARCA:KLDW quotes & news - Google Finance</title>
	private static Extract extractTitle = new Extract.Simple("TITLE", 3, "<title>(.+): (.+):(.+) quotes & news - Google Finance</title>");
		
	private void extractInfo(File file) {
		String fileName = file.getName();
//		logger.debug("{}", fileName);
		
		String contents = Util.getContents(file);
		
		String name         = extractTitle.getValue(fileName, contents);
		String exchange     = extractTitle.getValue(2);
		String symbol       = extractTitle.getValue(3);
		
		name = name.replace("&amp;", "&");
		name = name.replace("&#39;", "'");
		
//		map.put(symbol, new Element(symbol, name, inceptionDate, expenseRatio, issuer, homePage, aum, adv));
		
		logger.debug("{}", String.format("%-8s %-8s %s", exchange, symbol, name));
	}

	public Google(String path) {
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
		Google nameList = new Google(DIR_PATH);
		logger.info("map = {}", nameList.map.size());
		logger.info("STOP");
	}

}
