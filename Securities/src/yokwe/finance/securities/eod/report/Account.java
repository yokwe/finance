package yokwe.finance.securities.eod.report;

import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;

@Sheet.SheetName("Account")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class Account extends Sheet implements Comparable<Account> {
	@ColumnName("Date")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public String date;       // YYYY-MM
	
	// summary
	@ColumnName("Fund Total")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2)
	public double fundTotal;  // total wire or ach money in this account
	
	@ColumnName("Cash Total")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2)
	public double cashTotal;  // cash available
	
	@ColumnName("Stock Total")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2)
	public double stockTotal; // unrealized gain or loss
	
	@ColumnName("Gain Total")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2)
	public double gainTotal;  // cash + stock - fund
	
	// detail of fund
	@ColumnName("Wire In")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double wireIn;     // wired money deposit for this month
	
	@ColumnName("Wire Out")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double wireOut;    // wired money withdraw for this month
	
	@ColumnName("ACH In")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double achIn;      // ACH money deposit for this month
	
	@ColumnName("ACH Out")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double achOut;     // ACH money withdraw for this month

	// detail of cash
	@ColumnName("Interest")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double interest;   // interest for this month
	
	@ColumnName("Dividend")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double dividend;   // dividend for this month

	// detail of stock
	@ColumnName("Symbol")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public String symbol;     // symbol of stock
	
	@ColumnName("Buy")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double buy;        // buy for this month
	
	@ColumnName("Sell")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double sell;       // sell for this month
	
	@ColumnName("Sell Cost")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double sellCost;   // sell cost for this month
	
	@ColumnName("Sell Gain")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double sellGain;   // sell gain for this month
	
	@ColumnName("Unreal Gain")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2_BLANK)
	public double unrealGain;   // unrealized gain for this month
		
	
	public Account(String date, double fundTotal, double cashTotal, double stockTotal, double gainTotal,
			double wireIn, double wireOut, double achIn, double achOut, double interest, double dividend,
			String symbol, double buy, double sell, double sellCost, double sellGain, double unrealGain) {
		this.date       = date;
		this.fundTotal  = fundTotal;
		this.cashTotal  = cashTotal;
		this.stockTotal = stockTotal;
		this.gainTotal  = gainTotal;
		
		this.wireIn     = wireIn;
		this.wireOut    = wireOut;
		this.achIn      = achIn;
		this.achOut     = achOut;
		this.interest   = interest;
		this.dividend   = dividend;
		
		this.symbol     = symbol;
		this.buy        = buy;
		this.sell       = sell;
		this.sellCost   = sellCost;
		this.sellGain   = sellGain;
		this.unrealGain = unrealGain;
	}
	public Account() {
		this("", 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0,
				"", 0, 0, 0, 0, 0);
	}
	public Account(Account that) {
		this(new String(that.date), that.fundTotal, that.cashTotal, that.stockTotal, that.gainTotal,
				that.wireIn, that.wireOut, that.achIn, that.achOut, that.interest, that.dividend,
				that.symbol, that.buy, that.sell, that.sellCost, that.sellGain, that.unrealGain);
	}
	@Override
	public String toString() {
		return String.format("%s %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %-10s %9.2f %9.2f %9.2f %9.2f %9.2f", 
			date, fundTotal, cashTotal, stockTotal, gainTotal, wireIn, wireOut, achIn, achOut, interest, dividend, symbol, buy, sell, sellCost, sellGain, unrealGain);
	}
	@Override
	public int compareTo(Account that) {
		if (this.date.equals(that.date)) {
			return this.symbol.compareTo(that.symbol);
		} else {
			return this.date.compareTo(that.date);
		}
	}
}
