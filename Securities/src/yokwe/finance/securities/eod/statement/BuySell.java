package yokwe.finance.securities.eod.statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.eod.Price;
import yokwe.finance.securities.eod.PriceUtil;
import yokwe.finance.securities.eod.tax.Transaction;
import yokwe.finance.securities.util.DoubleUtil;

public class BuySell {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(BuySell.class);

	String symbol;
	String name;

	int    buyCount;

	double totalQuantity;
	double totalCost;
	
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
		
		totalQuantity = 0;
		totalCost     = 0;
	}
	
	boolean isAlmostZero() {
		return DoubleUtil.isAlmostZero(totalQuantity);
	}

	void buy(Transaction transaction) {
		buyCount++;
		
		totalQuantity += transaction.quantity;
		totalCost     += Transaction.roundPrice(transaction.quantity * transaction.price);

		Transfer.Buy buy = new Transfer.Buy(
			transaction.date, transaction.symbol, transaction.name,
			transaction.quantity, transaction.price, transaction.fee,
			totalQuantity, totalCost
			);
		current.add(new Transfer(transaction.id, buy));
	}
	void sell(Transaction transaction) {
		double sell = Transaction.roundPrice(transaction.price * transaction.quantity);
		double cost = Position.cost(transaction);
		
		if (buyCount == 1) {
			// maintain totalQuantity, totalAcquisitionCost and totalAcquisitionCostJPY
			totalQuantity -= transaction.quantity;
			totalCost     -= cost;
			
			// date symbol name sellAmountJPY asquisionCostJPY sellCommisionJPY dateBuyFirst dateBuyLast
			logger.info("SELL {}", String.format("%s %-9s %9.5f %9.2f %9.2f",
					transaction.date, symbol, totalQuantity, sell, cost));
		} else {
			// maintain totalQuantity, totalAcquisitionCost and totalAcquisitionCostJPY
			totalQuantity -= transaction.quantity;
			totalCost     -= cost;
			
			// date symbol name sellAmountJPY asquisionCostJPY sellCommisionJPY dateBuyFirst dateBuyLast
			logger.info("SELL*{}", String.format("%s %-9s %9.5f %9.2f %9.2f",
					transaction.date, symbol, totalQuantity, sell, cost));
		}

		Transfer.Sell transferSell = new Transfer.Sell(
			transaction.date, transaction.symbol, transaction.name,
			transaction.quantity, transaction.price, transaction.fee,
			cost, totalQuantity, totalCost
			);
		if (buyCount == 1 && current.size() == 1 && isAlmostZero()) {
			// Special case buy one time and sell whole
			Transfer.Buy transferBuy = current.remove(0).buy;
			current.add(new Transfer(transaction.id, transferBuy, transferSell));
		} else {
			current.add(new Transfer(transaction.id, transferSell));
		}
		past.add(current);
		current = new ArrayList<>();
		//
		if (isAlmostZero()) {
			reset();
		}
	}
	void change(Transaction transaction) {
		// Sanity check
		if (!DoubleUtil.isAlmostEqual(this.totalQuantity, -transaction.quantity)) {
			logger.error("change {}  {} -> {}  {}", transaction.symbol, transaction.quantity, transaction.newSymbol, transaction.newQuantity);
			logger.error("Quantity mismatch  {}  {}", this.totalQuantity, transaction.quantity);
			throw new SecuritiesException("Unexpected");
		}
		
		String newSymbol = transaction.newSymbol;
		String newName   = transaction.newName;
		double oldQuantity = -transaction.quantity;
		double newQuantity = transaction.newQuantity;
		
		this.symbol = newSymbol;
		this.name   = newName;

		if (oldQuantity == newQuantity) {
			// no need to update toatlQuantiy
			// no need to update symbol and name in current
		} else {
			// Adjust totalQuantity
			totalQuantity = transaction.newQuantity;

			// no need to update symbol and name in current
		}
		
		Transfer.Buy buy = new Transfer.Buy(
			transaction.date, newSymbol, transaction.newName,
			totalQuantity, totalCost
			);
		current.add(new Transfer(transaction.id, buy));
	}
	
	
	public static Map<String, BuySell>  getBuySellMap(List<Transaction> transactionList) {
		Map<String, BuySell> ret = new TreeMap<>();
		
		for(Transaction transaction: transactionList) {
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
				// another for build positionMap
				Position.buy(transaction);
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
				Position.sell(transaction);
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
				Position.change(transaction.date, transaction.symbol, transaction.quantity, transaction.newSymbol, transaction.newQuantity);
			}
		}
		
		// Add dummy sell record for remaining stocks
		addDummySell(ret);
		return ret;
	}
	
	public static final String DUMMY_DATE = "9999-12-31";
	public static final double DUMMY_FEE  = 3.0;
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
			Position.sell(transaction);
		}
	}

}
