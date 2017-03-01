package yokwe.finance.securities.tax;

import yokwe.finance.securities.libreoffice.Sheet;

@Sheet.SheetName("MonthlyStats")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class MonthlyStats extends Sheet {
	@ColumnName("date")
	public String date;     // YYYY-MM
	
	// summary
	@ColumnName("fund")
	public double fund;     // total wire or ach money in this account
	@ColumnName("cash")
	public double cash;     // cash available
	@ColumnName("stock")
	public double stock;    // unrealized gain or loss
	@ColumnName("gain")
	public double gain;     // cash + stock - fund
	
	// detail of fund
	@ColumnName("wire")
	public double wire;     // wired money for this month
	@ColumnName("ach")
	public double ach;      // ACH money for this month

	// detail of cash
	@ColumnName("interest")
	public double interest; // interest for this month
	@ColumnName("dividend")
	public double dividend; // dividend for this month

	// detail of stock
	@ColumnName("buy")
	public double buy;      // buy for this month
	@ColumnName("sell")
	public double sell;     // sell for this month
	@ColumnName("sell cost")
	public double sellCost;     // sell for this month
	
	@Override
	public String toString() {
		return String.format("%s %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f", 
			date, fund, cash, stock, gain, wire, ach, interest, dividend, buy, sell, sellCost);
	}
}
