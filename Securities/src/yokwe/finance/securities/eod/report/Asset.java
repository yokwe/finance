package yokwe.finance.securities.eod.report;

import java.util.List;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.DoubleUtil;

public class Asset {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Asset.class);
	
	public static final int DIGIT_QUANTITY = 4;
	public static final int DIGIT_COST     = 2;
	
	public static class Buy {
		public final String date;
		public final String symbol;
		public final double quantity;
		public final double cost;
		public final double fee;
		
		public double quantityRemain;
		public double costRemain;
		
		public Buy(String date, String symbol, double quantity, double cost, double fee) {
			this.date     = date;
			this.symbol   = symbol;
			this.quantity = quantity;
			this.cost     = cost;
			this.fee      = fee;
		}
	}
	
	public String    symbol;
	public double    quantityTotal;
	public double    costTotal;
	public List<Buy> buyList = null;
	
	public Asset(String symbol) {
		this.symbol = symbol;
	}
	
	public void clear() {
		quantityTotal = 0;
		costTotal     = 0;
		buyList.clear();
	}
	protected void update() {
		quantityTotal = 0;
		costTotal     = 0;
		for(Buy buy: buyList) {
			if (DoubleUtil.isAlmostZero(buy.quantityRemain)) {
				buy.quantityRemain = 0;
				buy.costRemain     = 0;
			} else {
				quantityTotal = DoubleUtil.round(quantityTotal + buy.quantityRemain, DIGIT_QUANTITY);
				costTotal     = DoubleUtil.round(costTotal     + buy.costRemain,     DIGIT_COST);
			}
		}
	}
	
	public void buy(String date, String symbol, double quantity, double cost, double fee) {
		buyList.add(new Buy(date, symbol, quantity, cost, fee));
		update();
	}
	public double sell(String date, String symbol, double quantity, double cost, double fee) {
		double costTotalOld = costTotal;
		for(Buy buy: buyList) {
			if (quantity == 0) break;
			
			if (DoubleUtil.isAlmostZero(buy.quantityRemain)) {
				continue;
			} else if (DoubleUtil.isAlmostEqual(buy.quantityRemain, quantity)) {
				buy.quantityRemain = 0;
				buy.costRemain     = 0;
				
				quantity = 0;
				
				cost = cost + buy.costRemain;
			} else if (buy.quantityRemain < quantity) {
				quantity = DoubleUtil.round(quantity - buy.quantityRemain, DIGIT_QUANTITY);
				
				buy.quantityRemain = 0;
				buy.costRemain     = 0;
				
				cost = cost + buy.costRemain;
			} else if (quantity < buy.quantityRemain) {				
				buy.quantityRemain = DoubleUtil.round(buy.quantityRemain - quantity, DIGIT_QUANTITY);
				
				double ratio = quantity / buy.quantityRemain;
				double costMinus = DoubleUtil.round(buy.costRemain * ratio, DIGIT_COST);
				
				buy.costRemain = DoubleUtil.round(buy.costRemain - costMinus, DIGIT_COST);
				
				quantity = 0;
				cost = DoubleUtil.round(cost + costMinus, DIGIT_COST);
			} else {
				logger.error("Unexpected.  {}  {}", quantity, buy.quantityRemain);
				throw new SecuritiesException("Unexpected");
			}
		}
		update();
		double costTotalNew = costTotal;
		
		return costTotalOld - costTotalNew;
	}
	
	public void change(String date, String symbol, double quantity, String symbolNew, double quantityNew) {
		if (DoubleUtil.isAlmostEqual(this.quantityTotal, quantity)) {
			// Fix symbol
			this.symbol = symbol;
			
			// Fix quantityRemain in buyList
			for(Buy buy: buyList) {
				if (DoubleUtil.isAlmostZero(buy.quantityRemain)) {
					buy.quantityRemain = 0;
					buy.costRemain     = 0;
				} else {
					double ratio = quantityNew / quantity;
					buy.quantityRemain = DoubleUtil.round(buy.quantityRemain * ratio, DIGIT_QUANTITY);
				}
			}
		} else {
			logger.error("quantityTotal mismatch.  {}  {}", this.quantityTotal, quantity);
			throw new SecuritiesException("Unexpected");
		}
		update();
	}
}