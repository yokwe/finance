package yokwe.finance.securities.eod.report;

import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;

@Sheet.SheetName("Transaction")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class Activity extends Sheet implements Comparable<Activity> {
	@ColumnName("YYYY-MM")
	public String yyyyMM;
	
	@ColumnName("Page")
	public String page;
	
	@ColumnName("Transaction")
	public String transaction;
	
	@ColumnName("Date")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public String date;
	
	@ColumnName("TradeDate")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public String tradeDate;
	
	@ColumnName("Symbol")
	public String symbol;
	
	@ColumnName("Name")
	public String name;
	
	@ColumnName("Quantity")
	public double quantity;
	
	@ColumnName("Price")
	public double price;
	
	@ColumnName("Commission")
	public double commission;
	
	@ColumnName("Debit")
	public double debit;
	
	@ColumnName("Credit")
	public double credit;
	
	@Override
	public String toString() {
		return String.format("%s %s %-8s %s %s %-8s %-60s %7.2f %8.4f %5.2f %8.2f %8.2f",
				yyyyMM, page, transaction, date, tradeDate, symbol, name, quantity, price, commission, debit, credit);
	}

	@Override
	public int compareTo(Activity that) {
		String thisDate = (this.tradeDate != null && this.tradeDate.length() != 0) ? this.tradeDate : this.date;
		String thatDate = (that.tradeDate != null && that.tradeDate.length() != 0) ? that.tradeDate : that.date;
		return thisDate.compareTo(thatDate);
	}
}
