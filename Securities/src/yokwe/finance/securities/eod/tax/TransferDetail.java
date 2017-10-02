package yokwe.finance.securities.eod.tax;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;

@Sheet.SheetName("譲渡明細")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class TransferDetail extends Sheet {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Report.class);
	
	@ColumnName("銘柄コード")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public final String symbol;
	
	@ColumnName("銘柄")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public final String symbolName;

	@ColumnName("売約定日")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public final String dateSell;
	
	@ColumnName("売数量")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public final Double quantitySell;
	
	@ColumnName("売値")
	@NumberFormat(SpreadSheet.FORMAT_USD5)
	public final Double priceSell;
	
	@ColumnName("売手数料")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final Double feeSell;
	
	@ColumnName("売レート")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2)
	public final Double fxRateSell;
	
	@ColumnName("譲渡金額")
	@NumberFormat(SpreadSheet.FORMAT_JPY)
	public final Integer sellJPY;
	
	@ColumnName("取得費")
	@NumberFormat(SpreadSheet.FORMAT_JPY)
	public final Integer costJPY;
	
	@ColumnName("譲渡手数料")
	@NumberFormat(SpreadSheet.FORMAT_JPY)
	public final Integer feeSellJPY;
	
	@ColumnName("取得日最初")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public final String dateBuyFirst;
	
	@ColumnName("取得日最後")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public final String dateBuyLast;
		
	@ColumnName("買約定日")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public final String dateBuy;
	
	@ColumnName("買数量")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public final Double quantityBuy;
	
	@ColumnName("買値")
	@NumberFormat(SpreadSheet.FORMAT_USD5)
	public final Double priceBuy;
	
	@ColumnName("買手数料")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final Double feeBuy;
	
	@ColumnName("買レート")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2)
	public final Double fxRateBuy;
	
	@ColumnName("取得価格")
	@NumberFormat(SpreadSheet.FORMAT_JPY)
	public final Integer buyJPY;
	
	@ColumnName("総数量")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public final Double totalQuantity;
	
	@ColumnName("総取得価格")
	@NumberFormat(SpreadSheet.FORMAT_JPY)
	public final Integer totalCostJPY;
	
	
	public TransferDetail(Transfer.Buy buy) {
		this.symbol        = buy.symbol;
		this.symbolName    = buy.name;
		
		this.dateSell      = null;
		this.quantitySell  = null;
		this.priceSell     = null;
		this.feeSell       = null;
		this.fxRateSell    = null;
		this.feeSellJPY    = null;
		this.sellJPY       = null;
		this.costJPY       = null;
		this.dateBuyFirst  = null;
		this.dateBuyLast   = null;

		this.dateBuy       = buy.date;
		this.quantityBuy   = buy.quantity;
		this.priceBuy      = buy.price;
		this.feeBuy        = buy.fee;
		this.fxRateBuy     = buy.fxRate;
		this.buyJPY        = buy.buyJPY + buy.feeJPY;
		this.totalQuantity = buy.totalQuantity;
		this.totalCostJPY  = buy.totalCostJPY;
	}
	public TransferDetail(Transfer.Sell sell) {
		this.symbol        = sell.symbol;
		this.symbolName    = sell.name;
		
		this.dateSell      = sell.date;
		this.quantitySell  = sell.quantity;
		this.priceSell     = sell.price;
		this.feeSell       = sell.fee;
		this.fxRateSell    = sell.fxRate;
		this.feeSellJPY    = sell.feeJPY;
		this.sellJPY       = sell.sellJPY;
		this.costJPY       = sell.costJPY;
		this.dateBuyFirst  = sell.dateFirst; 
		this.dateBuyLast   = sell.dateLast;

		this.dateBuy       = null;
		this.quantityBuy   = null;
		this.priceBuy      = null;
		this.feeBuy        = null;
		this.fxRateBuy     = null;
		this.buyJPY        = null;
		// Output blank if totalQuantity is almost zero
		if (sell.totalQuantity < 0.0001) {
			this.totalQuantity = null;
			this.totalCostJPY  = null;
		} else {
			this.totalQuantity = sell.totalQuantity;
			this.totalCostJPY  = sell.totalCostJPY;
		}
	}
	public TransferDetail(Transfer.Buy  buy, Transfer.Sell  sell) {
		this.symbol        = sell.symbol;
		this.symbolName    = sell.name;
		
		this.dateSell      = sell.date;
		this.quantitySell  = sell.quantity;
		this.priceSell     = sell.price;
		this.feeSell       = sell.fee;
		this.fxRateSell    = sell.fxRate;
		this.feeSellJPY    = sell.feeJPY;
		this.sellJPY       = sell.sellJPY;
		this.costJPY       = sell.costJPY;
		this.dateBuyFirst  = sell.dateFirst;
		this.dateBuyLast   = sell.dateLast;

		this.dateBuy       = buy.date;
		this.quantityBuy   = buy.quantity;
		this.priceBuy      = buy.price;
		this.feeBuy        = buy.fee;
		this.fxRateBuy     = buy.fxRate;
		this.buyJPY        = buy.buyJPY + buy.feeJPY;
		
		if (sell.totalQuantity < 0.0001) {
			this.totalQuantity = null;
			this.totalCostJPY  = null;
		} else {
			this.totalQuantity = sell.totalQuantity;
			this.totalCostJPY  = sell.totalCostJPY;
		}
	}
	
	public static TransferDetail getInstance(Transfer transfer) {
		Transfer.Buy  buy  = transfer.buy;
		Transfer.Sell sell = transfer.sell;
		
		if (buy != null && sell == null) {
			return new TransferDetail(buy);
		} else if (buy == null && sell != null){
			return new TransferDetail(sell);
		} else if (buy != null && sell != null) {
			return new TransferDetail(buy, sell);
		} else {
			logger.error("Unexpected");
			throw new SecuritiesException("Unexpected");
		}
	}
}
