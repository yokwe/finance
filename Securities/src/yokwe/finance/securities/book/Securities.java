package yokwe.finance.securities.book;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class Securities {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(Securities.class);
	
	private static final double ALMOST_ZERO = 0.000001;
	
	String dateBuyFirst;
	String dateBuyLast;
	String symbol;
	String name;
	double quantity;
	double price;
	double commission;
	double usdjpy;
	
	int    count;
	int    acquisionCostJPY;
	
	private Securities(String date, String symbol, String name, double quantity, double price, double commission, double usdjpy) {
		this.dateBuyFirst = date;
		this.dateBuyLast  = "";
		this.symbol       = symbol;
		this.name         = name;
		this.quantity     = quantity;
		this.price        = price;
		this.commission   = commission;
		this.usdjpy       = usdjpy;
		
		count             = 1;
		acquisionCostJPY  = (int)Math.round((quantity * price + commission) * usdjpy);
	}
	
	private static Map<String, Securities> securitiesMap = new LinkedHashMap<>();
	
	public static void buy(String date, String symbol, String name, double quantity, double price, double commission, double usdjpy) {
		if (securitiesMap.containsKey(symbol)) {
			Securities securities = securitiesMap.get(symbol);
			
			securities.dateBuyLast       = date;
			securities.quantity         += quantity;
			securities.price             = price;
			securities.commission        = commission;
			securities.usdjpy            = usdjpy;
			securities.count++;
			securities.acquisionCostJPY += (int)Math.round((quantity * price + commission) * usdjpy);
			
			// Special case for TAL/TRTN(negative quantity for BUY)
			if (Math.abs(securities.quantity) < ALMOST_ZERO) {
				securitiesMap.remove(symbol);
			}
		} else {
			securitiesMap.put(symbol, new Securities(date, symbol, name, quantity, price, commission, usdjpy));
		}
	}
	
	public static void sell(String date, String symbol, String name, double quantity, double price, double commission, double usdjpy) {
		if (securitiesMap.containsKey(symbol)) {
			int sellAmountJPY     = (int)Math.round(price * quantity * usdjpy);
			int sellCommisionJPY  = (int)Math.round(commission * usdjpy);

			Securities securities = securitiesMap.get(symbol);
			
			if (securities.count == 1) {
				int acquisionCostJPY = (int)Math.round(securities.acquisionCostJPY * (quantity / securities.quantity));
				
				// date symbol name sellAmountJPY asquisionCostJPY sellCommisionJPY dateBuyFirst dateBuyLast
				logger.info("SELL {}", String.format("%s %-8s %9.5f %7d %7d %7d %s %s",
						date, symbol, quantity, sellAmountJPY, acquisionCostJPY, sellCommisionJPY, securities.dateBuyFirst, securities.dateBuyLast));
				
				// maintain securities
				securities.quantity         -= quantity;
				securities.acquisionCostJPY -= acquisionCostJPY;
			} else {
				double unitCost = Math.ceil(securities.acquisionCostJPY / securities.quantity);
				int acquisionCostJPY = (int)Math.round(unitCost * quantity);
				
				securities.quantity         -= quantity;
				securities.acquisionCostJPY  = (int)Math.round(unitCost * securities.quantity);
				
				// date symbol name sellAmountJPY asquisionCostJPY sellCommisionJPY dateBuyFirst dateBuyLast
				logger.info("SELL*{}", String.format("%s %-8s %9.5f %7d %7d %7d %s %s",
						date, symbol, quantity, sellAmountJPY, acquisionCostJPY, sellCommisionJPY, securities.dateBuyFirst, securities.dateBuyLast));
			}
			
			if (Math.abs(securities.quantity) < ALMOST_ZERO) {
				securitiesMap.remove(symbol);
			}
		} else {
			logger.error("Unknown symbol = {}", symbol);
			throw new SecuritiesException("Unexpected");
		}
	}
	
	public static void main(String[] args) {
		logger.info("START");
		String url = "file:///home/hasegawa/Dropbox/Trade/投資損益計算_2016.ods";
		
		try (LibreOffice libreOffice = new LibreOffice(url, true)) {
			List<TransactionBuySell> transactionList = SheetData.getInstance(libreOffice, TransactionBuySell.class);
	
			Mizuho.Map mizuhoMap = new Mizuho.Map(url);
			SymbolName.Map symbolNameMap = new SymbolName.Map(url);

			for(TransactionBuySell transaction: transactionList) {
				double usdjpy = mizuhoMap.get(transaction.tradeDate).usd;
				String symbolName = symbolNameMap.getName(transaction.symbol);
				switch (transaction.transaction) {
				case "BOUGHT": {
					Securities.buy(transaction.tradeDate, transaction.symbol, symbolName, transaction.quantity, transaction.price, transaction.commission, usdjpy);
					break;
				}
				case "SOLD": {
					Securities.sell(transaction.tradeDate, transaction.symbol, symbolName, transaction.quantity, transaction.price, transaction.commission, usdjpy);
					break;
				}
				default: {
					logger.error("Unknown transaction = {}", transaction.transaction);
					throw new SecuritiesException("Unexpected");
				}
				}
			}
			
			{
				int n = 1;
				for(Map.Entry<String, Securities> entry: securitiesMap.entrySet()) {
					Securities securities = entry.getValue();
					
					if (securities.count == 1) {
						// date symbol name sellAmountJPY asquisionCostJPY sellCommisionJPY dateBuyFirst dateBuyLast
						logger.info("BUY  {}", String.format("%10d %-8s %9.5f %7d %7d %7d %s %s",
								n++, securities.symbol, securities.quantity, 0, securities.acquisionCostJPY, 0, securities.dateBuyFirst, securities.dateBuyLast));
					} else {
						double unitCost = Math.ceil(securities.acquisionCostJPY / securities.quantity);
						int acquisionCostJPY = (int)Math.round(unitCost * securities.quantity);

						// date symbol name sellAmountJPY asquisionCostJPY sellCommisionJPY dateBuyFirst dateBuyLast
						logger.info("BUY *{}", String.format("%10d %-8s %9.5f %7d %7d %7d %s %s",
								n++, securities.symbol, securities.quantity, 0, acquisionCostJPY, 0, securities.dateBuyFirst, securities.dateBuyLast));
					}
				}
			}
			
			logger.info("STOP");
			System.exit(0);
		}
	}
}
