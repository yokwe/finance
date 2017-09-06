package yokwe.finance.securities.eod.tax;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.tax.Transfer;
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
			logger.info("SELL {}", String.format("%s %-8s %9.5f %7d %7d %7d %s %s",
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
			logger.info("SELL*{}", String.format("%s %-8s %9.5f %7d %7d %7d %s %s",
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
}
