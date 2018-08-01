package yokwe.finance.securities.iex;

import org.slf4j.LoggerFactory;

public class UpdateStats {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateStats.class);
	
	public static String getCSVPath() {
		return IEXBase.getCSVPath(Stats.class);
	}

	public static void main (String[] args) {
		logger.info("START");
		
		IEXBase.updateCSV(Stats.class);

		logger.info("STOP");
	}

}
