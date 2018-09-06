package yokwe.finance.stock.iex;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import yokwe.finance.stock.util.CSVUtil;

public class UpdateSymbols {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateSymbols.class);
	
	public static String getCSVPath() {
		return IEXBase.getCSVPath(Symbols.class);
	}

	private static List<String> symbolList = null;
	
	public static List<String> getSymbolList() {
		if (symbolList == null) {
			String csvPath = getCSVPath();
			logger.info("csvPath {}", csvPath);
			List<Symbols> symbolsList = CSVUtil.loadWithHeader(csvPath, Symbols.class);
			logger.info("symbolsList {}", symbolsList.size());
			
			// Exclude not enabled and crypto
			symbolList = symbolsList.stream().filter(o -> o.isEnabled).filter(o -> !o.type.equals("crypto")).map(o -> o.symbol).collect(Collectors.toList());
			Collections.sort(symbolList);
			logger.info("symbolList {}", symbolList.size());
		}
		return symbolList;
	}
	
	public static void main (String[] args) {
		logger.info("START");
		
		Symbols[] symbols = Symbols.getRefData();
		logger.info("symbols {}", symbols.length);
		
		String csvPath = getCSVPath();
		logger.info("csvPath {}", csvPath);

		CSVUtil.saveWithHeader(Arrays.asList(symbols), csvPath);
		logger.info("STOP");
	}
}
