package yokwe.finance.securities.eod.statement;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;
import yokwe.finance.securities.util.DoubleUtil;

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

	@ColumnName("売受渡日")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public final String dateSell;
	
	@ColumnName("売数量")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public final Double quantitySell;
	
	@ColumnName("売値")
	@NumberFormat(SpreadSheet.FORMAT_USD5)
	public final Double priceSell;
	
	@ColumnName("譲渡金額")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final Double sell;
	
	@ColumnName("取得費")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final Double cost;

	@NumberFormat(SpreadSheet.FORMAT_USD)
	@ColumnName("損益")
	public final Double profitLoss;

	@ColumnName("買受渡日")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public final String dateBuy;
	
	@ColumnName("買数量")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public final Double quantityBuy;
	
	@ColumnName("買値")
	@NumberFormat(SpreadSheet.FORMAT_USD5)
	public final Double priceBuy;
	
	@ColumnName("取得価格")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final Double buy;
	
	@ColumnName("総数量")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public final Double totalQuantity;
	
	@ColumnName("総取得価格")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final Double totalCost;
	
	
	public TransferDetail(Transfer.Buy buy) {
		this.symbol        = buy.symbol;
		this.symbolName    = buy.name;
		
		this.dateSell      = null;
		this.quantitySell  = null;
		this.priceSell     = null;
		this.sell          = null;
		this.cost          = null;
		this.profitLoss    = null;

		this.dateBuy       = buy.date;
		if (buy.quantity == 0) {
			this.quantityBuy   = null;
			this.priceBuy      = null;
			this.buy           = null;
		} else {
			this.quantityBuy   = buy.quantity;
			this.priceBuy      = buy.price;
			this.buy           = buy.buy;
		}
		this.totalQuantity = buy.totalQuantity;
		this.totalCost     = buy.totalCost;
	}
	public TransferDetail(Transfer.Sell sell) {
		this.symbol        = sell.symbol;
		this.symbolName    = sell.name;
		
		this.dateSell      = sell.date;
		this.quantitySell  = sell.quantity;
		this.priceSell     = sell.price;
		this.sell          = sell.sell;
		this.cost          = sell.cost;
		this.profitLoss    = DoubleUtil.roundPrice(this.sell - this.cost);

		this.dateBuy       = null;
		this.quantityBuy   = null;
		this.priceBuy      = null;
		this.buy           = null;
		// Output blank if totalQuantity is almost zero
		if (sell.totalQuantity < 0.0001) {
			this.totalQuantity = null;
			this.totalCost     = null;
		} else {
			this.totalQuantity = sell.totalQuantity;
			this.totalCost     = sell.totalCost;
		}
	}
	public TransferDetail(Transfer.Buy  buy, Transfer.Sell  sell) {
		this.symbol        = sell.symbol;
		this.symbolName    = sell.name;
		
		this.dateSell      = sell.date;
		this.quantitySell  = sell.quantity;
		this.priceSell     = sell.price;
		this.sell          = sell.sell;
		this.cost          = sell.cost;
		this.profitLoss    = DoubleUtil.roundPrice(this.sell - this.cost);

		this.dateBuy       = buy.date;
		this.quantityBuy   = buy.quantity;
		this.priceBuy      = buy.price;
		this.buy           = buy.buy;
		
		if (sell.totalQuantity < 0.0001) {
			this.totalQuantity = null;
			this.totalCost     = null;
		} else {
			this.totalQuantity = sell.totalQuantity;
			this.totalCost     = sell.totalCost;
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
