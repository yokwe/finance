package yokwe.finance.securities.book;

import java.util.List;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class Stock {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(Stock.class);
	
	public static class Result {
		public final String dateSell;
		public final String symbol;
		public final double quantity;
		public final int    priceSell;
		public final int    priceBuy;
		public final int    commissionSell;
		public final String dateBuyFirst;
		public final String dateBuyLast;
		
		public Result(String dateSell, String symbol, double quantity, int priceSell, int priceBuy, int commissionSell, String dateBuyFirst, String dateBuyLast) {
			this.dateSell       = dateSell;
			this.symbol         = symbol;
			this.quantity       = quantity;
			this.priceSell      = priceSell;
			this.priceBuy       = priceBuy;
			this.commissionSell = commissionSell;
			this.dateBuyFirst   = dateBuyFirst;
			this.dateBuyLast    = dateBuyLast;
		}
		
		@Override
		public String toString() {
			return String.format("%s  %-8s  %5.0f  %8d  %8d  %3d  %s  %s", dateSell, symbol, quantity, priceSell, priceBuy, commissionSell, dateBuyFirst, dateBuyLast);
		}
	}

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
		
		// TODO USD must be round to unit of cent and multiply by USDJPY and round to integer
		// TODO Use NFLX as sample   valueSell=188,052  value buy = 181,918
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
		
		Result sell(String symbol, double sellQuantity, String tradeDate, double price, double commission, double usdjpy) {
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
					
					Result result = new Result(tradeDate, symbol, sellQuantity, (int)priceSell, (int)priceBuy, (int)commissionSell, stock.tradeDateFirst, "");
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
					
					Result result = new Result(tradeDate, symbol, sellQuantity, (int)priceSell, (int)priceBuy, (int)commissionSell, stock.tradeDateFirst, stock.tradeDateLast);
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
