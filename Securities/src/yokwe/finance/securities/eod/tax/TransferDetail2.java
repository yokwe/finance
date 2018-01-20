package yokwe.finance.securities.eod.tax;

import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;

@Sheet.SheetName("譲渡明細2")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class TransferDetail2 extends Sheet {
	@ColumnName("銘柄コード")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public final String symbol;
	
	@ColumnName("銘柄")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public final String symbolName;

	@ColumnName("約定日")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public final String date;
	
	@ColumnName("数量")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public final Double quantity;
	
	@ColumnName("単価")
	@NumberFormat(SpreadSheet.FORMAT_USD5)
	public final Double price;
	
	@ColumnName("手数料")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final Double fee;
	
	@ColumnName("為替レート")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2)
	public final Double fxRate;
	
	@ColumnName("譲渡金額")
	@NumberFormat(SpreadSheet.FORMAT_JPY)
	public final Integer sellJPY;
	
	@ColumnName("譲渡手数料")
	@NumberFormat(SpreadSheet.FORMAT_JPY)
	public final Integer feeSellJPY;
	
	@ColumnName("取得費")
	@NumberFormat(SpreadSheet.FORMAT_JPY)
	public final Integer costJPY;
	
	@ColumnName("取得金額")
	@NumberFormat(SpreadSheet.FORMAT_JPY)
	public final Integer buyJPY;
	
	@ColumnName("取得手数料")
	@NumberFormat(SpreadSheet.FORMAT_JPY)
	public final Integer feeBuyJPY;
	
	@ColumnName("総数量")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public final Double totalQuantity;
	
	@ColumnName("総取得費")
	@NumberFormat(SpreadSheet.FORMAT_JPY)
	public final Integer totalCostJPY;
	
	
	public TransferDetail2() {
		this.symbol        = null;
		this.symbolName    = null;
		
		this.date          = null;
		this.quantity      = null;
		this.price         = null;
		this.fee           = null;
		this.fxRate        = null;
		
		this.sellJPY       = null;
		this.feeSellJPY    = null;
		this.costJPY       = null;
		
		this.buyJPY        = null;
		this.feeBuyJPY     = null;
		
		this.totalQuantity = null;
		this.totalCostJPY  = null;
	}
	
	public TransferDetail2(Transfer.Buy buy) {
		this.symbol        = buy.symbol;
		this.symbolName    = buy.name;
		
		this.date          = buy.date;
		
		if (Transaction.roundQuantity(buy.quantity) == 0) {
			// special for change
			this.quantity      = null;
			this.price         = null;
			this.fee           = null;
			this.fxRate        = null;
			
			this.sellJPY       = null;
			this.feeSellJPY    = null;
			this.costJPY       = null;
			
			this.buyJPY        = null;
			this.feeBuyJPY     = null;
			
			this.totalQuantity = buy.totalQuantity;
			this.totalCostJPY  = buy.totalCostJPY;
		} else {
			this.quantity      = buy.quantity;
			this.price         = buy.price;
			this.fee           = buy.fee;
			this.fxRate        = buy.fxRate;
			
			this.sellJPY       = null;
			this.feeSellJPY    = null;
			this.costJPY       = null;
			
			this.buyJPY        = buy.buyJPY;
			this.feeBuyJPY     = buy.feeJPY;
			
			this.totalQuantity = buy.totalQuantity;
			this.totalCostJPY  = buy.totalCostJPY;
		}
	}
	public TransferDetail2(Transfer.Sell sell) {
		this.symbol        = sell.symbol;
		this.symbolName    = sell.name;
		
		this.date          = sell.date;
		this.quantity      = sell.quantity;
		this.price         = sell.price;
		this.fee           = sell.fee;
		this.fxRate        = sell.fxRate;
		
		this.sellJPY       = sell.sellJPY;
		this.feeSellJPY    = sell.feeJPY;
		this.costJPY       = sell.costJPY;
		
		this.buyJPY        = null;
		this.feeBuyJPY     = null;
		
		this.totalQuantity = sell.totalQuantity;
		this.totalCostJPY  = sell.totalCostJPY;
	}
}
