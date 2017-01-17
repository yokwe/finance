package yokwe.finance.securities.tax;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.libreoffice.Sheet;

@Sheet.SheetName("譲渡明細")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class TransferDetail extends Sheet {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Report.class);
	
	@ColumnName("銘柄コード")
	public final String symbol;
	@ColumnName("銘柄")
	public final String symbolName;
	@ColumnName("数量")
	public final double quantity;

	@ColumnName("売約定日")
	public final String dateSell;
	@ColumnName("売値")
	public final String priceSell;
	@ColumnName("売手数料")
	public final String feeSell;
	@ColumnName("売レート")
	public final String fxRateSell;
	@ColumnName("譲渡金額")
	public final String sellJPY;
	@ColumnName("取得費")
	public final String costJPY;
	@ColumnName("譲渡手数料")
	public final String feeSellJPY;
	@ColumnName("取得日最初")
	public final String dateBuyFirst;
	@ColumnName("取得日最後")
	public final String dateBuyLast;
		
	@ColumnName("買約定日")
	public final String dateBuy;
	@ColumnName("買値")
	public final String priceBuy;
	@ColumnName("買手数料")
	public final String feeBuy;
	@ColumnName("買レート")
	public final String fxRateBuy;
	@ColumnName("取得価格")
	public final String buyJPY;
	@ColumnName("総数量")
	public final String totalQuantity;
	@ColumnName("総取得価格")
	public final String totalCostJPY;
	
	private TransferDetail(
			String symbol, String symbolName, double quantity,
			String dateSell, String priceSell, String feeSell, String fxRateSell, String feeSellJPY, String sellJPY, String costJPY, String dateBuyFirst, String dateBuyLast,
			String dateBuy, String priceBuy, String feeBuy, String fxRateBuy, String buyJPY, String totalQuantity, String totalCostJPY
			) {
		this.symbol        = symbol;
		this.symbolName    = symbolName;
		this.quantity      = quantity;
		
		this.dateSell      = dateSell;
		this.priceSell     = priceSell;
		this.feeSell       = feeSell;
		this.fxRateSell    = fxRateSell;
		this.feeSellJPY    = feeSellJPY;
		this.sellJPY       = sellJPY;
		this.costJPY       = costJPY;
		this.dateBuyFirst  = dateBuyFirst;
		this.dateBuyLast   = dateBuyLast;

		this.dateBuy       = dateBuy;
		this.priceBuy      = priceBuy;
		this.feeBuy        = feeBuy;
		this.fxRateBuy     = fxRateBuy;
		this.buyJPY        = buyJPY;
		this.totalQuantity = totalQuantity;
		this.totalCostJPY  = totalCostJPY;
	}
	
	public TransferDetail(Transfer.Buy buy) {
		this.symbol        = buy.symbol;
		this.symbolName    = buy.name;
		this.quantity      = buy.quantity;
		
		this.dateSell      = "";
		this.priceSell     = "";
		this.feeSell       = "";
		this.fxRateSell    = "";
		this.feeSellJPY    = "";
		this.sellJPY       = "";
		this.costJPY       = "";
		this.dateBuyFirst  = "";
		this.dateBuyLast   = "";

		this.dateBuy       = buy.date;
		this.priceBuy      = String.format("%.5f", buy.price);
		this.feeBuy        = String.format("%.2f", buy.fee);
		this.fxRateBuy     = String.format("%.2f", buy.fxRate);
		this.buyJPY        = String.format("%d",   buy.buyJPY + buy.feeJPY);
		this.totalQuantity = String.format("%.5f", buy.totalQuantity);
		this.totalCostJPY  = String.format("%d",   buy.totalCostJPY);
	}
	public TransferDetail(Transfer.Sell sell) {
		this.symbol        = sell.symbol;
		this.symbolName    = sell.name;
		this.quantity      = sell.quantity;
		
		this.dateSell      = sell.date;
		this.priceSell     = String.format("%.5f", sell.price);
		this.feeSell       = String.format("%.2f", sell.fee);
		this.fxRateSell    = String.format("%.2f", sell.fxRate);
		this.feeSellJPY    = String.format("%d",   sell.feeJPY);
		this.sellJPY       = String.format("%d",   sell.sellJPY);
		this.costJPY       = String.format("%d",   sell.costJPY);
		this.dateBuyFirst  = sell.dateFirst; 
		this.dateBuyLast   = sell.dateLast;

		this.dateBuy       = "";
		this.priceBuy      = "";
		this.feeBuy        = "";
		this.fxRateBuy     = "";
		this.buyJPY        = "";
		// Output blank if totalQuantity is almost zero
		if (sell.totalQuantity < 0.0001) {
			this.totalQuantity = "";
			this.totalCostJPY  = "";
		} else {
			this.totalQuantity = String.format("%.5f", sell.totalQuantity);
			this.totalCostJPY  = String.format("%d",   sell.totalCostJPY);
		}
	}
	public TransferDetail(Transfer.Buy  buy, Transfer.Sell  sell) {
		this.symbol        = sell.symbol;
		this.symbolName    = sell.name;
		this.quantity      = sell.quantity;
		
		this.dateSell      = sell.date;
		this.priceSell     = String.format("%.5f", sell.price);
		this.feeSell       = String.format("%.2f", sell.fee);
		this.fxRateSell    = String.format("%.2f", sell.fxRate);
		this.feeSellJPY    = String.format("%d",   sell.feeJPY);
		this.sellJPY       = String.format("%d",   sell.sellJPY);
		this.costJPY       = String.format("%d",   sell.costJPY);
		this.dateBuyFirst  = sell.dateFirst;
		this.dateBuyLast   = sell.dateLast;

		this.dateBuy       = buy.date;
		this.priceBuy      = String.format("%.5f", buy.price);
		this.feeBuy        = String.format("%.2f", buy.fee);
		this.fxRateBuy     = String.format("%.2f", buy.fxRate);
		this.buyJPY        = String.format("%d",   buy.buyJPY + buy.feeJPY);
		
		if (sell.totalQuantity < 0.0001) {
			this.totalQuantity = "";
			this.totalCostJPY  = "";
		} else {
			this.totalQuantity = String.format("%.5f", sell.totalQuantity);
			this.totalCostJPY  = String.format("%d",   sell.totalCostJPY);
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
