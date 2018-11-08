package yokwe.finance.stock.report;

import java.util.ArrayList;
import java.util.Calendar;
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

	public static List<StockHistory> getStockHistoryList(List<Transaction> transactionList) {
		StockHistory.Builder builder = new StockHistory.Builder();
		
		for(Transaction transaction: transactionList) {
			switch(transaction.type) {
			case DIVIDEND:
				// We can get dividend after selling the stock.
//				if (!DoubleUtil.isAlmostZero(transaction.fee)) {
//					logger.error("Unexpected {} {} {}", transaction.date, transaction.symbol, transaction.fee);
//					throw new UnexpectedException("Unexpected");
//				}
				
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

	private static List<StockHistory> onlyActiveSession(List<StockHistory> stockHistoryList) {
		Map<Integer, List<StockHistory>> map = new TreeMap<>();
		for(StockHistory stockHistory: stockHistoryList) {
			int session = stockHistory.session;
			if (!map.containsKey(session)) {
				map.put(session, new ArrayList<>());
			}
			map.get(session).add(stockHistory);
		}
		
		List<StockHistory> ret = new ArrayList<>();
		for(List<StockHistory> list: map.values()) {
			StockHistory last = list.get(list.size() - 1);
			if (last.totalQuantity == 0) continue;
			
			ret.addAll(list);
		}
		
		Collections.sort(ret);
		return ret;
	}

	private static String THIS_YEAR = String.format("%d", Calendar.getInstance().get(Calendar.YEAR));
	private static List<StockHistory> onlyActiveThisYear(List<StockHistory> stockHistoryList) {
		Map<Integer, List<StockHistory>> map = new TreeMap<>();
		for(StockHistory stockHistory: stockHistoryList) {
			int session = stockHistory.session;
			if (!map.containsKey(session)) {
				map.put(session, new ArrayList<>());
			}
			map.get(session).add(stockHistory);
		}
		
		List<StockHistory> ret = new ArrayList<>();
		for(List<StockHistory> list: map.values()) {
			StockHistory last = list.get(list.size() - 1);
			if (!last.date.startsWith(THIS_YEAR)) continue;
			
			ret.addAll(list);
		}
		
		Collections.sort(ret);
		return ret;
	}
	
	public static List<StockHistory> getActiveStockHistoryList(List<Transaction> transactionList) {
		return onlyActiveSession(getStockHistoryList(transactionList));
	}
	
	public static List<StockHistory> getActiveThisYearStockHistoryList(List<Transaction> transactionList) {
		return onlyActiveThisYear(getStockHistoryList(transactionList));
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
