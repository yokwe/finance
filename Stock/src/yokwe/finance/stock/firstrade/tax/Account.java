package yokwe.finance.stock.firstrade.tax;

import yokwe.finance.stock.libreoffice.Sheet;
import yokwe.finance.stock.libreoffice.SpreadSheet;

@Sheet.SheetName("口座")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class Account extends Sheet implements Comparable<Account> {
	@ColumnName("年月日")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public String date;       // YYYY-MM-DD
	
	// summary
	@ColumnName("資金累計")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public double fundTotal;  // total wire or ach money in this account
	
	@ColumnName("現金累計")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public double cashTotal;  // cash available
	
	@ColumnName("株式累計")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public double stockTotal; // unrealized gain or loss
	
	@ColumnName("損益累計")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public double gainTotal;  // cash + stock - fund
	
	// detail of fund
	@ColumnName("送金入金")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public double wireIn;     // wired money deposit for this month
	
	@ColumnName("送金出金")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public double wireOut;    // wired money withdraw for this month
	
	@ColumnName("ACH入金")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public double achIn;      // ACH money deposit for this month
	
	@ColumnName("ACH出金")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public double achOut;     // ACH money withdraw for this month

	// detail of cash
	@ColumnName("利息")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public double interest;   // interest for this month
	
	@ColumnName("配当")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public double dividend;   // dividend for this month

	// detail of stock
	@ColumnName("銘柄コード")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public String symbol;     // symbol of stock
	
	@ColumnName("購入")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public double buy;        // buy for this month
	
	@ColumnName("売却")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public double sell;       // sell for this month
	
	@ColumnName("売却原価")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public double sellCost;   // sell cost for this month
	
	@ColumnName("売却損益")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public double sellGain;   // sell gain for this month
	

	private Account(String date, double fundTotal, double cashTotal, double stockTotal, double gainTotal,
			double wireIn, double wireOut, double achIn, double achOut, double interest, double dividend,
			String symbol, double buy, double sell, double sellCost, double sellGain) {
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
	}
	
	private static Account nonStock(String date, double fundTotal, double cashTotal, double stockTotal, double gainTotal,
			double wireIn, double wireOut, double achIn, double achOut, double interest) {
		return new Account(date, fundTotal, cashTotal, stockTotal, gainTotal,
				wireIn, wireOut, achIn, achOut, interest, 0,
				"", 0, 0, 0, 0);
	}
	private static Account stock(String date, double fundTotal, double cashTotal, double stockTotal, double gainTotal,
			String symbol, double dividend, double buy, double sell, double sellCost, double sellGain) {
		return new Account(date, fundTotal, cashTotal, stockTotal, gainTotal,
				0, 0, 0, 0, 0, dividend,
				symbol, buy, sell, sellCost, sellGain);
	}
	
	public static Account wireIn(String date, double fundTotal, double cashTotal, double stockTotal, double gainTotal,
			double wireIn) {
		return nonStock(date, fundTotal, cashTotal, stockTotal, gainTotal,
				wireIn, 0, 0, 0, 0);
	}
	public static Account wireOut(String date, double fundTotal, double cashTotal, double stockTotal, double gainTotal,
			double wireOut) {
		return nonStock(date, fundTotal, cashTotal, stockTotal, gainTotal,
				0, wireOut, 0, 0, 0);
	}
	public static Account achIn(String date, double fundTotal, double cashTotal, double stockTotal, double gainTotal,
			double achIn) {
		return nonStock(date, fundTotal, cashTotal, stockTotal, gainTotal,
				0, 0, achIn, 0, 0);
	}
	public static Account achOut(String date, double fundTotal, double cashTotal, double stockTotal, double gainTotal,
			double achOut) {
		return nonStock(date, fundTotal, cashTotal, stockTotal, gainTotal,
				0, 0, 0, achOut, 0);
	}
	public static Account interest(String date, double fundTotal, double cashTotal, double stockTotal, double gainTotal,
			double interest) {
		return nonStock(date, fundTotal, cashTotal, stockTotal, gainTotal,
				0, 0, 0, 0, interest);
	}
	
	public static Account dividend(String date, double fundTotal, double cashTotal, double stockTotal, double gainTotal,
			String symbol, double dividend) {
		return stock(date, fundTotal, cashTotal, stockTotal, gainTotal,
				symbol, dividend, 0, 0, 0, 0);
	}
	public static Account buy(String date, double fundTotal, double cashTotal, double stockTotal, double gainTotal,
			String symbol, double buy) {
		return stock(date, fundTotal, cashTotal, stockTotal, gainTotal,
				symbol, 0, buy, 0, 0, 0);
	}
	public static Account sell(String date, double fundTotal, double cashTotal, double stockTotal, double gainTotal,
			String symbol, double sell, double sellCost, double sellGain) {
		return stock(date, fundTotal, cashTotal, stockTotal, gainTotal,
				symbol, 0, 0, sell, sellCost, sellGain);
	}

	@Override
	public String toString() {
		return String.format("%s %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %9.2f %-10s %9.2f %9.2f %9.2f %9.2f", 
			date, fundTotal, cashTotal, stockTotal, gainTotal, wireIn, wireOut, achIn, achOut, interest, dividend, symbol, buy, sell, sellCost, sellGain);
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
