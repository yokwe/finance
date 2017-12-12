package yokwe.finance.securities.eod.statement;

import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;

@Sheet.SheetName("譲渡概要")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class TransferSummary extends Sheet {
	@ColumnName("売受渡日")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public final String dateSell;

	@ColumnName("銘柄コード")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public final String symbol;
	
	@ColumnName("銘柄")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public final String name;
	
	@ColumnName("数量")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public final double quantity;

	@ColumnName("譲渡金額")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final double sell;
	
	@ColumnName("取得費")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final double cost;
	
	@NumberFormat(SpreadSheet.FORMAT_USD)
	@ColumnName("損益")
	public final double profitLoss;
	
	
	public TransferSummary(Transfer.Sell sell) {
		this.dateSell     = sell.date;
		
		this.symbol       = sell.symbol;
		this.name         = sell.name;
		this.quantity     = sell.quantity;
		
		this.sell         = sell.sell;
		this.cost         = sell.cost;
		this.profitLoss   = sell.sell - sell.cost;
	}
}
