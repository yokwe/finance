package yokwe.finance.securities.monex;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;

public class Transaction {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Transaction.class);
	
	public static final String URL_ACTIVITY      = "file:///home/hasegawa/Dropbox/Trade/マネックス証券.ods";

	public enum Type {
		JPY_IN, JPY_OUT, USD_IN, USD_OUT,
		BUY,
	}
	
	private static final String PRODUCT_KINSEN   = "金銭";
	private static final String PRODUCT_SOTOKABU = "外株";
	
	private static final String DETAIL_FROM_SOUGOU    = "証券総合口座より";
	private static final String DETAIL_TO_DEPOSIT_USD = "円貨から外貨預り金へ（外貨）";
	private static final String DETAIL_TO_DEPOSIT_JPY = "円貨から外貨預り金へ（円貨）";

	private static final String ACCOUNT_TYPE = "特定";

	public final Type   type;
	public final String date; // settlement date
	
	public final String symbol;
	public final int    quantity;
	public final double price;
	public final double fee;
	public final double total;
	
	public final int    jpy;
	public final double usd;
	public final double fxRate;
	
	private Transaction(
		Type type, String date,
		String symbol, int quantity, double price, double fee, double total,
		int jpy, double usd, double fxRate) {
		this.type     = type;
		this.date     = date;
		
		this.symbol   = symbol;
		this.quantity = quantity;
		this.price    = price;
		this.fee      = fee;
		this.total    = total;
		
		this.jpy      = jpy;
		this.usd      = usd;
		this.fxRate   = fxRate;
	}
	
	@Override
	public String toString() {
		return String.format("%s %7s %s %4d %6.2f %6.2f %6.2f %8d %8.2f %6.2f",
				date, type, symbol, quantity, price, fee, total, jpy, usd, fxRate);
	}
	private static Transaction jpyIn(String date, int jpy) {
		return new Transaction(Type.JPY_IN, date, "", 0, 0, 0, 0, jpy, 0, 0);
	}
	private static Transaction usdIn(String date, int jpy, double usd, double fxRate) {
		return new Transaction(Type.USD_IN, date, "", 0, 0, 0, 0, jpy, usd, fxRate);
	}
	
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
			
			Transaction lastTransaction = null;
			for(Activity activity: activityList) {
				// Sanity check
				if (activity.settlementDate == null) {
					logger.error("Unexpected  {}", activity);
					throw new SecuritiesException("Unexpected");
				}
				if (activity.tradeDate == null) {
					logger.error("Unexpected  {}", activity);
					throw new SecuritiesException("Unexpected");
				}
				if (activity.product == null) {
					logger.error("Unexpected  {}", activity);
					throw new SecuritiesException("Unexpected");
				}
				if (activity.detail == null) {
					logger.error("Unexpected  {}", activity);
					throw new SecuritiesException("Unexpected");
				}
				if (activity.amount == null) {
					logger.error("Unexpected  {}", activity);
					throw new SecuritiesException("Unexpected");
				}

//				logger.info("{}", activity);
				
				switch(activity.product) {
				case PRODUCT_KINSEN:
				{
					logger.info("KINSEN   {}", activity);
					
					// Sanity check
					if (!activity.tradeDate.equals(activity.settlementDate)) {
						logger.error("Unexpected  {}", activity);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.accountType != null) {
						logger.error("Unexpected  {}", activity);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.transaction != null) {
						logger.error("Unexpected  {}", activity);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.amount == null) {
						logger.error("Unexpected  {}", activity);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.fxRate != null) {
						logger.error("Unexpected  {}", activity);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.fee != null) {
						logger.error("Unexpected  {}", activity);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.tax != null) {
						logger.error("Unexpected  {}", activity);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.total != null) {
						logger.error("Unexpected  {}", activity);
						throw new SecuritiesException("Unexpected");
					}
					
					switch(activity.detail) {
					case DETAIL_FROM_SOUGOU:
					{
						// Sanity check
						if (activity.unitPrice != null) {
							logger.error("Unexpected  {}", activity);
							throw new SecuritiesException("Unexpected");
						}
						double amount = activity.amount;
						{
							int decimal = (int)amount;
							double fractional = amount - decimal;
							if (fractional != 0) {
								logger.error("Unexpected  {}", activity);
								throw new SecuritiesException("Unexpected");
							}
						}

						lastTransaction = Transaction.jpyIn(activity.settlementDate, (int)amount);
						transactionList.add(lastTransaction);
					}
						break;
					case DETAIL_TO_DEPOSIT_USD:
					{
						// Sanity check
						if (activity.unitPrice == null) {
							logger.error("Unexpected  {}", activity);
							throw new SecuritiesException("Unexpected");
						}
						
						lastTransaction = Transaction.usdIn(activity.settlementDate, 0, activity.amount, activity.unitPrice);
					}
						break;
					case DETAIL_TO_DEPOSIT_JPY:
					{
						// Sanity check
						if (activity.unitPrice != null) {
							logger.error("Unexpected  {}", activity);
							throw new SecuritiesException("Unexpected");
						}
						double amount = activity.amount;
						{
							int decimal = (int)amount;
							double fractional = amount - decimal;
							if (fractional != 0) {
								logger.error("Unexpected  {}", activity);
								throw new SecuritiesException("Unexpected");
							}
						}
						
						if (lastTransaction.type != Type.USD_IN) {
							logger.error("Unexpected  {}", activity);
							throw new SecuritiesException("Unexpected");
						}
						if (!lastTransaction.date.equals(activity.settlementDate)) {
							logger.error("Unexpected  {}", activity);
							throw new SecuritiesException("Unexpected");
						}

						lastTransaction = Transaction.usdIn(lastTransaction.date, (int)amount, lastTransaction.usd, lastTransaction.fxRate);
						transactionList.add(lastTransaction);
					}
						break;
					default:
						logger.error("Unexpected  {}", activity);
						throw new SecuritiesException("Unexpected");
					}
				}
					break;
				case PRODUCT_SOTOKABU:
				{
					logger.info("SOTOKABU {}", activity);
					// Sanity check
					if (!activity.accountType.equals(ACCOUNT_TYPE)) {
						logger.error("Unexpected  {}", activity);
						throw new SecuritiesException("Unexpected");
					}
				}
					break;
				default:
					logger.error("Unexpected  {}", activity);
					throw new SecuritiesException("Unexpected");
				}
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
			for(Transaction transaction: transactionList) {
				logger.info("{}", transaction);
			}
			logger.info("transactionList {}", transactionList.size());
		}
		logger.info("STOP");
	}
}
