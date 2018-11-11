package yokwe.finance.stock.report;

import yokwe.finance.stock.libreoffice.Sheet;
import yokwe.finance.stock.libreoffice.SpreadSheet;

@Sheet.SheetName("口座")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class Account extends Sheet {
	@ColumnName("年月日")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public final String date;
	
	@ColumnName("円入金")
	@NumberFormat(SpreadSheet.FORMAT_JPY_BLANK)
	public final Integer depositJPY;
	
	@ColumnName("円出金")
	@NumberFormat(SpreadSheet.FORMAT_JPY_BLANK)
	public final Integer withdrawJPY;
	
	@ColumnName("円資金")
	@NumberFormat(SpreadSheet.FORMAT_JPY)
	public final Integer fundJPY;
	
	@ColumnName("入金")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public final Double deposit;
	
	@ColumnName("出金")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public final Double withdraw;
	
	@ColumnName("資金")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final Double fund;
	
	@ColumnName("現金")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final Double cash;
	
	@ColumnName("株式")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final Double stock;
	
	@ColumnName("損益")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final Double gain;
	
	@ColumnName("銘柄コード")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public final String symbol;
	
	@ColumnName("配当")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public final Double dividend;
	
	@ColumnName("購入")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public final Double buy;
	
	@ColumnName("売却")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public final Double sell;
	
	@ColumnName("売却原価")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public final Double sellCost;
	
	@ColumnName("売却損益")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public final Double sellGain;

	private Account(
		String date,
		Integer depositJPY, Integer withdrawJPY, Integer fundJPY,
		Double deposit, Double withdraw, Double fund,
		Double cash, Double stock, Double gain,
		String symbol, Double dividend, Double buy, Double sell, Double sellCost, Double sellGain) {
		this.date = date;
		this.depositJPY = depositJPY;
		this.withdrawJPY = withdrawJPY;
		this.fundJPY = fundJPY;
		this.deposit = deposit;
		this.withdraw = withdraw;
		this.fund = fund;
		this.cash = cash;
		this.stock = stock;
		this.gain = gain;
		this.symbol = symbol;
		this.dividend = dividend;
		this.buy = buy;
		this.sell = sell;
		this.sellCost = sellCost;
		this.sellGain = sellGain;
	}
			
	private Account() {
		this(
			null,
			null, null, null,
			null, null, null,
			null, null, null,
			null, null, null, null, null, null);
	}
	
	public static Account fundJPY(String date, Integer depositJPY, Integer withdrawJPY, Integer fundJPY, Double fund, Double cash, Double stock, Double gain) {
		return new Account(
			date,
			depositJPY, withdrawJPY, fundJPY,
			null, null, fund,
			cash, stock, gain,
			null, null, null, null, null, null);
	}
	public static Account fundUSD(String date, Integer fundJPY, Double deposit, Double withdraw, Double fund, Double cash, Double stock, Double gain) {
		return new Account(
			date,
			null, null, fundJPY,
			deposit, withdraw, fund,
			cash, stock, gain,
			null, null, null, null, null, null);
	}
	public static Account fundJPYUSD(String date, Integer depositJPY, Integer withdrawJPY, Integer fundJPY,
		Double deposit, Double withdraw, Double fund, Double cash, Double stock, Double gain) {
		return new Account(
			date,
			depositJPY, withdrawJPY, fundJPY,
			deposit, withdraw, fund,
			cash, stock, gain,
			null, null, null, null, null, null);
	}
	public static Account dividend(String date, Integer fundJPY, Double dividend, Double fund, Double cash, Double stock, Double gain, String symbol) {
		return new Account(
			date,
			null, null, fundJPY,
			null, null, fund,
			cash, stock, gain,
			symbol, dividend, null, null, null, null);
	}
	public static Account buy(String date, Integer fundJPY, Double fund, Double cash, Double stock, Double gain, String symbol, Double buy) {
		return new Account(
			date,
			null, null, fundJPY,
			null, null, fund,
			cash, stock, gain,
			symbol, null, buy, null, null, null);
	}
	public static Account sell(String date, Integer fundJPY, Double fund, Double cash, Double stock, Double gain, String symbol, Double sell, Double sellCost, Double sellGain) {
		return new Account(
			date,
			null, null, fundJPY,
			null, null, fund,
			cash, stock, gain,
			symbol, null, null, sell, sellCost, sellGain);
	}
	public static Account change(String date, Integer fundJPY, Double fund, Double cash, Double stock, Double gain, String symbol) {
		return new Account(
			date,
			null, null, fundJPY,
			null, null, fund,
			cash, stock, gain,
			symbol, null, null, null, null, null);
	}
}
