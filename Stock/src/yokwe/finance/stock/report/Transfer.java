package yokwe.finance.stock.report;

import yokwe.finance.stock.libreoffice.Sheet;
import yokwe.finance.stock.libreoffice.SpreadSheet;

@Sheet.SheetName("譲渡")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class Transfer extends Sheet {
	@ColumnName("銘柄コード")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public final String symbol;
	
	@ColumnName("買年月日")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public final String buyDate;
	
	@ColumnName("買数量")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public final Double buyQuantity;
	
	@ColumnName("買値")
	@NumberFormat(SpreadSheet.FORMAT_USD5)
	public final Double buyPrice;
	
	@ColumnName("買手数料")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final Double buyFee;

	@ColumnName("取得金額")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final Double buyAmount;
	
	@ColumnName("総数量")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public final Double totalQuantity;

	@ColumnName("総取得金額")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final Double totalAmount;
	
	@ColumnName("平均金額")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final Double averagePrice;
	
	@ColumnName("売年月日")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public final String sellDate;
	
	@ColumnName("売数量")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public final Double sellQuantity;
	
	@ColumnName("売値")
	@NumberFormat(SpreadSheet.FORMAT_USD5)
	public final Double sellPrice;
	
	@ColumnName("売手数料")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final Double sellFee;

	@ColumnName("譲渡金額")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final Double sellAmount;
	
	@ColumnName("取得費")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final Double sellCost;
	
	@ColumnName("利益")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final Double profit;
	
	private Transfer(
			String symbol, String buyDate, Double buyQuantity, Double buyPrice, Double buyFee, Double buyAmount,
			Double totalQuantity, Double totalAmount, Double averagePrice, 
			String sellDate, Double sellQuantity, Double sellPrice, Double sellFee, Double sellAmount, Double sellCost, Double profit) {
		this.symbol = symbol;
		this.buyDate = buyDate;
		this.buyQuantity = buyQuantity;
		this.buyPrice = buyPrice;
		this.buyFee = buyFee;
		this.buyAmount = buyAmount;
		this.totalQuantity = totalQuantity;
		this.totalAmount = totalAmount;
		this.averagePrice = averagePrice;
		this.sellDate = sellDate;
		this.sellQuantity = sellQuantity;
		this.sellPrice = sellPrice;
		this.sellFee = sellFee;
		this.sellAmount = sellAmount;
		this.sellCost = sellCost;
		this.profit = profit;
	}

	public Transfer() {
		this(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
	}
	public static Transfer buy(
		String symbol, String buyDate, Double buyQuantity, Double buyPrice, Double buyFee, Double buyAmount,
		Double totalQuantity, Double totalAmount, Double averagePrice){
		return new Transfer(
			symbol, buyDate, buyQuantity, buyPrice, buyFee, buyAmount,
			totalQuantity, totalAmount, averagePrice,
			null, null, null, null, null, null, null);
	}
	public static Transfer sell(
		String symbol,
		String sellDate, Double sellQuantity, Double sellPrice, Double sellFee, Double sellAmount, Double sellCost, Double profit) {
		return new Transfer(
			symbol, null, null, null, null, null,
			null, null, null,
			sellDate, sellQuantity, sellPrice, sellFee, sellAmount, sellCost, profit);
	}
	public static Transfer buySell(
		String symbol, String buyDate, Double buyQuantity, Double buyPrice, Double buyFee, Double buyAmount,
		Double totalQuantity, Double totalAmount, Double averagePrice, 
		String sellDate, Double sellQuantity, Double sellPrice, Double sellFee, Double sellAmount, Double sellCost, Double profit){
		return new Transfer(
			symbol, buyDate, buyQuantity, buyPrice, buyFee, buyAmount,
			totalQuantity, totalAmount, averagePrice,
			sellDate, sellQuantity, sellPrice, sellFee, sellAmount, sellCost, profit);
	}
}
