package yokwe.finance.stock.monex;

import java.util.Collections;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.stock.data.StockHistory;
import yokwe.finance.stock.libreoffice.SpreadSheet;
import yokwe.finance.stock.util.CSVUtil;
import yokwe.finance.stock.util.DoubleUtil;

public class UpdateStockHistory {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateStockHistory.class);

	public static final String PATH_STOCK_HISTORY               = "tmp/monex/stock-history-monex.csv";
	
	public static List<StockHistory> getStockHistoryList() {
		try (SpreadSheet docActivity = new SpreadSheet(Transaction.URL_ACTIVITY, true)) {

			// Create transaction from activity
			List<Transaction> transactionList = Transaction.getTransactionList(docActivity);
			StockHistory.Builder builder = new StockHistory.Builder();
			
			for(Transaction transaction: transactionList) {
				switch(transaction.type) {
				case BUY:
				{
					String date     = transaction.date;
					String symbol   = transaction.symbol;
					double quantity = transaction.quantity;
					double buy      = DoubleUtil.round(transaction.total - transaction.fee, 2);
					double buyFee   = transaction.fee;
					
					builder.buy(date, symbol, quantity, buy, buyFee);
				}
					break;
				case SELL:
				{
					String date     = transaction.date;
					String symbol   = transaction.symbol;
					double quantity = transaction.quantity;
					double sell     = DoubleUtil.round(transaction.total + transaction.fee, 2);
					double sellFee  = transaction.fee;
					
					builder.sell(date, symbol, quantity, sell, sellFee);
				}
					break;
//					case DIVIDEND:
//					{
//						String date   = transaction.date;
//						String symbol = transaction.symbol;
//						double fee    = transaction.fee;
//						double debit  = transaction.debit;
//						double credit = transaction.credit;
//						
//						if (!DoubleUtil.isAlmostZero(fee)) {
//							logger.error("Unexpected {} {} {}", date, symbol, fee);
//							throw new SecuritiesException("Unexpected");
//						}
//						
//						StockHistory.dividend(date, symbol, credit, debit);
//					}
//						break;
//				case CHANGE:
//				{
//					String date        = transaction.date;
//					String symbol      = transaction.symbol;
//					double quantity    = transaction.quantity;
//					String newSymbol   = transaction.newSymbol;
//					double newQuantity = transaction.newQuantity;
//					
//					StockHistory.change(date, symbol, -quantity, newSymbol, newQuantity);
//				}
//					break;
				default:
					break;
				}
			}
			
			List<StockHistory> stockHistoryList = builder.getStockList();
			
			// Change symbol style from ".PR." to "-"
			for(StockHistory stockHistory: stockHistoryList) {
				stockHistory.group  = stockHistory.group.replace(".PR.", "-");
				stockHistory.symbol = stockHistory.symbol.replace(".PR.", "-");
			}
			Collections.sort(stockHistoryList);
			
			return stockHistoryList;
		}
	}

	public static void main(String[] args) {
		logger.info("START");
		
		List<StockHistory>stockHistoryList = getStockHistoryList();
		logger.info("stockHistoryList = {}", stockHistoryList.size());

		CSVUtil.saveWithHeader(stockHistoryList, PATH_STOCK_HISTORY);
		
		logger.info("STOP");
		System.exit(0);
	}
}
