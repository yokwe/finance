package yokwe.finance.stock.monex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import yokwe.finance.stock.data.StockHistory;
import yokwe.finance.stock.data.StockHistoryUtil;
import yokwe.finance.stock.data.YahooPortfolio;
import yokwe.finance.stock.util.CSVUtil;
import yokwe.finance.stock.util.DoubleUtil;

public class UpdateYahooPortfolio {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateYahooPortfolio.class);
	
	public static final String PATH_YAHOO_PORTFOLIO = "tmp/monex/yahoo-portfolio-monex.csv";

	public static void main(String[] args) {
		logger.info("START");
		
		List<YahooPortfolio> yahooPortfolioList = new ArrayList<>();
		
		Map<String, List<StockHistory>> stockHistoryMap = StockHistoryUtil.getStockHistoryMap(".", StockHistoryUtil.PATH_STOCK_HISTORY_MONEX);
		logger.info("stockHistoryMap {}", stockHistoryMap.size());
		
		for(Map.Entry<String, List<StockHistory>> entry: stockHistoryMap.entrySet()) {
			List<StockHistory> stockHistoryList = entry.getValue();
			
			StockHistory lastStockHistory = stockHistoryList.get(stockHistoryList.size() - 1);
			if (lastStockHistory.totalQuantity == 0) continue;
			
			// Change symbol style from iex to yahoo
			String symbol        = entry.getKey().replaceAll("-", "-P");
			double quantity      = lastStockHistory.totalQuantity;
			double purchasePrice = DoubleUtil.round(lastStockHistory.totalCost / lastStockHistory.totalQuantity, 2);
			
			yahooPortfolioList.add(new YahooPortfolio(symbol, purchasePrice, quantity));
		}
		
		CSVUtil.saveWithHeader(yahooPortfolioList, PATH_YAHOO_PORTFOLIO);
		logger.info("yahooPortfolioList {}", yahooPortfolioList.size());
		
		logger.info("STOP");
	}
}
