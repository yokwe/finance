package yokwe.finance.securities.book;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class Stock {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(Stock.class);
	
	// Need to record value in USD, JPY and conversion rate.
	public final String symbol;
	public final String name;
	public String tradeDateFirst;
	public String tradeDateLast;
	public double quantity;
	public double value; // value in JPY for tax declaration
	public int    count; // count of transaction
	
	private Stock(String symbol, String name, String tradeDate, double quantity, double value) {
		this.symbol         = symbol;
		this.name           = name;
		this.tradeDateFirst = tradeDate;
		this.tradeDateLast  = tradeDate;
		this.quantity       = quantity;
		this.value          = value;
		this.count          = 1;
	}
	
	public static class Map {
		private java.util.Map<String, Stock> stockMap = new TreeMap<>();
		
		// TODO Use BigDecimal to calculate precious value using rounding mode and scale
		void buy(String symbol, String name, double quantity, String tradeDate, double price, double commission, double usdjpy) {
			double buyValue = Math.round((quantity * price + commission) * usdjpy);
			if (stockMap.containsKey(symbol)) {
				Stock stock = stockMap.get(symbol);
				stock.tradeDateLast  = tradeDate;
				stock.quantity      += quantity;
				stock.value         += buyValue;
				stock.count         += 1;
			} else { 
				stockMap.put(symbol, new Stock(symbol, name, tradeDate, quantity, buyValue));
			}
//			logger.info("{}", String.format("BUY  %s  %s  %6.2f  %3.0f  %8.0f", tradeDate, symbol, usdjpy, quantity, buyValue));
		}
		
		ReportSell sell(String symbol, String symbolName, double sellQuantity, String tradeDate, double price, double commission, double usdjpy) {
			if (stockMap.containsKey(symbol)) {
				Stock stock = stockMap.get(symbol);
				
				{
					double remainingQuantity = stock.quantity - sellQuantity;
					if (remainingQuantity < 0.00001) {
						// remove symbol if quantity is zero
						stockMap.remove(symbol);
					}
				}
				
				if (stock.count == 1) {
					// If stock was bought just once, calculation of priceBuy be done with ratio of buy and sell.
					//   https://www.nta.go.jp/taxanswer/shotoku/1464.htm
					double buyRatio       = (sellQuantity / stock.quantity);
					double priceBuy       = stock.value * buyRatio;
					double priceSell      = Math.round(sellQuantity * price * usdjpy);
					double commissionSell = Math.round(commission * usdjpy);
					
					// TODO How to get symbolName?
					ReportSell result = new ReportSell(tradeDate, symbol, symbolName, sellQuantity, (int)priceSell, (int)priceBuy, (int)commissionSell, stock.tradeDateFirst, "");
					logger.info("{}", result);
					
					stock.quantity  = stock.quantity - sellQuantity;
					stock.value     = stock.value - priceBuy;
									
					return result;
				} else {
					// If same stock was bought more than once, calculation of priceBuy must be done with Weighted-Average method (Sou heikin hou) describe below.
					//   https://www.nta.go.jp/taxanswer/shotoku/1466.htm
					double unitCost       = Math.ceil(stock.value / stock.quantity); // Round up
					double priceBuy       = Math.round(sellQuantity * unitCost);
					double priceSell      = Math.round(sellQuantity * price * usdjpy);
					double commissionSell = Math.round(commission * usdjpy);
					
					// TODO How to get symbolName?
					ReportSell result = new ReportSell(tradeDate, symbol, symbolName, sellQuantity, (int)priceSell, (int)priceBuy, (int)commissionSell, stock.tradeDateFirst, stock.tradeDateLast);
					logger.info("{}", result);
					
					stock.quantity        = stock.quantity - sellQuantity;
					stock.value           = (int)Math.round(stock.quantity * unitCost);
									
					return result;
				}
			} else {
				logger.error("Unknown symbol = {}", symbol);
				throw new SecuritiesException("Unexptected");
			}
		}
	}
	
	public static void main(String[] args) {
		logger.info("START");
		Stock.Map stockMap = new Stock.Map();
		String url = "file:///home/hasegawa/Dropbox/Trade/投資損益計算_2016.ods";
		
		List<ReportSell> reportSellList = new ArrayList<>();
		
		try (LibreOffice libreOffice = new LibreOffice(url, true)) {
			List<TransactionBuySell> transactionList = SheetData.getInstance(libreOffice, TransactionBuySell.class);
	
			Mizuho.Map mizuhoMap = new Mizuho.Map(url);
			SymbolName.Map symbolNameMap = new SymbolName.Map(url);

			for(TransactionBuySell transaction: transactionList) {
				double usdjpy = mizuhoMap.get(transaction.tradeDate).usd;
				String symbolName = symbolNameMap.getName(transaction.symbol);
				switch (transaction.transaction) {
				case "BOUGHT": {
					stockMap.buy(transaction.symbol, symbolName, transaction.quantity, transaction.tradeDate, transaction.price, transaction.commission, usdjpy);
					break;
				}
				case "SOLD": {
					ReportSell reportSell = stockMap.sell(transaction.symbol, symbolName, transaction.quantity, transaction.tradeDate, transaction.price, transaction.commission, usdjpy);
					reportSellList.add(reportSell);
					break;
				}
				default: {
					logger.error("Unknown transaction = {}", transaction.transaction);
					throw new SecuritiesException("Unexpected");
				}
				}
			}
			
			{
				String urlLoad = "file:///home/hasegawa/Dropbox/Trade/T002_LOAD.ods";
				String urlSave = "file:///home/hasegawa/Dropbox/Trade/T002_SAVE.ods";
				
				try (LibreOffice docLoad = new LibreOffice(urlLoad, true)) {
					SheetData.saveSheet(docLoad, ReportSell.class, reportSellList);
					docLoad.store(urlSave);
				}
			}
			
			logger.info("IVV  {}", symbolNameMap.getName("IVV"));
			logger.info("IJH  {}", symbolNameMap.getName("IJH"));
			logger.info("STOP");
			System.exit(0);
		}
	}
}
