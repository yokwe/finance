package yokwe.finance.stock.firstrade;

import java.util.Collections;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.stock.UnexpectedException;
import yokwe.finance.stock.data.StockHistory;
import yokwe.finance.stock.data.StockHistoryUtil;
import yokwe.finance.stock.libreoffice.SpreadSheet;
import yokwe.finance.stock.util.CSVUtil;
import yokwe.finance.stock.util.DoubleUtil;

public class UpdateStockHistory {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateStockHistory.class);

	public static List<StockHistory> getStockHistoryList() {
		try (SpreadSheet docActivity = new SpreadSheet(Transaction.URL_ACTIVITY, true)) {

			// Create transaction from activity
			List<Transaction> transactionList = Transaction.getTransactionList(docActivity, true);
			StockHistory.Builder builder = new StockHistory.Builder();
			
			for(Transaction transaction: transactionList) {
				switch(transaction.type) {
				case DIVIDEND:
					if (!DoubleUtil.isAlmostZero(transaction.fee)) {
						logger.error("Unexpected {} {} {}", transaction.date, transaction.symbol, transaction.fee);
						throw new UnexpectedException("Unexpected");
					}
					
					builder.dividend(transaction.date, transaction.symbol, transaction.debit, transaction.credit);
					break;
				case BUY:
					if (!DoubleUtil.isAlmostZero(transaction.credit)) {
						logger.error("Unexpected {} {} {}", transaction.date, transaction.symbol, transaction.credit);
						throw new UnexpectedException("Unexpected");
					}
					
					builder.buy(transaction.date, transaction.symbol, transaction.quantity, transaction.fee, DoubleUtil.roundPrice(transaction.debit + transaction.fee));
					break;
				case SELL:
					if (!DoubleUtil.isAlmostZero(transaction.debit)) {
						logger.error("Unexpected {} {} {}", transaction.date, transaction.symbol, transaction.debit);
						throw new UnexpectedException("Unexpected");
					}
					
					builder.sell(transaction.date, transaction.symbol, transaction.quantity, transaction.fee, DoubleUtil.roundPrice(transaction.credit - transaction.fee));
					break;
				case CHANGE:
					builder.change(transaction.date, transaction.symbol, -transaction.quantity, transaction.newSymbol, transaction.newQuantity);
					break;
				case WIRE_IN:
				case WIRE_OUT:
				case ACH_IN:
				case ACH_OUT:
				case INTEREST:
					break;
				default:
					logger.error("Unexpected {}", transaction);
					throw new UnexpectedException("Unexpected");
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

		CSVUtil.saveWithHeader(stockHistoryList, StockHistoryUtil.PATH_STOCK_HISTORY_FIRSTRADE);
		
		logger.info("STOP");
		System.exit(0);
	}
}
