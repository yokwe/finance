package yokwe.finance.securities.eod.report;

import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;

@Sheet.SheetName("Account")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class Account extends Sheet {
	@ColumnName("date")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public String date;       // YYYY-MM
	
	// summary
	@ColumnName("Fund Total")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2)
	public double fundTotal;  // total wire or ach money in this account
	
	@ColumnName("Cash Total")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2)
	public double cashTotal;  // cash available
	
	@ColumnName("Stock Total")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2)
	public double stockTotal; // unrealized gain or loss
	
	@ColumnName("Gain Total")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2)
	public double gainTotal;  // cash + stock - fund
	
	// detail of fund
	@ColumnName("WIre In")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double wireIn;     // wired money deposit for this month
	
	@ColumnName("Wire Out")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double wireOut;    // wired money withdraw for this month
	
	@ColumnName("ACH In")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double achIn;      // ACH money deposit for this month
	
	@ColumnName("ACH Out")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double achOut;     // ACH money withdraw for this month

	// detail of cash
	@ColumnName("Interest")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double interest;   // interest for this month
	
	@ColumnName("Dividend")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double dividend;   // dividend for this month

	// detail of stock
	@ColumnName("Buy")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double buy;        // buy for this month
	
	@ColumnName("Sell")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double sell;       // sell for this month
	
	@ColumnName("Sell Cost")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double sellCost;   // sell cost for this month
	
	@ColumnName("Sell Gain")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double sellGain;   // sell gain for this month
	
	
	@Override
	public String toString() {
		return String.format("%s %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f", 
			date, fundTotal, cashTotal, stockTotal, gainTotal, wireIn, wireOut, achIn, achOut, interest, dividend, buy, sell, sellCost, sellGain);
	}
}
