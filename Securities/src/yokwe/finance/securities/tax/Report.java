package yokwe.finance.securities.tax;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class Report {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(Report.class);
	
	public static void main(String[] args) {
		logger.info("START");
		
		String url = "file:///home/hasegawa/Dropbox/Trade/投資損益.ods";
		String targetYear = "2016";
		
		// key is "date-symbol"
		Map<String, Dividend> dividendMap = new TreeMap<>();
		// key is symbol
		//Map<String, BuySell> transferMap = new TreeMap<>();
		//TODO BuySell contains List<List<Transfer>>.  Is this correct?
		
		try (LibreOffice libreOffice = new LibreOffice(url, true)) {
			for(Activity activity: Sheet.getInstance(libreOffice, Activity.class)) {
				logger.info("activity {} {} {}", activity.date, activity.transaction, activity.symbol);
				double fxRate = Mizuho.getUSD(activity.date);
				
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
					// Add record to dividendMap that belong target year
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
					// Add record to dividendMap that belong target year
					if (activity.date.startsWith(targetYear)) {
						String key = String.format("%s-%s", activity.date, "____");
						if (dividendMap.containsKey(key)) {
							Dividend dividend = dividendMap.get(key);
							dividend.update(activity.credit, activity.debit);
						} else {
							Dividend dividend = Dividend.getInstance(activity.date, activity.credit, activity.debit, fxRate);
							dividendMap.put(key, dividend);
						}
					}
					break;
				}
				default:
					logger.error("Unknonw transaction {}", activity.transaction);
					throw new SecuritiesException("Unknonw transaction");
				}
			}
		}
		
		{
			String timeStamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
			logger.info("timeStamp {}", timeStamp);
			
			String urlLoad = "file:///home/hasegawa/Dropbox/Trade/REPORT_TEMPLATE.ods";
			String urlSave = String.format("file:///home/hasegawa/Dropbox/Trade/REPORT_%s.ods", timeStamp);
			
			try (LibreOffice docLoad = new LibreOffice(urlLoad, true)) {
//				SheetData.saveSheet(docLoad, ReportTransfer.class, reportTransferList);
				{
					List<Dividend> dividendList = new ArrayList<>();
					{
						List<String> keyList = new ArrayList<>();
						keyList.addAll(dividendMap.keySet());
						Collections.sort(keyList);
						for(String key: keyList) {
							Dividend dividend = dividendMap.get(key);
							dividendList.add(dividend);
						}
					}
					Sheet.saveSheet(docLoad, Dividend.class, dividendList);
				}
				docLoad.store(urlSave);
			}
		}
		
		logger.info("STOP");
		System.exit(0);
	}
}
