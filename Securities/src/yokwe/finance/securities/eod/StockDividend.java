package yokwe.finance.securities.eod;

import java.time.LocalDate;
import java.util.Map;

public class StockDividend implements Comparable<StockDividend> {
	public static class PayDiv {
		public LocalDate pay;
		public double    quantity;
		public double    amount;
		public double    div;
		
		PayDiv(LocalDate pay, double quantity, double amount, double div) {
			this.pay      = pay;
			this.quantity = quantity;
			this.amount   = amount;
			this.div      = div;
		}
		
		@Override
		public String toString() {
			return String.format("{%s %4.0f %8.5f %6.2f}", pay, quantity, amount, div);
		}
	}

	public String symbol;
	public double quantity;
	public double cost;
	public double div;
	
	//         month 1 .. 12
	public Map<Integer, PayDiv> payDivMap;
	
	public StockDividend(String symbol, double quantity, double cost, double div, Map<Integer, PayDiv> payDivMap) {
		this.symbol    = symbol;
		this.quantity  = quantity;
		this.cost      = cost;
		this.div       = div;
		this.payDivMap = payDivMap;
	}
	
	@Override
	public String toString() {
		return String.format("{%-8s %4.0f %8.2f %8.2f %s}", symbol, quantity, cost, div, payDivMap);
	}

	@Override
	public int compareTo(StockDividend that) {
		return this.symbol.compareTo(that.symbol);
	}
}
