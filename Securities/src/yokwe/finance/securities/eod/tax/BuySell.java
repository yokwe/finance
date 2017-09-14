package yokwe.finance.securities.eod.tax;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.eod.Price;
import yokwe.finance.securities.eod.PriceUtil;
import yokwe.finance.securities.util.DoubleUtil;

public class BuySell {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(BuySell.class);

	String symbol;
	String name;

	int    buyCount;
	String dateBuyFirst;
	String dateBuyLast;

	double totalQuantity;
	double totalCost;
	int    totalCostJPY;
	double totalDividend;
	
	List<Transfer>       current;
	List<List<Transfer>> past;
	
	public BuySell(String symbol, String name) {
		this.symbol = symbol;
		this.name   = name;

		current     = new ArrayList<>();
		past        = new ArrayList<>();

		reset();
	}
	
	void reset() {
		buyCount      = 0;
		dateBuyFirst  = "";
		dateBuyLast   = "";
		
		totalQuantity = 0;
		totalCost     = 0;
		totalCostJPY  = 0;
	}
	
	boolean isAlmostZero() {
		return DoubleUtil.isAlmostZero(totalQuantity);
	}

	void buy(Transaction tansaction) {
		double fxRate = tansaction.fxRate;
		buyCount++;
		if (buyCount == 1) {
			dateBuyFirst = tansaction.date;
		} else {
			dateBuyLast  = tansaction.date;
		}
		
		// maintain totalQuantity, totalAcquisitionCost and totalAcquisitionCostJPY
		double costPrice = Transaction.roundPrice(tansaction.quantity * tansaction.price);
		double costFee   = tansaction.fee;
		double cost      = costPrice + costFee;
		int    costJPY   = (int)Math.floor(costPrice * fxRate) + (int)Math.floor(costFee * fxRate);
		
		totalQuantity += tansaction.quantity;
		totalCost     += cost;
		totalCostJPY  += costJPY;

		Transfer.Buy buy = new Transfer.Buy(
			tansaction.date, tansaction.symbol, tansaction.name,
			tansaction.quantity, tansaction.price, tansaction.fee, fxRate,
			totalQuantity, totalCost, totalCostJPY
			);
		current.add(new Transfer(buy));
	}
	void sell(Transaction transaction) {
		double fxRate  = transaction.fxRate;
		double sell    = Transaction.roundPrice(transaction.price * transaction.quantity);
		int    sellJPY = (int)Math.floor(sell * fxRate);
		int    feeJPY  = (int)Math.floor(transaction.fee * fxRate);

		double sellRatio = transaction.quantity / totalQuantity;
		double cost      = Transaction.roundPrice(totalCost * sellRatio);
		int    costJPY;
		
		if (buyCount == 1) {
			costJPY = (int)Math.floor(totalCostJPY * sellRatio);
			
			// maintain totalQuantity, totalAcquisitionCost and totalAcquisitionCostJPY
			totalQuantity -= transaction.quantity;
			totalCost     -= cost;
			totalCostJPY  -= costJPY;
			
			// date symbol name sellAmountJPY asquisionCostJPY sellCommisionJPY dateBuyFirst dateBuyLast
			logger.info("SELL {}", String.format("%s %-9s %9.5f %7d %7d %7d %s %s",
					transaction.date, symbol, totalQuantity, sellJPY, costJPY, feeJPY, dateBuyFirst, dateBuyLast));
		} else {
			double unitCostJPY = Math.ceil(totalCostJPY / totalQuantity); // need to be round up. See https://www.nta.go.jp/taxanswer/shotoku/1466.htm
			costJPY = (int)Math.floor(unitCostJPY * transaction.quantity);
			// need to adjust totalAcquisitionCostJPY
			totalCostJPY = (int)Math.floor(unitCostJPY * totalQuantity);
			
			// maintain totalQuantity, totalAcquisitionCost and totalAcquisitionCostJPY
			totalQuantity -= transaction.quantity;
			totalCost     -= cost;
			totalCostJPY  -= costJPY;
			
			// date symbol name sellAmountJPY asquisionCostJPY sellCommisionJPY dateBuyFirst dateBuyLast
			logger.info("SELL*{}", String.format("%s %-9s %9.5f %7d %7d %7d %s %s",
					transaction.date, symbol, totalQuantity, sellJPY, totalCostJPY, feeJPY, dateBuyFirst, dateBuyLast));
		}

		Transfer.Sell transferSell = new Transfer.Sell(
			transaction.date, transaction.symbol, transaction.name,
			transaction.quantity, transaction.price, transaction.fee, fxRate,
			cost, costJPY,
			dateBuyFirst, dateBuyLast, totalDividend,
			totalQuantity, totalCost, totalCostJPY
			);
		if (buyCount == 1 && current.size() == 1 && isAlmostZero()) {
			// Special case buy one time and sell whole
			Transfer.Buy transferBuy = current.remove(0).buy;
			current.add(new Transfer(transferBuy, transferSell));
		} else {
			current.add(new Transfer(transferSell));
		}
		past.add(current);
		current = new ArrayList<>();
		//
		if (isAlmostZero()) {
			reset();
		}
	}
	void change(Transaction transaction) {
		if (-transaction.quantity == transaction.newQuantity) {
			// rename symbol with newSymbol
			String newSymbol = transaction.newSymbol;
			String newName   = transaction.newName;
			
			this.symbol = newSymbol;
			this.name   = newName;
			
//			for(Transfer transfer: current) {
//				if (transfer.buy != null) {
//					transfer.buy.symbol = newSymbol;
//					transfer.buy.name = newName;
//				}
//				if (transfer.sell != null) {
//					transfer.sell.symbol = newSymbol;
//					transfer.sell.name = newName;
//				}
//			}
//			for(List<Transfer> transferList: past) {
//				for(Transfer transfer: transferList) {
//					if (transfer.buy != null) {
//						transfer.buy.symbol = newSymbol;
//						transfer.buy.name = newName;
//					}
//					if (transfer.sell != null) {
//						transfer.sell.symbol = newSymbol;
//						transfer.sell.name = newName;
//					}
//				}
//			}
		} else {
			logger.error("quantity  {}  {}", transaction.quantity, transaction.newQuantity);
			throw new SecuritiesException("Unexpected");
		}
	}
	
	
	public static Map<String, BuySell>  getBuySellMap(List<Transaction> transactionList) {
		Map<String, BuySell> ret = new TreeMap<>();
		
		// To calculate correct Japanese tax,
		// If buy and sell happen in same day, treat as all buy first then sell
		// Order of transaction should be change, buy and sell per stock for one day
		List<Transaction> reorderedTransactionList = new ArrayList<>();
		{
			String lastDate   = "*NA*";
			String lastSymbol = "*NA*";
			List<Transaction> buyList    = new ArrayList<>();
			List<Transaction> sellList   = new ArrayList<>();
			List<Transaction> changeList = new ArrayList<>();
			
			for(Transaction transaction: transactionList) {
				String date   = transaction.date;
				String symbol = transaction.symbol;
				
				if (!date.equals(lastDate) || !symbol.equals(lastSymbol)) {
					reorderedTransactionList.addAll(changeList);
					reorderedTransactionList.addAll(buyList);
					reorderedTransactionList.addAll(sellList);
					
					buyList.clear();
					sellList.clear();
					changeList.clear();
				}

				switch(transaction.type) {
				case BUY:
					buyList.add(transaction);
					break;
				case SELL:
					sellList.add(transaction);
					break;
				case CHANGE:
					changeList.add(transaction);
					break;
				default:
					break;
				}
				
				lastDate = date;
				lastSymbol = symbol;
			}
			
			reorderedTransactionList.addAll(buyList);
			reorderedTransactionList.addAll(sellList);
			reorderedTransactionList.addAll(changeList);
		}

		for(Transaction transaction: reorderedTransactionList) {
			if (transaction.type == Transaction.Type.BUY) {
				String key = transaction.symbol;
				BuySell buySell;
				if (ret.containsKey(key)) {
					buySell = ret.get(key);
				} else {
					buySell = new BuySell(transaction.symbol, transaction.name);
					ret.put(key, buySell);
				}
				buySell.buy(transaction);
			}
			if (transaction.type == Transaction.Type.SELL) {
				String key = transaction.symbol;
				BuySell buySell;
				if (ret.containsKey(key)) {
					buySell = ret.get(key);
				} else {
					logger.error("Unknonw symbol {}", key);
					throw new SecuritiesException("Unexpected");
				}
				
				buySell.sell(transaction);
			}
			if (transaction.type == Transaction.Type.CHANGE) {
				String key = transaction.symbol;
				
				BuySell buySell;
				if (ret.containsKey(key)) {
					buySell = ret.get(key);
				} else {
					logger.error("Unknonw symbol {}", key);
					throw new SecuritiesException("Unexpected");
				}
				ret.remove(key);
				
				buySell.change(transaction);
				ret.put(buySell.symbol, buySell);
			}
		}
		
		// Add dummy sell record for remaining stocks
		addDummySell(ret);
		return ret;
	}
	
	public static final String DUMMY_DATE = "9999-12-31";
	public static final double DUMMY_FEE  = 5.0;
	private static void addDummySell(Map<String, BuySell> buySellMap) {
		for(BuySell buySell: buySellMap.values()) {
			if (buySell.isAlmostZero()) continue;
			
			String symbol   = buySell.symbol;
			String name     = buySell.name;
			double quantity = buySell.totalQuantity;
			
			// Get latest price of symbol
			Price price = PriceUtil.getLastPrice(symbol);
//			logger.info("price {} {} {}", price.date, price.symbol, price.close);

			// Add dummy sell record
			Transaction transaction = Transaction.sell(DUMMY_DATE, symbol, name, quantity, price.close, DUMMY_FEE, Transaction.roundPrice(quantity * price.close));	
			buySell.sell(transaction);
		}
	}

}
