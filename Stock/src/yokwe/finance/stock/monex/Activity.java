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
		public static final String TRANSACTION_ADR_FEE          = "ADRﾃｽｳﾘﾖｳ";

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
			return String.format("%s %s %.2f %.2f %d", settlementDate, transaction, fxRate, usd, jpy);
		}
	}

	@Sheet.SheetName("Trade")
	@Sheet.HeaderRow(0)
	@Sheet.DataRow(1)
	public static class Trade extends Sheet {
		public static final String TRANSACTION_BUY    = "買付";
		public static final String TRANSACTION_SELL   = "売付";
		public static final String TRANSACTION_CHANGE = "変更";

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

		@ColumnName("国内手数料率")
		public double feeRateJP;

		@ColumnName("消費税率")
		public double consumptionTaxRateJP;

		@ColumnName("手数料率")
		public double totalFeeRateJP;
		
		@ColumnName("国内手数料税抜")
		public double feeBeforeTaxJP;

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
				double subTotalPrice, double fxRate,
				double feeRateJP, double consumptionTaxRateJP, double totalFeeRateJP, double feeBeforeTaxJP,
				double feeJP, double consumptionTaxJP, double withholdingTaxJP,
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
			this.feeRateJP        = feeRateJP;
			this.consumptionTaxRateJP = consumptionTaxRateJP;
			this.totalFeeRateJP   = totalFeeRateJP;
			this.feeBeforeTaxJP   = feeBeforeTaxJP;
			this.feeJP            = feeJP;
			this.consumptionTaxJP = consumptionTaxJP;
			this.withholdingTaxJP = withholdingTaxJP;
			this.total            = total;
			this.totalJPY         = totalJPY;
		}

		public Trade() {
			this(null, null, null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
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
	
	@Sheet.SheetName("配当")
	@Sheet.HeaderRow(0)
	@Sheet.DataRow(1)
	public static class Dividend extends Sheet {
		@ColumnName("確定日")
		@NumberFormat(SpreadSheet.FORMAT_DATE)
		public String payDateUS;

		@ColumnName("支払日")
		@NumberFormat(SpreadSheet.FORMAT_DATE)
		public String payDateJP;

		@ColumnName("銘柄")
		public String securityCode;

		@ColumnName("シンボル")
		public String symbol;

		@ColumnName("数量")
		public int quantity;

		@ColumnName("単価")
		public double unitPrice;

		@ColumnName("税込金額")
		public double taxBaseUS;

		@ColumnName("国外税率")
		public double taxRateUS;
		
		@ColumnName("国外税額")
		public double withholdingTaxUS;

		@ColumnName("差引金額")
		public double amount1;
		
		@ColumnName("所得税率")
		public double incomeTaxRateJP;
		
		@ColumnName("地方税率")
		public double localTaxRateJP;
		
		@ColumnName("国内税額")
		public double withholdingTaxJPUS;

		@ColumnName("受取金額")
		public double amount2;

		@ColumnName("為替レート")
		public double fxRate;

		@ColumnName("課税対象額")
		public double taxBaseJP;
		
		@ColumnName("所得税")
		public double incomeTaxJP;
		
		@ColumnName("地方税")
		public double localTaxJP;
		
		@ColumnName("国内源泉額")
		public double withholdingTaxJP;
		
		@ColumnName("国外課税額")
		public double taxBaseUSJP;
		
		@ColumnName("国外源泉額")
		public double withholdingTaxUSJP;
		

		public Dividend(String payDateUS, String payDateJP, String securityCode, String symbol,
				int quantity, double unitPrice, double taxBaseUS,
				double taxRateUS, double withholdingTaxUS, double amount1,
				double incomeTaxRateJP, double localTaxRateJP, double withholdingTaxJPUS, double amount2,
				double fxRate, double taxBaseJP, double incomeTaxJP, double localTaxJP, double withholdingTaxJP, double taxBaseUSJP, double withholdingTaxUSJP) {
			this.payDateUS          = payDateUS;
			this.payDateJP          = payDateJP;
			this.securityCode       = securityCode;
			this.symbol             = symbol;
			
			this.quantity           = quantity;
			this.unitPrice          = unitPrice;
			this.taxBaseUS          = taxBaseUS;
			
			this.taxRateUS          = taxRateUS;
			this.withholdingTaxUS   = withholdingTaxUS;
			this.amount1            = amount1;

			this.incomeTaxRateJP    = incomeTaxRateJP;
			this.localTaxRateJP     = localTaxRateJP;
			this.withholdingTaxJPUS = withholdingTaxJPUS;
			this.amount2            = amount2;
			
			this.fxRate             = fxRate;
			this.taxBaseJP          = taxBaseJP;
			this.incomeTaxJP        = incomeTaxJP;
			this.localTaxJP         = localTaxJP;
			this.withholdingTaxJP   = withholdingTaxJP;
			this.taxBaseUSJP        = taxBaseUSJP;
			this.withholdingTaxUSJP = withholdingTaxUSJP;
		}

		public Dividend() {
			this(null, null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		}

		@Override
		public String toString() {
			return String.format("%s %s %s %s   %d %.2f %.2f   %.2f %.2f   %.2f %.2f   %.2f %.2f %.2f %.2f %.2f", 
				payDateUS, payDateJP, securityCode, symbol,
				quantity, unitPrice, taxBaseUS,
				withholdingTaxUS, amount1,
				withholdingTaxJPUS, amount2,
				fxRate, taxBaseJP, withholdingTaxJP, taxBaseUSJP, withholdingTaxUSJP);
		}
	}
}
