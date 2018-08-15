package yokwe.finance.securities.monex;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;

public class Transaction {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Transaction.class);
	
	public static final String URL_ACTIVITY      = "file:///home/hasegawa/Dropbox/Trade/マネックス証券.ods";

	public enum Type {
		YEN_IN, YEN_OUT, USD_IN, USD_OUT,
		BUY,
	}

	public Type   type;
	public String date; // settlement date
	
	public String symbol;
	public int    quantity;
	public double price;
	public double fee;
	public double amount;
	
	public static List<Transaction> getTransactionList(SpreadSheet docActivity) {
		List<Transaction> transactionList = new ArrayList<>();
		
		List<String> sheetNameList = docActivity.getSheetNameList();
		sheetNameList.sort((a, b) -> a.compareTo(b));
		
		for(String sheetName: sheetNameList) {
			if (!sheetName.matches("^20[0-9][0-9]$")) {
				logger.warn("Sheet {} skip", sheetName);
				continue;
			}
			logger.info("Sheet {}", sheetName);
			
			List<Activity> activityList = Sheet.extractSheet(docActivity, Activity.class, sheetName);
			
			for(Activity activity: activityList) {
				logger.info("{}", activity);
			}
		}
		return transactionList;
	}
	
	public static void main(String[] args) {
		logger.info("START");
		
		String url = URL_ACTIVITY;
		logger.info("url        {}", url);		
		try (SpreadSheet docActivity = new SpreadSheet(url, true)) {
			List<Transaction> transactionList = getTransactionList(docActivity);
			logger.info("transactionList {}", transactionList.size());
		}
		logger.info("STOP");
	}
}
