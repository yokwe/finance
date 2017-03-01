package yokwe.finance.securities.tax;

import yokwe.finance.securities.libreoffice.Sheet;

@Sheet.SheetName("実績")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class Perf extends Sheet {
	@ColumnName("年月")
	public String date;     // YYYY-MM
	
	// summary
	@ColumnName("資金")
	public double fund;     // total wire or ach money in this account
	@ColumnName("現金")
	public double cash;     // cash available
	@ColumnName("株式")
	public double stock;    // unrealized gain or loss
	@ColumnName("損益")
	public double gain;     // cash + stock - fund
	
	// detail of fund
	@ColumnName("送金")
	public double wire;     // wired money for this month
	@ColumnName("ACH")
	public double ach;      // ACH money for this month

	// detail of cash
	@ColumnName("利子")
	public double interest; // interest for this month
	@ColumnName("配当")
	public double dividend; // dividend for this month

	// detail of stock
	@ColumnName("購入")
	public double buy;      // buy for this month
	@ColumnName("売却")
	public double sell;     // sell for this month
	@ColumnName("売却原価")
	public double sellCost;     // sell for this month
	
	@Override
	public String toString() {
		return String.format("%s %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f", 
			date, fund, cash, stock, gain, wire, ach, interest, dividend, buy, sell, sellCost);
	}
}
