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
		public final String dateBuy;
		
		public Result(String dateSell, String symbol, double quantity, int priceSell, int priceBuy, int commissionSell, String dateBuy) {
			this.dateSell       = dateSell;
			this.symbol         = symbol;
			this.quantity       = quantity;
			this.priceSell      = priceSell;
			this.priceBuy       = priceBuy;
			this.commissionSell = commissionSell;
			this.dateBuy        = dateBuy;
		}
		
		public String toString() {
			return String.format("%s  %-8s  %5.0f  %8d  %8d  %3d  %s  -  %8d", dateSell, symbol, quantity, priceSell, priceBuy, commissionSell, dateBuy, priceSell - priceBuy - commissionSell);
		}
	}

	public final String symbol;
	public String tradeDate;
	public double quantity;
	public double value; // value in JPY for tax declaration
	
	private Stock(String symbol, String tradeDate, double quantity, double value) {
		this.symbol    = symbol;
		this.tradeDate = tradeDate;
		this.quantity  = quantity;
		this.value     = value;
	}
	
	public static class Map {
		private java.util.Map<String, Stock> stockMap = new TreeMap<>();
		
		// TODO USD must be round to unit of cent and multiply by USDJPY and round to integer
		// TODO Use NFLX as sample   valueSell=188,052  value buy = 181,918
		void buy(String symbol, double quantity, String tradeDate, double price, double commission, double usdjpy) {
			double buyValue = Math.round((quantity * price + commission) * usdjpy);
			if (stockMap.containsKey(symbol)) {
				Stock stock = stockMap.get(symbol);
				stock.tradeDate  = tradeDate;
				stock.quantity  += quantity;
				stock.value     += buyValue;
			} else { 
				stockMap.put(symbol, new Stock(symbol, tradeDate, quantity, buyValue));
			}
		}
		
		Result sell(String symbol, double sellQuantity, String tradeDate, double price, double commission, double usdjpy) {
			if (stockMap.containsKey(symbol)) {
				Stock stock = stockMap.get(symbol);
				
				// See below for calculation of obtaining cost of securities.
				//   https://www.nta.go.jp/taxanswer/shotoku/1466.htm
				double unitCost = Math.ceil(stock.value / stock.quantity);
				double priceBuy = Math.round(sellQuantity * unitCost);
				
				Result result = new Result(tradeDate, symbol, sellQuantity, (int)Math.round(sellQuantity * price * usdjpy), (int)priceBuy, (int)Math.round(commission * usdjpy), stock.tradeDate);
				
				stock.tradeDate = tradeDate;
				stock.quantity  = stock.quantity - sellQuantity;
				stock.value     = (int)Math.round(stock.quantity * unitCost);
				
				logger.info("{}", result);
				
				return result;
			} else {
				logger.error("Unknown symbol = {}", symbol);
				throw new SecuritiesException("Unexptected");
			}
		}
	}
	
	public static void main(String[] args) {
		logger.info("START");
		Stock.Map stockMap = new Stock.Map();
		String url = "file:///home/hasegawa/Dropbox/Trade/投資損益計算_2016_SAVE.ods";
		
		try (LibreOffice libreOffice = new LibreOffice(url)) {
			List<BuySellTransaction> transactionList = SheetData.getInstance(libreOffice, BuySellTransaction.class);
	
			Mizuho.Map mizuhoMap = new Mizuho.Map(url);

			for(BuySellTransaction transaction: transactionList) {
				double usdjpy = mizuhoMap.get(transaction.date).usd;
				
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
