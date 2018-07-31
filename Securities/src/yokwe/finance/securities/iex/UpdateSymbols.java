package yokwe.finance.securities.iex;

import java.util.Arrays;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.util.CSVUtil;

public class UpdateSymbols {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateSymbols.class);
	
	public static final String PATH_SYMBOLS = "tmp/iex/symbols.csv";
	
	public static void main (String[] args) {
		logger.info("START");
		
		Symbols[] symbols = Symbols.getRefData();
		logger.info("symbols {}", symbols.length);
		
		CSVUtil.saveWithHeader(Arrays.asList(symbols), PATH_SYMBOLS);
		logger.info("STOP");
	}
}
