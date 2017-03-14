package yokwe.finance.securities.tax;

import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;

@Sheet.SheetName("口座")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class Account extends Sheet {
	@ColumnName("年月")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public String date;       // YYYY-MM
	
	// summary
	@ColumnName("資金累計")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2)
	public double fundTotal;  // total wire or ach money in this account
	
	@ColumnName("現金累計")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2)
	public double cashTotal;  // cash available
	
	@ColumnName("株式累計")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2)
	public double stockTotal; // unrealized gain or loss
	
	@ColumnName("損益累計")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2)
	public double gainTotal;  // cash + stock - fund
	
	// detail of fund
	@ColumnName("送金入金")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double wireIn;     // wired money deposit for this month
	
	@ColumnName("送金出金")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double wireOut;    // wired money withdraw for this month
	
	@ColumnName("ACH入金")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double achIn;      // ACH money deposit for this month
	
	@ColumnName("ACH出金")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double achOut;     // ACH money withdraw for this month

	// detail of cash
	@ColumnName("利子")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double interest;   // interest for this month
	
	@ColumnName("配当")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double dividend;   // dividend for this month

	// detail of stock
	@ColumnName("購入")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double buy;        // buy for this month
	
	@ColumnName("売却")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double sell;       // sell for this month
	
	@ColumnName("売却原価")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double sellCost;   // sell cost for this month
	
	@ColumnName("売却損益")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double sellGain;   // sell gain for this month
	
	@Override
	public String toString() {
		return String.format("%s %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f", 
			date, fundTotal, cashTotal, stockTotal, gainTotal, wireIn, wireOut, achIn, achOut, interest, dividend, buy, sell, sellCost, sellGain);
	}
}
