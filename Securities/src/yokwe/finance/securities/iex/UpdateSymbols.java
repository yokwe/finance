package yokwe.finance.securities.iex;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.util.CSVUtil;

public class UpdateSymbols {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateSymbols.class);
	
	public static final String PATH_SYMBOLS = "tmp/iex/symbols.csv";
	
	private static List<String> symbolList = null;
	
	public static List<String> getSymbolList() {
		if (symbolList == null) {
			List<Symbols> symbolsList = CSVUtil.loadWithHeader(UpdateSymbols.PATH_SYMBOLS, Symbols.class);
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
		
		CSVUtil.saveWithHeader(Arrays.asList(symbols), PATH_SYMBOLS);
		logger.info("STOP");
	}
}
