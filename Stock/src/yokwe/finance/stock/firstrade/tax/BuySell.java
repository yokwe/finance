package yokwe.finance.stock.firstrade.tax;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.stock.UnexpectedException;
import yokwe.finance.stock.firstrade.Transaction;
import yokwe.finance.stock.util.DoubleUtil;

public class BuySell {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(BuySell.class);

	public String symbol;
	public String name;

	public int    buyCount;
	public String dateBuyFirst;
	public String dateBuyLast;

	public double totalQuantity;
	public double totalCost;
	public int    totalCostJPY;
	
	public List<Transfer>       current;
	public List<List<Transfer>> past;
	
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

	void buy(Transaction transaction) {
		double fxRate = transaction.fxRate;
		buyCount++;
		if (buyCount == 1) {
			dateBuyFirst = transaction.date;
		} else {
			dateBuyLast  = transaction.date;
		}
		
		// maintain totalQuantity, totalAcquisitionCost and totalAcquisitionCostJPY
		double costPrice = DoubleUtil.roundPrice(transaction.quantity * transaction.price);
		double costFee   = transaction.fee;
		double cost      = DoubleUtil.roundPrice(costPrice + costFee);
		int    costJPY   = (int)Math.floor(costPrice * fxRate) + (int)Math.floor(costFee * fxRate);
		
		totalQuantity = DoubleUtil.roundQuantity(totalQuantity + transaction.quantity);
		totalCost     = DoubleUtil.roundPrice(totalCost + cost);
		totalCostJPY  = totalCostJPY + costJPY;

		Transfer.Buy buy = new Transfer.Buy(
			transaction.date, transaction.symbol, transaction.name,
			transaction.quantity, transaction.price, transaction.fee, fxRate,
			totalQuantity, totalCost, totalCostJPY
			);
		current.add(new Transfer(transaction.id, buy));
	}
	void sell(Transaction transaction) {
		double fxRate  = transaction.fxRate;
		double sell    = DoubleUtil.roundPrice(transaction.price * transaction.quantity);
		int    sellJPY = (int)Math.floor(sell * fxRate);
		int    feeJPY  = (int)Math.floor(transaction.fee * fxRate);

		double sellRatio = transaction.quantity / totalQuantity;
		double cost      = DoubleUtil.roundPrice(totalCost * sellRatio);
		int    costJPY;
		
		if (buyCount == 1) {
			costJPY = (int)Math.floor(totalCostJPY * sellRatio);
			
			// maintain totalQuantity, totalAcquisitionCost and totalAcquisitionCostJPY
			totalQuantity = DoubleUtil.roundQuantity(totalQuantity - transaction.quantity);
			totalCost     = DoubleUtil.roundPrice(totalCost - cost);
			totalCostJPY  = totalCostJPY - costJPY;
			
			// date symbol name sellAmountJPY asquisionCostJPY sellCommisionJPY dateBuyFirst dateBuyLast
			logger.info("SELL {}", String.format("%s %-9s %9.5f %7d %7d %7d %s %s",
					transaction.date, symbol, totalQuantity, sellJPY, costJPY, feeJPY, dateBuyFirst, dateBuyLast));
		} else {
			double unitCostJPY = Math.ceil(totalCostJPY / totalQuantity); // need to be round up. See https://www.nta.go.jp/taxanswer/shotoku/1466.htm
			costJPY = (int)Math.floor(unitCostJPY * transaction.quantity);
			
			// maintain totalQuantity, totalAcquisitionCost and totalAcquisitionCostJPY
			totalQuantity = DoubleUtil.roundQuantity(totalQuantity - transaction.quantity);
			totalCost     = DoubleUtil.roundPrice(totalCost - cost);
			totalCostJPY  = (int)Math.floor(unitCostJPY * totalQuantity); // totalCostJPY is calculated with unitCostJPY
			
			// date symbol name sellAmountJPY asquisionCostJPY sellCommisionJPY dateBuyFirst dateBuyLast
			logger.info("SELL*{}", String.format("%s %-9s %9.5f %7d %7d %7d %s %s",
					transaction.date, symbol, totalQuantity, sellJPY, totalCostJPY, feeJPY, dateBuyFirst, dateBuyLast));
		}

		Transfer.Sell transferSell = new Transfer.Sell(
			transaction.date, transaction.symbol, transaction.name,
			transaction.quantity, transaction.price, transaction.fee, fxRate,
			cost, costJPY,
			dateBuyFirst, dateBuyLast,
			totalQuantity, totalCost, totalCostJPY
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
			logger.error("Quantity mismatch  {}  {}", this.totalQuantity, transaction.quantity);
			throw new UnexpectedException("Unexpected");
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
			totalQuantity, totalCost, totalCostJPY
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
				Position.buy(transaction.date, transaction.symbol, transaction.quantity);
			}
			if (transaction.type == Transaction.Type.SELL) {
				String key = transaction.symbol;
				BuySell buySell;
				if (ret.containsKey(key)) {
					buySell = ret.get(key);
				} else {
					logger.error("Unknonw symbol {}", key);
					throw new UnexpectedException("Unexpected");
				}
				
				buySell.sell(transaction);
				Position.sell(transaction.date, transaction.symbol, transaction.quantity);
			}
			if (transaction.type == Transaction.Type.CHANGE) {
				String key = transaction.symbol;
				BuySell buySell;
				if (ret.containsKey(key)) {
					buySell = ret.get(key);
				} else {
					logger.error("Unknonw symbol {}", key);
					throw new UnexpectedException("Unexpected");
				}
				ret.remove(key);
				
				buySell.change(transaction);
				ret.put(buySell.symbol, buySell);
				Position.change(transaction.date, transaction.symbol, transaction.quantity, transaction.newSymbol, transaction.newQuantity);
			}
		}
		
		return ret;
	}

}
