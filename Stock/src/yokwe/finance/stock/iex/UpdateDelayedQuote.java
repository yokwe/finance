package yokwe.finance.stock.iex;

import org.slf4j.LoggerFactory;

public class UpdateDelayedQuote {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateDelayedQuote.class);
	
	public static String getCSVPath() {
		return IEXBase.getCSVPath(DelayedQuote.class);
	}

	public static void main (String[] args) {
		logger.info("START");
		
		IEXBase.updateCSV(DelayedQuote.class);
		
		logger.info("STOP");
	}
}
