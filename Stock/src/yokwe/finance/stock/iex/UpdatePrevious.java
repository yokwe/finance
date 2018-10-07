package yokwe.finance.stock.iex;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.stock.util.CSVUtil;

public class UpdatePrevious {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdatePrevious.class);
	
	public static String getCSVPath() {
		return IEXBase.getCSVPath(Previous.class);
	}

	private static List<String> symbolList = null;
	
	public static List<String> getSymbolList() {
		if (symbolList == null) {
			String csvPath = getCSVPath();
			logger.info("csvPath {}", csvPath);
			List<Previous> previousList = CSVUtil.loadWithHeader(csvPath, Previous.class);
			logger.info("previousList {}", previousList.size());
			
			Collections.sort(symbolList);
			logger.info("symbolList {}", symbolList.size());
		}
		return symbolList;
	}
	
	public static void main (String[] args) {
		logger.info("START");
		
		Previous[] previous = Previous.getMarket();
		logger.info("previous {}", previous.length);
		
		String csvPath = getCSVPath();
		logger.info("csvPath {}", csvPath);

		CSVUtil.saveWithHeader(Arrays.asList(previous), csvPath);
		logger.info("STOP");
	}
}
