package yokwe.finance.securities.eod.stockHistory;

import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;

@Sheet.SheetName("StockHistory")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class StockHistorySheet extends Sheet {
	@ColumnName("symbol")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public final String symbol;
	
	@ColumnName("date")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public final String date;
	
	@ColumnName("quantity")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public final Double quantity;
	
	@ColumnName("cost")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final Double cost;
	
	@ColumnName("value")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final Double value;
	
	@ColumnName("dividend")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final Double dividend;
	
	@ColumnName("profit")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final Double profit;
	
	StockHistorySheet(String symbol, String date, Double quantity, Double cost, Double value, Double dividend, Double profit) {
		this.symbol   = symbol;
		this.date     = date;
		this.quantity = quantity;
		this.cost     = cost;
		this.value    = value;
		this.dividend = dividend;
		this.profit   = profit;
	}
	StockHistorySheet() {
		this(null, null, null, null, null, null, null);
	}
}
