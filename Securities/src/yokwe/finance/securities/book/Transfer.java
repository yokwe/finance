package yokwe.finance.securities.book;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class Transfer {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(Transfer.class);
	
	private static final double ALMOST_ZERO = 0.000001;
	
	String dateBuyFirst;
	String dateBuyLast;
	String symbol;
	String name;
	double quantity;
	double price;
	double commission;
	double usdjpy;
	
	int          count;
	int          acquisitionCostJPY;
	List<ReportTransfer> reportList;
	
	private Transfer(String date, String symbol, String name, double quantity, double price, double commission, double usdjpy) {
		this.dateBuyFirst  = date;
		this.dateBuyLast   = "";
		this.symbol        = symbol;
		this.name          = name;
		this.quantity      = quantity;
		this.price         = price;
		this.commission    = commission;
		this.usdjpy        = usdjpy;
		
		count              = 1;
		acquisitionCostJPY = (int)Math.round((quantity * price + commission) * usdjpy);
		reportList         = new ArrayList<>();
	}
	
	private static Map<String, Transfer> transferMap = new LinkedHashMap<>();
	
	public static void buy(String date, String symbol, String name, double quantity, double price, double commission, double usdjpy, List<ReportTransfer> reportList) {
		int acquisitionCostJPY = (int)Math.round((quantity * price + commission) * usdjpy);

		if (transferMap.containsKey(symbol)) {
			Transfer transfer = transferMap.get(symbol);
			
			transfer.dateBuyLast         = date;
			transfer.quantity           += quantity;
			transfer.price               = price;
			transfer.commission          = commission;
			transfer.usdjpy              = usdjpy;
			transfer.count++;
			transfer.acquisitionCostJPY += acquisitionCostJPY;
			
			// Special case for TAL/TRTN(negative quantity for BUY)
			if (Math.abs(transfer.quantity) < ALMOST_ZERO) {
				transferMap.remove(symbol);
			}
			
			ReportTransfer report = ReportTransfer.getInstance(symbol, name, quantity, date, price, commission, usdjpy, acquisitionCostJPY, transfer.quantity, transfer.acquisitionCostJPY);
			transfer.reportList.add(report);
		} else {
			Transfer transfer = new Transfer(date, symbol, name, quantity, price, commission, usdjpy);
			ReportTransfer report = ReportTransfer.getInstance(symbol, name, quantity, date, price, commission, usdjpy, acquisitionCostJPY, transfer.quantity, transfer.acquisitionCostJPY);
			transfer.reportList.add(report);
			transferMap.put(symbol, transfer);
		}
	}
	
	public static void sell(String date, String symbol, String name, double quantity, double price, double commission, double usdjpy, List<ReportTransfer> reportList) {
		if (transferMap.containsKey(symbol)) {
			double priceSell        = price * quantity;
			int    sellAmountJPY    = (int)Math.round(priceSell * usdjpy);
			int    sellCommisionJPY = (int)Math.round(commission * usdjpy);

			Transfer transfer = transferMap.get(symbol);
			
			int acquisitionCostJPY;
			if (transfer.count == 1) {
				acquisitionCostJPY = (int)Math.round(transfer.acquisitionCostJPY * (quantity / transfer.quantity));
				
				// date symbol name sellAmountJPY asquisionCostJPY sellCommisionJPY dateBuyFirst dateBuyLast
				logger.info("SELL {}", String.format("%s %-8s %9.5f %7d %7d %7d %s %s",
						date, symbol, quantity, sellAmountJPY, acquisitionCostJPY, sellCommisionJPY, transfer.dateBuyFirst, transfer.dateBuyLast));
				
				// maintain securities
				transfer.quantity           -= quantity;
				transfer.acquisitionCostJPY -= acquisitionCostJPY;
			} else {
				double unitCost = Math.ceil(transfer.acquisitionCostJPY / transfer.quantity);
				acquisitionCostJPY = (int)Math.round(unitCost * quantity);
				
				transfer.quantity           -= quantity;
				transfer.acquisitionCostJPY  = (int)Math.round(unitCost * transfer.quantity);
				
				// date symbol name sellAmountJPY asquisionCostJPY sellCommisionJPY dateBuyFirst dateBuyLast
				logger.info("SELL*{}", String.format("%s %-8s %9.5f %7d %7d %7d %s %s",
						date, symbol, quantity, sellAmountJPY, acquisitionCostJPY, sellCommisionJPY, transfer.dateBuyFirst, transfer.dateBuyLast));
			}
						
			if (transfer.count == 1 && transfer.reportList.size() == 1 && Math.abs(transfer.quantity) < ALMOST_ZERO) {
				// Special case: buy once and sell whole.
				//   Output one record for both buy and sell
				ReportTransfer buy  = transfer.reportList.get(0);
				ReportTransfer sell = ReportTransfer.getInstance(symbol, name, quantity, date, price, commission, usdjpy, sellCommisionJPY, sellAmountJPY, acquisitionCostJPY, transfer.dateBuyFirst, transfer.dateBuyLast);
				
				ReportTransfer report = ReportTransfer.getInstance(
						symbol, name, quantity,
						sell.dateSell, sell.priceSell, sell.commissionSell, sell.fxRateSell, sell.commissionSellJPY, sell.amountSellJPY, sell.acquisitionCostJPY, sell.dateBuyFirst, sell.dateBuyLast,
						buy.dateBuy, buy.priceBuy, buy.commissionBuy, buy.fxRateBuy, buy.amountBuyJPY, "", "");
				transfer.reportList.clear();
				transfer.reportList.add(report);
			} else {
				ReportTransfer report = ReportTransfer.getInstance(symbol, name, quantity, date, price, commission, usdjpy, sellCommisionJPY, sellAmountJPY, acquisitionCostJPY, transfer.dateBuyFirst, transfer.dateBuyLast);
				transfer.reportList.add(report);
			}
			
			// If quantity of securities become ZERO, output accumulated reportList and remove from securitiesMap
			if (Math.abs(transfer.quantity) < ALMOST_ZERO) {
				for(ReportTransfer report: transfer.reportList) {
					reportList.add(report);
				}
				transfer.reportList.clear();
				transferMap.remove(symbol);
			}
		} else {
			logger.error("Unknown symbol = {}", symbol);
			throw new SecuritiesException("Unexpected");
		}
	}
	
	public static void addRemaining(List<ReportTransfer> reportList) {
		for(Map.Entry<String, Transfer> entry: transferMap.entrySet()) {
			Transfer transfer = entry.getValue();
			reportList.addAll(transfer.reportList);
		}
	}

	public static void addRemaining(String date, double usdjpy, EquityStats.Map equityStatsMap, List<ReportTransfer> reportList) {
		double commission = 7;
		List<String> keys = new ArrayList<>();
		for(Map.Entry<String, Transfer> entry: transferMap.entrySet()) keys.add(entry.getKey());
			
		for(String key: keys) {
			Transfer transfer = transferMap.get(key);
			String symbol = transfer.symbol;
			String name   = transfer.name;
			double price  = equityStatsMap.get(symbol).price;
			
			// calculate quantity
			double quantity = 0;
			for(ReportTransfer reportTransfer: transfer.reportList) {
				if (0 < reportTransfer.dateSell.length()) {
					// sell
					quantity -= reportTransfer.quantity;
				} else {
					// buy
					quantity += reportTransfer.quantity;
				}
			}
			// invoke sell method to generate fake sell entry
			sell(date, symbol, name, quantity, price, commission, usdjpy, reportList);
		}
	}
}
