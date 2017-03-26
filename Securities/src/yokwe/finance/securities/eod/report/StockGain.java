package yokwe.finance.securities.eod.report;

import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;

@Sheet.SheetName("StockGain")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class StockGain extends Sheet implements Comparable<StockGain> {
	@ColumnName("Date")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public String date;       // YYYY-MM
	
	@ColumnName("Stock")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2)
	public double stock;

	@ColumnName("Unreal")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double unreal;

	@ColumnName("Unreal Gain")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double unrealGain;

	@ColumnName("Buy")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double buy;
	
	@ColumnName("Sell")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double sell;
	
	@ColumnName("Sell Gain")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double sellGain;
	
	@ColumnName("Real Gain")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2)
	public double realGain;

	@ColumnName("Total Gain")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double totalGain;

	public StockGain(String date, double stock, double unreal, double unrealGain, double buy, double sell, double sellGain, double realGain, double totalGain) {
		this.date       = date;
		this.stock      = stock;
		this.unreal     = unreal;
		this.unrealGain = unrealGain;
		this.buy        = buy;
		this.sell       = sell;
		this.sellGain   = sellGain;
		this.realGain   = realGain;
		this.totalGain  = totalGain;
	}
	public StockGain() {
		this("", 0, 0, 0, 0, 0, 0, 0, 0);
	}
	public StockGain(String date) {
		this(date, 0, 0, 0, 0, 0, 0, 0, 0);
	}
	public StockGain(StockGain that) {
		this.date       = that.date;
		this.stock      = that.stock;
		this.unreal     = that.unreal;
		this.unrealGain = that.unrealGain;
		this.buy        = that.buy;
		this.sell       = that.sell;
		this.sellGain   = that.sellGain;
		this.realGain   = that.realGain;
		this.totalGain  = that.totalGain;
	}
	
	@Override
	public String toString() {
		return String.format("%s %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f", date, stock, unreal, unrealGain, buy, sell, sellGain, realGain, totalGain);
	}
	@Override
	public int compareTo(StockGain that) {
		return this.date.compareTo(that.date);
	}
}
