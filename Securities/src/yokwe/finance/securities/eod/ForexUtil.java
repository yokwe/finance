package yokwe.finance.securities.eod;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class ForexUtil {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ForexUtil.class);

	private static DateMap<Forex> forexMap = new DateMap<>();
	
	static {
		for(Forex forex: Forex.load()) {
			forexMap.put(forex.date, forex);
		}
		logger.info("forexMap {}", forexMap.size());
	}
	
	private static boolean testMode = false;
	public static void enableTestMode() {
		testMode = true;
		logger.info("enableTestMode {}", testMode);
	}
	
	
	public static Forex get(String date) {
		return forexMap.get(date);
	}
	public static String getValidDate(String date) {
		return forexMap.getValidDate(date);
	}
	
	public static double getUSD(String date) {
		if (testMode) return 1;
		
		return get(date).usd;
	}
	
	public static void main(String[] args) {
		logger.info("START");
		
		{
			String date = "2017-01-02";
			logger.info("date {}  validDate {}", date, getValidDate(date));
		}
		{
			String date = "2017-01-03";
			logger.info("date {}  validDate {}", date, getValidDate(date));
		}
		{
			String date = "2017-01-04";
			logger.info("date {}  validDate {}", date, getValidDate(date));
		}
		{
			String date = "2017-01-05";
			logger.info("date {}  validDate {}", date, getValidDate(date));
		}
		{
			String date = "2099-01-01";
			logger.info("date {}  validDate {}", date, getValidDate(date));
		}
		try {
			String date = "2000-01-01";
			logger.info("date {}  validDate {}", date, getValidDate(date));
		} catch (SecuritiesException e) {
			logger.info("e {}", e.getMessage());
		}
		 
		logger.info("STOP");
	}
}
