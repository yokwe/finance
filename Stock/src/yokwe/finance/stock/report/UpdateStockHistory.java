package yokwe.finance.stock.report;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.stock.UnexpectedException;
import yokwe.finance.stock.data.StockHistory;
import yokwe.finance.stock.util.DoubleUtil;

public class UpdateStockHistory {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateStockHistory.class);

	public static List<StockHistory> getStockHistoryList(List<Transaction> transactionList, boolean includeDividend) {
		StockHistory.Builder builder = new StockHistory.Builder();
		
		for(Transaction transaction: transactionList) {
			switch(transaction.type) {
			case DIVIDEND:
				// We can get dividend after selling the stock.
//				if (!DoubleUtil.isAlmostZero(transaction.fee)) {
//					logger.error("Unexpected {} {} {}", transaction.date, transaction.symbol, transaction.fee);
//					throw new UnexpectedException("Unexpected");
//				}
				
				if (includeDividend) builder.dividend(transaction.date, transaction.symbol, transaction.debit, transaction.credit);
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
			case DEPOSIT_JPY:
			case WITHDRAW_JPY:
			case EXCHANGE_JPY_TO_USD:
			case EXCHANGE_USD_TO_JPY:
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
		Collections.sort(stockHistoryList);
		
		return stockHistoryList;
	}
	public static List<StockHistory> getStockHistoryListWithDividend(List<Transaction> transactionList) {
		return getStockHistoryList(transactionList, true);
	}
	public static List<StockHistory> getStockHistoryListWithoutDividend(List<Transaction> transactionList) {
		return getStockHistoryList(transactionList, false);
	}
	
	public static List<List<StockHistory>> getSotckHistoryListCollection(List<StockHistory> stockHistoryList) {
		Collections.sort(stockHistoryList);
		
		Map<Integer, List<StockHistory>> map = new TreeMap<>();
		for(StockHistory stockHistory: stockHistoryList) {
			int session = stockHistory.session;
			if (!map.containsKey(session)) {
				map.put(session, new ArrayList<>());
			}
			map.get(session).add(stockHistory);
		}
		
		List<List<StockHistory>> ret = new ArrayList<>();		
		// To make sure order of list
		for(List<StockHistory> list: map.values()) {
			Collections.sort(list);
			ret.add(new ArrayList<>(list));
		}
		
		// Sort with first element of list
		ret.sort((a, b) -> a.get(0).compareTo(b.get(0)));
		
		return ret;
	}
	
	private static String THIS_YEAR = String.format("%d", Calendar.getInstance().get(Calendar.YEAR));
	public static List<List<StockHistory>> filter(Collection<List<StockHistory>> listCollection, boolean wantActive, boolean wantThisYear) {
		List<List<StockHistory>> ret = new ArrayList<>();
		
		for(List<StockHistory> list: listCollection) {
			StockHistory last = list.get(list.size() - 1);
			
			if (wantActive) {
				if (last.isActive()) ret.add(list);
			}
			if (wantThisYear) {
				if (last.date.startsWith(THIS_YEAR)) ret.add(list);
			}
		}
		
		return ret;
	}
	public static List<List<StockHistory>> filter(List<StockHistory> stockHistoryList, boolean wantActive, boolean wantThisYear) {
		return filter(getSotckHistoryListCollection(stockHistoryList), wantActive, wantThisYear);
	}
	
	public static void main(String[] args) {
		logger.info("START");
		
		{
			List<StockHistory> stockHistoryList = getStockHistoryListWithDividend(Transaction.getMonex());
			for(StockHistory stockHistory: stockHistoryList) {
				logger.info("monex     {}", stockHistory);
			}
//			CSVUtil.saveWithHeader(stockHistoryList, "tmp/sh-m.csv");
		}
		
		{
			List<StockHistory> stockHistoryList = getStockHistoryListWithDividend(Transaction.getFirstrade());
			for(StockHistory stockHistory: stockHistoryList) {
				logger.info("firstarde {}", stockHistory);
			}
//			CSVUtil.saveWithHeader(stockHistoryList, "tmp/sh-f.csv");
		}
		
		logger.info("STOP");
		System.exit(0);
	}
}
