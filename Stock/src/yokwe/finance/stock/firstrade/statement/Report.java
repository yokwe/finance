package yokwe.finance.stock.firstrade.statement;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import yokwe.finance.stock.UnexpectedException;
import yokwe.finance.stock.firstrade.Transaction;
import yokwe.finance.stock.firstrade.tax.Account;
import yokwe.finance.stock.firstrade.tax.BuySell;
import yokwe.finance.stock.firstrade.tax.Transfer;
import yokwe.finance.stock.libreoffice.Sheet;
import yokwe.finance.stock.libreoffice.SpreadSheet;
import yokwe.finance.stock.util.DoubleUtil;

public class Report {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Report.class);
	
	public static final String TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());

	public static final String URL_TEMPLATE      = "file:///home/hasegawa/Dropbox/Trade/TEMPLATE_FIRSTRADE_TAX.ods";
	public static final String URL_REPORT        = String.format("file:///home/hasegawa/Dropbox/Trade/Report/FIRSTRADE_STATEMENT_%s.ods", TIMESTAMP);

	
	static List<Account> getAccountList(List<Transaction> transactionList) {
		List<Account> ret = new ArrayList<>();
		
		double fundTotal  = 0;
		double cashTotal  = 0;
		double stockTotal = 0;
		double gainTotal  = 0;
		
		for(Transaction transaction: transactionList) {
			String date = transaction.date;
			
			switch(transaction.type) {
			case WIRE_IN:
			{
				double credit = transaction.credit;
				fundTotal += credit;
				cashTotal += credit;
				
				ret.add(Account.wireIn(date, fundTotal, cashTotal, stockTotal, gainTotal, credit));
			}
				break;
			case WIRE_OUT:
			{
				double debit = transaction.debit;
				fundTotal -= debit;
				cashTotal -= debit;
				
				ret.add(Account.wireOut(date, fundTotal, cashTotal, stockTotal, gainTotal, debit));
			}
				break;
			case ACH_IN:
			{
				double credit = transaction.credit;
				fundTotal += credit;
				cashTotal += credit;
				
				ret.add(Account.achIn(date, fundTotal, cashTotal, stockTotal, gainTotal, credit));
			}
				break;
			case ACH_OUT:
			{
				double debit = transaction.debit;
				fundTotal -= debit;
				cashTotal -= debit;
				
				ret.add(Account.achOut(date, fundTotal, cashTotal, stockTotal, gainTotal, debit));
			}
				break;
			case INTEREST:
			{
				double credit = transaction.credit;
				cashTotal += credit;
				gainTotal += credit;
				
				ret.add(Account.interest(date, fundTotal, cashTotal, stockTotal, gainTotal, credit));
			}
				break;
			case DIVIDEND:
			{
				String symbol = transaction.symbol;
				
				double debit  = transaction.debit;
				double credit = transaction.credit;
				
				if (!DoubleUtil.isAlmostZero(debit)) {
					cashTotal -= debit;
					gainTotal -= debit;

					ret.add(Account.dividend(date, fundTotal, cashTotal, stockTotal, gainTotal, symbol, -debit));
				}
				if (!DoubleUtil.isAlmostZero(credit)) {
					cashTotal += credit;
					gainTotal += credit;

					ret.add(Account.dividend(date, fundTotal, cashTotal, stockTotal, gainTotal, symbol, credit));
				}
			}
				break;
			case BUY:
			{
				String symbol     = transaction.symbol;
				Transfer transfer = Transfer.getByID(transaction.id);
				double   buy      = DoubleUtil.roundPrice(transfer.buy.buy + transfer.buy.fee);

				cashTotal  -= buy;
				stockTotal += buy;
				ret.add(Account.buy(date, fundTotal, cashTotal, stockTotal, gainTotal, symbol, buy));
			}
				break;
			case SELL:
			{
				String symbol     = transaction.symbol;
				Transfer transfer = Transfer.getByID(transaction.id);
				double sell       = DoubleUtil.roundPrice(transfer.sell.sell - transfer.sell.fee);
				double sellCost   = transfer.sell.cost;
				double sellGain   = DoubleUtil.roundPrice(sell - sellCost);
				
				cashTotal  += sell;
				stockTotal -= sellCost;
				gainTotal  += sellGain;

				ret.add(Account.sell(date, fundTotal, cashTotal, stockTotal, gainTotal, symbol, sell, sellCost, sellGain));
			}
				break;
			case CHANGE:
				break;
			default:
				logger.error("Unknown type = {}", transaction.type);
				throw new UnexpectedException("Unexpected");
			}
		}
		return ret;
	}


	public static void generateReport(String url) {
		logger.info("url        {}", url);		
		try (SpreadSheet docActivity = new SpreadSheet(url, true)) {
			SpreadSheet docLoad = new SpreadSheet(URL_TEMPLATE, true);
			SpreadSheet docSave = new SpreadSheet();

			// Create transaction from activity
			List<Transaction> transactionList = Transaction.getTransactionList(docActivity, false);
			
			// Need to call getBuySellMap to build Transfer record for getAccountList
			BuySell.getBuySellMap(transactionList); 
			
			// account activity list
			List<Account> accountList = getAccountList(transactionList);
			
			// Build yearList from accountList
			List<String> yearList = new ArrayList<>();
			yearList.addAll(accountList.stream().map(e -> e.date.substring(0, 4)).collect(Collectors.toSet()));
			// Add dummy date used in BuySell.addDummySell
			yearList.add(BuySell.DUMMY_DATE.substring(0, 4));
			Collections.sort(yearList);

			for(String targetYear: yearList) {
				// Account
				{
					List<Account> myList = new ArrayList<>();
					for(Account account: accountList) {
						if (account.date.startsWith(targetYear)) myList.add(account);
					}

					if (!myList.isEmpty()) {
						String sheetName = Sheet.getSheetName(Account.class);
						docSave.importSheet(docLoad, sheetName, docSave.getSheetCount());
						Sheet.fillSheet(docSave, myList);
						
						String newSheetName = String.format("%s-%s",  targetYear, sheetName);
						logger.info("sheet {}", newSheetName);
						docSave.renameSheet(sheetName, newSheetName);
					}
				}
			}
			
			// remove first sheet
			docSave.removeSheet(docSave.getSheetName(0));

			docSave.store(URL_REPORT);
			logger.info("output {}", URL_REPORT);
			docLoad.close();
		}
	}
	
	public static void main(String[] args) {
		logger.info("START");
		
		// For test
//		ForexUtil.enableTestMode();
//		generateReport(URL_ACTIVITY_TEST);
		
		// Not test
		generateReport(Transaction.URL_ACTIVITY);

		logger.info("STOP");
		System.exit(0);
	}
}
