package yokwe.finance.securities.monex;

import yokwe.finance.securities.libreoffice.Sheet;

@Sheet.SheetName("Transaction")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class Activity extends Sheet {
	@ColumnName("受渡日")
	public String settlementDate;
	
	@ColumnName("約定日")
	public String tradeDate;
	
	// 金銭 外株
	@ColumnName("商品")
	public String product;
	
	// 証券総合口座より
	// 円貨から外貨預り金へ（外貨）
	// 円貨から外貨預り金へ（円貨）
	@ColumnName("銘柄・適用")
	public String detail;
	
	// 特定
	@ColumnName("口座区分")
	public String accountType;

	// 買付
	@ColumnName("取引")
	public String transaction;

	@ColumnName("数量")
	public int quantity;

	@ColumnName("単価")
	public double unitPrice;

	@ColumnName("受渡金額")
	public double amount;

	@ColumnName("為替レート")
	public double fxRate;

	@ColumnName("手数料")
	public double fee;

	@ColumnName("消費税")
	public double consumptionTax;

	@ColumnName("換算金額")
	public double equivalentAmount;

	@Override
	public String toString() {
		return String.format("%s %s %s %s %s %8.2f %8.2f %8.2f %8.2f",
				settlementDate, tradeDate, product, detail, transaction, quantity, unitPrice, amount, fxRate);
	}
}
