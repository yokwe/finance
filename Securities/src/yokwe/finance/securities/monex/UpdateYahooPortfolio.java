package yokwe.finance.securities.monex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.eod.StockHistory;
import yokwe.finance.securities.eod.YahooPortfolio;
import yokwe.finance.securities.eod.tax.Transaction;
import yokwe.finance.securities.util.CSVUtil;

public class UpdateYahooPortfolio {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateYahooPortfolio.class);
	
	public static final String PATH_YAHOO_PORTFOLIO = "tmp/monex/yahoo-portfolio-monex.csv";

	public static void main(String[] args) {
		logger.info("START");
		
		List<YahooPortfolio> yahooPortfolioList = new ArrayList<>();
		
		Map<String, List<StockHistory>> stockHistoryMap = UpdateStockHistory.getStockHistoryMap();
		logger.info("stockHistoryMap {}", stockHistoryMap.size());
		
		for(Map.Entry<String, List<StockHistory>> entry: stockHistoryMap.entrySet()) {
			List<StockHistory> stockHistoryList = entry.getValue();
			
			StockHistory lastStockHistory = stockHistoryList.get(stockHistoryList.size() - 1);
			if (lastStockHistory.totalQuantity == 0) continue;
			
			// Change symbol style from iex to yahoo
			String symbol        = entry.getKey().replaceAll("-", "-P");
			double quantity      = lastStockHistory.totalQuantity;
			double purchasePrice = Transaction.roundPrice(lastStockHistory.totalCost / lastStockHistory.totalQuantity);
			
			yahooPortfolioList.add(new YahooPortfolio(symbol, purchasePrice, quantity));
		}
		
		CSVUtil.saveWithHeader(yahooPortfolioList, PATH_YAHOO_PORTFOLIO);
		logger.info("yahooPortfolioList {}", yahooPortfolioList.size());
		
		logger.info("STOP");
	}
}
