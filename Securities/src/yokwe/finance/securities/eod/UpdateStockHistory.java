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
	public static Map<String, List<StockHistory>> getStockHistoryMap() {
		Map<String, List<StockHistory>> ret = new TreeMap<>();
		
		List<StockHistory> allStockHistory = CSVUtil.loadWithHeader(PATH_STOCK_HISTORY, StockHistory.class);
		for(StockHistory stockHistory: allStockHistory) {
			String key = stockHistory.group;
			if (!ret.containsKey(key)) {
				ret.put(key, new ArrayList<>());
			}
			ret.get(stockHistory.group).add(stockHistory);
		}
		
		for(Map.Entry<String, List<StockHistory>> entry: ret.entrySet()) {
			Collections.sort(entry.getValue());
		}
		
		return ret;
	}
	
	
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
			
			logger.info("");
			List<StockHistory> stockHistoryList = StockHistory.getStockList();
			for(StockHistory stockHistory: stockHistoryList) {
				// Change symbol style
				stockHistory.group  = stockHistory.group.replace(".PR.", "-");
				stockHistory.symbol = stockHistory.symbol.replace(".PR.", "-");
				logger.info("{}", stockHistory);
			}
			Collections.sort(stockHistoryList);
			
			logger.info("transactionList  = {}", transactionList.size());
			logger.info("stockHistoryList = {}", stockHistoryList.size());
			
			logger.info("save stockHistoryList as {}", PATH_STOCK_HISTORY);
			CSVUtil.saveWithHeader(stockHistoryList, PATH_STOCK_HISTORY);
		}
	}
		
	public static void main(String[] args) {
		logger.info("START");
		
		generateReport(Report.URL_ACTIVITY);
		
		logger.info("STOP");
		System.exit(0);
	}
}
