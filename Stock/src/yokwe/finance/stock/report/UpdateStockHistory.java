package yokwe.finance.stock.report;

import java.util.Collections;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.stock.UnexpectedException;
import yokwe.finance.stock.data.StockHistory;
import yokwe.finance.stock.util.DoubleUtil;

public class UpdateStockHistory {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateStockHistory.class);

	public static List<StockHistory> getStockHistoryList(List<Transaction> transactionList) {
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
				
				builder.buy(transaction.date, transaction.symbol, transaction.quantity, transaction.fee, transaction.debit);
				break;
			case SELL:
				if (!DoubleUtil.isAlmostZero(transaction.debit)) {
					logger.error("Unexpected {} {} {}", transaction.date, transaction.symbol, transaction.debit);
					throw new UnexpectedException("Unexpected");
				}
				
				builder.sell(transaction.date, transaction.symbol, transaction.quantity, transaction.fee, transaction.credit);
				break;
			case CHANGE:
				builder.change(transaction.date, transaction.symbol, -transaction.quantity, transaction.newSymbol, transaction.newQuantity);
				break;
			case DEPOSIT:
			case WITHDRAW:
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

	public static void main(String[] args) {
		logger.info("START");
		
		{
			List<StockHistory> stockHistoryList = getStockHistoryList(Transaction.getMonex());
			for(StockHistory stockHistory: stockHistoryList) {
				logger.info("monex     {}", stockHistory);
			}
//			CSVUtil.saveWithHeader(stockHistoryList, "tmp/sh-m.csv");
		}
		
		{
			List<StockHistory> stockHistoryList = getStockHistoryList(Transaction.getFirstrade());
			for(StockHistory stockHistory: stockHistoryList) {
				logger.info("firstarde {}", stockHistory);
			}
//			CSVUtil.saveWithHeader(stockHistoryList, "tmp/sh-f.csv");
		}
		
		logger.info("STOP");
		System.exit(0);
	}
}
