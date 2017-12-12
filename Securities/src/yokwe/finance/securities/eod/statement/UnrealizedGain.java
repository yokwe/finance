package yokwe.finance.securities.eod.statement;

import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;

@Sheet.SheetName("評価損益")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class UnrealizedGain extends Sheet {
	@ColumnName("年月日")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public String date;       // YYYY-MM-DD
	
	// summary
	@ColumnName("資金")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public double fund;  // total wire or ach money in this account
	
	@ColumnName("現金")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public double cash;  // cash available
	
	@ColumnName("損益")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public double realizedGain;  // cash + stock - fund
	
	@ColumnName("株式簿価")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public double stockCost; // unrealized gain or loss
	
	@ColumnName("株式時価")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public double stockValue; // unrealized gain or loss
	
	@ColumnName("株式評価損益")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public double stockUnrealizedGain; // unrealized gain or loss
	
	@ColumnName("評価損益")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public double unrealizedGain; // unrealized gain or loss
	

	public UnrealizedGain(String date, double fund, double cash, double realizedGain,
			double stockCost, double stockValue, double stockUnrealizedGain, double unrealizedGain) {
		this.date                = date;
		this.fund                = fund;
		this.cash                = cash;
		this.realizedGain        = realizedGain;
		
		this.stockCost           = stockCost;
		this.stockValue          = stockValue;
		this.stockUnrealizedGain = stockUnrealizedGain;
		this.unrealizedGain      = unrealizedGain;
	}

	@Override
	public String toString() {
		return String.format("%s %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f", 
			date, fund, cash, realizedGain, stockCost, stockValue, stockUnrealizedGain, unrealizedGain);
	}
}
