package yokwe.finance.securities.book;

import java.util.List;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class Securities {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(Securities.class);

	String symbol;
	String tradeDate;
	double quantities;
	int    value; // value in JPY for tax declaration
	
	private Securities(String symbol, String tradeDate, double quantities, int value) {
		this.symbol     = symbol;
		this.tradeDate  = tradeDate;
		this.quantities = quantities;
		this.value      = value;
	}
	
	public static class Map {
		private java.util.Map<String, Securities> map = new TreeMap<>();
		
		void buy(String symbol, double quantities, String tradeDate, int value) {
			if (map.containsKey(symbol)) {
				Securities securities = map.get(symbol);
				securities.tradeDate   = tradeDate;
				securities.quantities += quantities;
				securities.value      += value;
			} else {
				map.put(symbol, new Securities(symbol, tradeDate, quantities, value));
			}
		}
		
		void sell(String symbol, double sellQuantities, String tradeDate, int value) {
			if (map.containsKey(symbol)) {
				Securities securities = map.get(symbol);
				double sellRatio = sellQuantities / securities.quantities;
				int sellValue = (int)Math.round(securities.value * sellRatio);
				
				securities.tradeDate  = tradeDate;
				securities.quantities = securities.quantities - sellQuantities;
				securities.value      = securities.value - sellValue;
				
				logger.info("{}", String.format("%s %-8s  %6.2f  %8d  %8d", tradeDate, symbol, sellQuantities, value - sellValue, sellValue));
			} else {
				logger.error("Unknown symbol = {}", symbol);
				throw new SecuritiesException("Unexptected");
			}
		}
	}
	
	public static void main(String[] args) {
		String url = "file:///home/hasegawa/Dropbox/Trade/投資損益計算_2016_SAVE.ods";
		
		logger.info("START");
		Map securitiesMap = new Map();
		
		try (LibreOffice libreOffice = new LibreOffice(url)) {			
			List<BuySellTransactions> transactionList = SheetData.getInstance(libreOffice, BuySellTransactions.class);
			for(BuySellTransactions transaction: transactionList) {
//				logger.info("{}", transaction);
				double usdjpy = 1;
				
				switch (transaction.transaction) {
				case "BOUGHT": {
					securitiesMap.buy(transaction.symbol, transaction.quantity, transaction.tradeDate, (int)Math.round(transaction.debit * usdjpy));
					break;
				}
				case "SOLD": {
					securitiesMap.sell(transaction.symbol, transaction.quantity, transaction.tradeDate, (int)Math.round(transaction.credit * usdjpy));
					break;
				}
				default: {
					logger.error("Unknown transaction = {}", transaction.transaction);
					throw new SecuritiesException("Unexpected");
				}
				}
			}
		}
		logger.info("STOP");
		System.exit(0);
	}
}
