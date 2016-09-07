package yokwe.finance.securities.book;

import java.util.List;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class Stock {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(Stock.class);
	
	public final String symbol;
	public String tradeDateFirst;
	public String tradeDateLast;
	public double quantity;
	public double value; // value in JPY for tax declaration
	public int    count; // count of transaction
	
	private Stock(String symbol, String tradeDate, double quantity, double value) {
		this.symbol         = symbol;
		this.tradeDateFirst = tradeDate;
		this.tradeDateLast  = tradeDate;
		this.quantity       = quantity;
		this.value          = value;
		this.count          = 1;
	}
	
	public static class Map {
		private java.util.Map<String, Stock> stockMap = new TreeMap<>();
		
		// TODO Use BigDecimal to calculate precious value using rounding mode and scale
		void buy(String symbol, double quantity, String tradeDate, double price, double commission, double usdjpy) {
			double buyValue = Math.round((quantity * price + commission) * usdjpy);
			if (stockMap.containsKey(symbol)) {
				Stock stock = stockMap.get(symbol);
				stock.tradeDateLast  = tradeDate;
				stock.quantity      += quantity;
				stock.value         += buyValue;
				stock.count         += 1;
			} else { 
				stockMap.put(symbol, new Stock(symbol, tradeDate, quantity, buyValue));
			}
//			logger.info("{}", String.format("BUY  %s  %s  %6.2f  %3.0f  %8.0f", tradeDate, symbol, usdjpy, quantity, buyValue));
		}
		
		SellReport sell(String symbol, double sellQuantity, String tradeDate, double price, double commission, double usdjpy) {
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
					double buyRatio       = (sellQuantity / stock.quantity);
					double priceBuy       = stock.value * buyRatio;
					double priceSell      = Math.round(sellQuantity * price * usdjpy);
					double commissionSell = Math.round(commission * usdjpy);
					
					// TODO How to get symbolName?
					SellReport result = new SellReport(tradeDate, symbol, "", sellQuantity, (int)priceSell, (int)priceBuy, (int)commissionSell, stock.tradeDateFirst, "");
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
					
					SellReport result = new SellReport(tradeDate, symbol, sellQuantity, (int)priceSell, (int)priceBuy, (int)commissionSell, stock.tradeDateFirst, stock.tradeDateLast);
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
		
		try (LibreOffice libreOffice = new LibreOffice(url)) {
			List<BuySellTransaction> transactionList = SheetData.getInstance(libreOffice, BuySellTransaction.class);
	
			Mizuho.Map mizuhoMap = new Mizuho.Map(url);

			for(BuySellTransaction transaction: transactionList) {
				double usdjpy = mizuhoMap.get(transaction.tradeDate).usd;
				
				switch (transaction.transaction) {
				case "BOUGHT": {
					stockMap.buy(transaction.symbol, transaction.quantity, transaction.tradeDate, transaction.price, transaction.commission, usdjpy);
					break;
				}
				case "SOLD": {
					stockMap.sell(transaction.symbol, transaction.quantity, transaction.tradeDate, transaction.price, transaction.commission, usdjpy);
					break;
				}
				default: {
					logger.error("Unknown transaction = {}", transaction.transaction);
					throw new SecuritiesException("Unexpected");
				}
				}
			}
			logger.info("STOP");
			System.exit(0);
		}
	}
}
