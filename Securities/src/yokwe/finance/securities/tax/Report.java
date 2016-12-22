package yokwe.finance.securities.tax;

import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class Report {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(Report.class);
	
	public static void main(String[] args) {
		logger.info("START");
		String url = "file:///home/hasegawa/Dropbox/Trade/投資損益計算.ods";
		String targetYear = "2016";
		
		// key is date
		Map<String, Double> usdMap    = new TreeMap<>();
		// key is symbol
		Map<String, Equity> equityMap = new TreeMap<>();

		// key is "date-symbol"
		Map<String, Dividend> dividendMap = new TreeMap<>();
		// key is symbol
		//Map<String, BuySell> transferMap = new TreeMap<>();
		//TODO BuySell contains List<List<Transfer>>.  Is this correct?
		
		try (LibreOffice libreOffice = new LibreOffice(url, true)) {

			for(Mizuho mizuho: Sheet.getInstance(libreOffice, Mizuho.class)) {
				usdMap.put(mizuho.date, mizuho.usd);
			}
			for(Equity equity: Sheet.getInstance(libreOffice, Equity.class)) {
				equityMap.put(equity.symbol, equity);
			}
			
			for(Activity activity: Sheet.getInstance(libreOffice, Activity.class)) {
				double fxRate = usdMap.get(activity.date);
				
				switch(activity.transaction) {
				// Transfer
				case "BOUGHT":
				case "NAME CHG": {
					// Use tradeDate
					break;
				}
				case "SOLD":
				case "REDEEMED": {
					// Use tradeDate
					break;
				}
				// Dividend
				case "DIVIDEND":
				case "ADR":
				case "MLP":
				case "NRA":
				case "CAP GAIN": {
					// 			String date, String symbol, String symbolName, double quantity,

					if (activity.date.startsWith(targetYear)) {
						// Create Dividend Record
						String key = String.format("%s-%s", activity.date, activity.symbol);
						if (dividendMap.containsKey(key)) {
							Dividend dividend = dividendMap.get(key);
							dividend.update(activity.credit, activity.debit);
						} else {
							Dividend dividend = Dividend.getInstance(activity.date, activity.symbol, activity.name, activity.quantity,
									activity.credit, activity.debit, fxRate);
							dividendMap.put(key, dividend);
						}
					}
					break;
				}
				case "INTEREST": {
					// Create Dividend Record
					String key = String.format("%s-%s", activity.date, "____");
					if (dividendMap.containsKey(key)) {
						Dividend dividend = dividendMap.get(key);
						dividend.update(activity.credit, activity.debit);
					} else {
						Dividend dividend = Dividend.getInstance(activity.date, activity.credit, activity.debit, fxRate);
						dividendMap.put(key, dividend);
					}
					break;
				}
				default:
					logger.error("Unknonw transaction {}", activity.transaction);
					throw new SecuritiesException("Unknonw transaction");
				}
			}
			
		}
	}
}
