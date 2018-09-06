package yokwe.finance.stock.iex;

import org.slf4j.LoggerFactory;

public class UpdateQuote {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateQuote.class);
	
	public static String getCSVPath() {
		return IEXBase.getCSVPath(Quote.class);
	}

	public static void main (String[] args) {
		logger.info("START");
		
		IEXBase.updateCSV(Quote.class);

		logger.info("STOP");
	}
}
