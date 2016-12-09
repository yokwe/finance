package yokwe.finance.securities.book;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class Report {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(Report.class);

	public static void main(String[] args) {
		logger.info("START");
		String url = "file:///home/hasegawa/Dropbox/Trade/投資損益計算.ods";
		
		try (LibreOffice libreOffice = new LibreOffice(url, true)) {
			List<Transaction> transactionList = SheetData.getInstance(libreOffice, Transaction.class);
			List<ReportTransfer> reportTransferList = new ArrayList<>();
			List<ReportDividend> reportDividendList = new ArrayList<>();
	
			Mizuho.Map mizuhoMap = new Mizuho.Map(url);
			EquityStats.Map equtiyStatsMap = new EquityStats.Map(url);

			for(Transaction transaction: transactionList) {
				switch (transaction.transaction) {
				case "BOUGHT":
				case "NAME CHG": {
					double usdjpy = mizuhoMap.get(transaction.tradeDate).usd;
					Transfer.buy(transaction.tradeDate, transaction.symbol, transaction.name, transaction.quantity, transaction.price, transaction.commission, usdjpy, reportTransferList);
					break;
				}
				case "SOLD":
				case "REDEEMED": {
					double usdjpy = mizuhoMap.get(transaction.tradeDate).usd;
					Transfer.sell(transaction.tradeDate, transaction.symbol, transaction.name, transaction.quantity, transaction.price, transaction.commission, usdjpy, reportTransferList);
					break;
				}
				case "DIVIDEND":
				case "ADR": {
					double usdjpy = mizuhoMap.get(transaction.date).usd;
					Dividend.dividend(
						transaction.transaction, transaction.date, transaction.symbol, transaction.name, transaction.quantity, transaction.credit, transaction.debit, usdjpy);
					break;
				}
				case "MLP": {
					double usdjpy = mizuhoMap.get(transaction.date).usd;
					Dividend.mlp(
						transaction.transaction, transaction.date, transaction.symbol, transaction.name, transaction.quantity, transaction.credit, transaction.debit, usdjpy);
					break;
				}
				case "NRA": {
					double usdjpy = mizuhoMap.get(transaction.date).usd;
					Dividend.nra(
						transaction.transaction, transaction.date, transaction.symbol, transaction.name, transaction.quantity, transaction.credit, transaction.debit, usdjpy);
					break;
				}
				case "CAP GAIN": {
					double usdjpy = mizuhoMap.get(transaction.date).usd;
					Dividend.capGain(
						transaction.transaction, transaction.date, transaction.symbol, transaction.name, transaction.quantity, transaction.credit, transaction.debit, usdjpy);
					break;
				}
				case "INTEREST":
					// Intentionally ignore 0.01% interest from Firstrade
					break;
				default: {
					logger.error("Unknown transaction = {}", transaction.transaction);
					throw new SecuritiesException("Unexpected");
				}
				}
			}
			
//			{
//				int n = 1;
//				for(Map.Entry<String, Securities> entry: securitiesMap.entrySet()) {
//					Securities securities = entry.getValue();
//					
//					if (securities.count == 1) {
//						// date symbol name sellAmountJPY asquisionCostJPY sellCommisionJPY dateBuyFirst dateBuyLast
//						logger.info("BUY  {}", String.format("%10d %-8s %9.5f %7d %7d %7d %s %s",
//								n++, securities.symbol, securities.quantity, 0, securities.acquisitionCostJPY, 0, securities.dateBuyFirst, securities.dateBuyLast));
//					} else {
//						double unitCost = Math.ceil(securities.acquisitionCostJPY / securities.quantity);
//						int acquisitionCostJPY = (int)Math.round(unitCost * securities.quantity);
//
//						// date symbol name sellAmountJPY asquisionCostJPY sellCommisionJPY dateBuyFirst dateBuyLast
//						logger.info("BUY *{}", String.format("%10d %-8s %9.5f %7d %7d %7d %s %s",
//								n++, securities.symbol, securities.quantity, 0, acquisitionCostJPY, 0, securities.dateBuyFirst, securities.dateBuyLast));
//					}
//				}
//			}
			
			// Output report of remaining securities
			
			{
				String lastDate = mizuhoMap.getLastDate();
				double usdjpy = mizuhoMap.get(lastDate).usd;
				logger.info("Last Date {} {}", lastDate, usdjpy);
				Transfer.addRemaining(lastDate, usdjpy, equtiyStatsMap, reportTransferList);
			}
			Dividend.addRemaining(reportDividendList);
			logger.info("reportTransferList = {}", reportTransferList.size());
			logger.info("reportDividendList = {}", reportDividendList.size());
			
			{
				String timeStamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
				logger.info("timeStamp {}", timeStamp);
				
				String urlLoad = "file:///home/hasegawa/Dropbox/Trade/REPORT_TEMPLATE.ods";
				String urlSave = String.format("file:///home/hasegawa/Dropbox/Trade/REPORT_%s.ods", timeStamp);
				
				try (LibreOffice docLoad = new LibreOffice(urlLoad, true)) {
					SheetData.saveSheet(docLoad, ReportTransfer.class, reportTransferList);
					SheetData.saveSheet(docLoad, ReportDividend.class, reportDividendList);
					docLoad.store(urlSave);
				}
			}
			
			logger.info("STOP");
			System.exit(0);
		}
	}

}
