package yokwe.finance.securities.eod.tax;

import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.libreoffice.SpreadSheet;
import yokwe.finance.securities.util.DoubleUtil;

public class UpdateStock {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateStock.class);

	public static void generateReport(String url) {
		logger.info("url        {}", url);		
		try (SpreadSheet docActivity = new SpreadSheet(url, true)) {

			// Create transaction from activity
			List<Transaction> transactionList = Transaction.getTransactionList(docActivity);
			
			for(Transaction transaction: transactionList) {
				switch(transaction.type) {
				case DIVIDEND:
				{
					String date   = transaction.date;
					String symbol = transaction.symbol;
					double fee    = transaction.fee;
					double debit  = transaction.debit;
					double credit = transaction.credit;
					
					if (!DoubleUtil.isAlmostZero(fee)) {
						logger.error("Unexpected {} {} {}", date, symbol, fee);
						throw new SecuritiesException("Unexpected");
					}
					
					Stock.dividend(date, symbol, credit, debit);
				}
					break;
				case BUY:
				{
					String date     = transaction.date;
					String symbol   = transaction.symbol;
					double quantity = transaction.quantity;
					double buy      = transaction.debit;
					double buyFee   = transaction.fee;
					
					if (!DoubleUtil.isAlmostZero(transaction.credit)) {
						logger.error("Unexpected {} {} {}", date, symbol, transaction.credit);
						throw new SecuritiesException("Unexpected");
					}
					
					Stock.buy(date, symbol, quantity, buy, buyFee);
				}
					break;
				case SELL:
				{
					String date     = transaction.date;
					String symbol   = transaction.symbol;
					double quantity = transaction.quantity;
					double sell     = transaction.credit;
					double sellFee  = transaction.fee;
					
					if (!DoubleUtil.isAlmostZero(transaction.debit)) {
						logger.error("Unexpected {} {} {}", date, symbol, transaction.debit);
						throw new SecuritiesException("Unexpected");
					}
					
					Stock.sell(date, symbol, quantity, sell, sellFee);
				}
					break;
				case CHANGE:
				{
					String date        = transaction.date;
					String symbol      = transaction.symbol;
					double quantity    = transaction.quantity;
					String newSymbol   = transaction.newSymbol;
					double newQuantity = transaction.newQuantity;
					
					Stock.change(date, symbol, -quantity, newSymbol, newQuantity);
				}
					break;
				default:
					break;
				}
			}
			
			logger.info("");
			int stockCount = 0;
			for(String symbol: Stock.getSymbolList()) {
				for(Stock stock: Stock.getStockList(symbol)) {
					stockCount++;
					logger.info("Stock {}", String.format("%-9s %s", symbol, stock.toString()));
				}
			}
			logger.info("transactionList = {}", transactionList.size());
			logger.info("stockCount      = {}", stockCount);
		}
	}
		
	public static void main(String[] args) {
		logger.info("START");
		
		generateReport(Report.URL_ACTIVITY);
		
		logger.info("STOP");
		System.exit(0);
	}
}
