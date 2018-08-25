package yokwe.finance.securities.eod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.eod.tax.Report;
import yokwe.finance.securities.eod.tax.Transaction;
import yokwe.finance.securities.libreoffice.SpreadSheet;
import yokwe.finance.securities.util.CSVUtil;
import yokwe.finance.securities.util.DoubleUtil;

public class UpdateStockHistory {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateStockHistory.class);

	public static final String PATH_STOCK_HISTORY               = "tmp/eod/stock-history.csv";

	//                group
	public static Map<String, List<StockHistory>> getStockHistoryMap(String pathBase) {
		String path = String.format("%s/%s", pathBase, PATH_STOCK_HISTORY);
		List<StockHistory> stockHistoryList = CSVUtil.loadWithHeader(path, StockHistory.class);
		
		return getStockHistoryMap(stockHistoryList);
	}
	public static Map<String, List<StockHistory>> getStockHistoryMap(List<StockHistory> stockHistoryList) {
		Map<String, List<StockHistory>> ret = new TreeMap<>();
		
		for(StockHistory stockHistory: stockHistoryList) {
			String key = stockHistory.group;
			if (!ret.containsKey(key)) {
				ret.put(key, new ArrayList<>());
			}
			ret.get(key).add(stockHistory);
		}
		
		for(Map.Entry<String, List<StockHistory>> entry: ret.entrySet()) {
			Collections.sort(entry.getValue());
		}
		
		return ret;
	}
	public static Map<String, List<StockHistory>> getStockHistoryMap() {
		return getStockHistoryMap(".");
	}
	
	
	public static List<StockHistory> getStockHistoryList() {
		try (SpreadSheet docActivity = new SpreadSheet(Report.URL_ACTIVITY, true)) {

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
					
					StockHistory.dividend(date, symbol, credit, debit);
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
					
					StockHistory.buy(date, symbol, quantity, buy, buyFee);
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
					
					StockHistory.sell(date, symbol, quantity, sell, sellFee);
				}
					break;
				case CHANGE:
				{
					String date        = transaction.date;
					String symbol      = transaction.symbol;
					double quantity    = transaction.quantity;
					String newSymbol   = transaction.newSymbol;
					double newQuantity = transaction.newQuantity;
					
					StockHistory.change(date, symbol, -quantity, newSymbol, newQuantity);
				}
					break;
				default:
					break;
				}
			}
			
			List<StockHistory> stockHistoryList = StockHistory.getStockList();
			
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
