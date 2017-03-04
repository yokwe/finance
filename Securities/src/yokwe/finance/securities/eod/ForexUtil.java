package yokwe.finance.securities.eod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class ForexUtil {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ForexUtil.class);

	private static List<String> dateList = new ArrayList<>();
	// key is date
	private static Map<String, Forex> forexMap = new TreeMap<>();
	
	static {
		for(Forex forex: Forex.load()) {
			dateList.add(forex.date);
			forexMap.put(forex.date, forex);
		}
		logger.info("forexMap {}", forexMap.size());
		// sort dateList
		Collections.sort(dateList);
	}
	
	private static boolean testMode = false;
	public static void enableTestMode() {
		testMode = true;
		logger.info("enableTestMode {}", testMode);
	}
	
	public static String getValidDate(String date) {
		int index = Collections.binarySearch(dateList, date);
		if (index < 0) {
			index = - (index + 1) - 1;
			if (index < 0) {
				logger.info("Unexpected date = {}", date);
				throw new SecuritiesException("Unexpected");
			}
		}
		return dateList.get(index);
	}
	
	public static Forex get(String date) {
		String validDate = getValidDate(date);
		return forexMap.get(validDate);
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
