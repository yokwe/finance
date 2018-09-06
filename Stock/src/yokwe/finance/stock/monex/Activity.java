package yokwe.finance.stock.monex;

import yokwe.finance.stock.libreoffice.Sheet;
import yokwe.finance.stock.libreoffice.SpreadSheet;

public class Activity {
	@Sheet.SheetName("口座")
	@Sheet.HeaderRow(0)
	@Sheet.DataRow(1)
	public static class Account extends Sheet {
		public static final String TRANSACTION_FROM_SOUGOU      = "証券総合口座より";
		public static final String TRANSACTION_TO_SOUGOU        = "証券総合口座へ";
		public static final String TRANSACTION_TO_USD_DEPOSIT   = "円貨から外貨預り金へ";
		public static final String TRANSACTION_FROM_USD_DEPOSIT = "外貨から円貨預り金へ";

		@ColumnName("受渡日")
		@NumberFormat(SpreadSheet.FORMAT_DATE)
		public String settlementDate;

		// 証券総合口座より
		// 証券総合口座へ
		// 円貨から外貨預り金へ
		// 外貨から円貨預り金へ
		@ColumnName("取引")
		public String transaction;

		@ColumnName("為替レート")
		public double fxRate;

		@ColumnName("米ドル")
		public double usd;

		@ColumnName("円貨")
		public int jpy;
		
		public Account(String settlementDate, String transaction, double fxRate, double usd, int jpy) {
			this.settlementDate = settlementDate;
			this.transaction    = transaction;
			this.fxRate         = fxRate;
			this.usd            = usd;
			this.jpy            = jpy;
		}
		public Account() {
			this(null, null, 0, 0, 0);
		}
		
		@Override
		public String toString() {
			return String.format("%s %s %s %.2f %.2f %d", settlementDate, transaction, fxRate, usd, jpy);
		}
	}

	@Sheet.SheetName("Trade")
	@Sheet.HeaderRow(0)
	@Sheet.DataRow(1)
	public static class Trade extends Sheet {
		public static final String TRANSACTION_BUY  = "買付";
		public static final String TRANSACTION_SELL = "売付";

		@ColumnName("約定日")
		@NumberFormat(SpreadSheet.FORMAT_DATE)
		public String tradeDate;

		@ColumnName("受渡日")
		@NumberFormat(SpreadSheet.FORMAT_DATE)
		public String settlementDate;

		@ColumnName("銘柄")
		public String securityCode;

		@ColumnName("シンボル")
		public String symbol;

		// 買付
		@ColumnName("取引")
		public String transaction;

		@ColumnName("数量")
		public int quantity;

		@ColumnName("約定単価")
		public double unitPrice;

		@ColumnName("売買代金")
		public double price;

		@ColumnName("取引税")
		public double tax;

		@ColumnName("手数料")
		public double fee;

		@ColumnName("その他")
		public double other;

		@ColumnName("差引代金")
		public double subTotalPrice;

		// fxRate of settlementDate for calculation of consumption tax
		@ColumnName("為替レート")
		public double fxRate;

		@ColumnName("国内手数料")
		public double feeJP;

		@ColumnName("消費税")
		public double consumptionTaxJP;

		@ColumnName("源泉税")
		public double withholdingTaxJP;

		@ColumnName("最終金額")
		public double total;

		@ColumnName("最終金額円貨")
		public int totalJPY;

		public Trade(String tradeDate, String settlementDate, String securityCode, String symbol, String transaction,
				int quantity, double unitPrice, double price, double tax, double fee, double other,
				double subTotalPrice, double fxRate, double feeJP, double consumptionTaxJP, double withholdingTaxJP,
				double total, int totalJPY) {
			this.tradeDate        = tradeDate;
			this.settlementDate   = settlementDate;
			this.securityCode     = securityCode;
			this.symbol           = symbol;
			this.transaction      = transaction;
			this.quantity         = quantity;
			this.unitPrice        = unitPrice;
			this.price            = price;
			this.tax              = tax;
			this.fee              = fee;
			this.other            = other;
			this.subTotalPrice    = subTotalPrice;
			this.fxRate           = fxRate;
			this.feeJP            = feeJP;
			this.consumptionTaxJP = consumptionTaxJP;
			this.withholdingTaxJP = withholdingTaxJP;
			this.total            = total;
			this.totalJPY         = totalJPY;
		}

		public Trade() {
			this(null, null, null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		}

		@Override
		public String toString() {
			return String.format("%s %s %s %s %s   %d %.2f %.2f %.2f %.2f %.2f   %.2f %.2f %.2f %.2f %.2f  %.2f %d", 
					tradeDate, settlementDate, securityCode, symbol, transaction,
					quantity, unitPrice, price, tax, fee, other,
					subTotalPrice, fxRate, feeJP, consumptionTaxJP, withholdingTaxJP,
					total, totalJPY);
		}
	}
}
