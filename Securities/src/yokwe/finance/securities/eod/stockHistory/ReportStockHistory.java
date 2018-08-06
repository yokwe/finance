package yokwe.finance.securities.eod.stockHistory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.eod.PriceUtil;
import yokwe.finance.securities.eod.StockHistory;
import yokwe.finance.securities.eod.UpdateStockHistory;
import yokwe.finance.securities.eod.tax.Transaction;
import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;

public class ReportStockHistory {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ReportStockHistory.class);
	
	public static final String TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());

	public static final String URL_TEMPLATE      = "file:///home/hasegawa/Dropbox/Trade/TEMPLATE_EOD_STOCK_HISTORY.ods";
	public static final String URL_REPORT        = String.format("file:///home/hasegawa/Dropbox/Trade/Report/EOD_STOCK_HISTORY_%s.ods", TIMESTAMP);

	private static void generateReport(List<StockHistory> stockHistoryList) {
		LocalDate today = LocalDate.now();
		
		Map<String, List<StockHistory>> stockHistoryMap = UpdateStockHistory.getStockHistoryMap(stockHistoryList);
		{
			Map<String, List<StockHistory>> newMap = new TreeMap<>();
			for(Map.Entry<String, List<StockHistory>> entry: stockHistoryMap.entrySet()) {
				String symbol = entry.getKey();				
				
				List<StockHistory> list = entry.getValue();
				StockHistory last = list.get(list.size() - 1);
				// Skip if the stock is sold all quantity.
				if (last.totalQuantity == 0) continue;
				
				List<StockHistory> newList = new ArrayList<>();
				for(StockHistory stockHistory: list) {
					if (stockHistory.session == last.session) newList.add(stockHistory);
				}
				
				newMap.put(symbol, newList);
			}
			stockHistoryMap = newMap;
		}
		try (
			SpreadSheet docLoad = new SpreadSheet(URL_TEMPLATE, true);
			SpreadSheet docSave = new SpreadSheet()) {
		
			// Build stockHistory sheet
			{
				List<StockHistorySheet> stockHistorySheetList = new ArrayList<>();
				
				for(Map.Entry<String, List<StockHistory>> entry: stockHistoryMap.entrySet()) {
					for(StockHistory stockHistory: entry.getValue()) {
						String symbol   = stockHistory.symbol;
						String date     = stockHistory.date;
						Double quantity = stockHistory.totalQuantity;
						Double cost     = stockHistory.totalCost;
						Double value    = null;
						Double dividend = stockHistory.totalDividend;
						Double profit   = null;
						
						if (PriceUtil.contains(symbol, date)) {
							double closePrice = PriceUtil.getClose(symbol, date);
							value = Transaction.roundPrice(stockHistory.totalQuantity * closePrice);
							profit = Transaction.roundPrice(value - cost);
						}
						
						stockHistorySheetList.add(new StockHistorySheet(symbol, date, quantity, cost, value, dividend, profit));
					}
					{
						List<StockHistory> list = entry.getValue();
						StockHistory stockHistory = list.get(list.size() - 1);
						
						String symbol   = stockHistory.symbol;
						String date     = today.toString();
						Double quantity = stockHistory.totalQuantity;
						Double cost     = stockHistory.totalCost;
						Double value    = stockHistory.totalValue;
						Double dividend = stockHistory.totalDividend;
						Double profit   = stockHistory.totalProfit;
						
						double closePrice = PriceUtil.getClose(symbol, date);
						value = Transaction.roundPrice(stockHistory.totalQuantity * closePrice);
						profit = Transaction.roundPrice(value - cost);
						
						stockHistorySheetList.add(new StockHistorySheet(symbol, date, quantity, cost, value, dividend, profit));
					}
					stockHistorySheetList.add(new StockHistorySheet());
				}

				{
					String sheetName = Sheet.getSheetName(StockHistorySheet.class);
					docSave.importSheet(docLoad, sheetName, docSave.getSheetCount());
					Sheet.fillSheet(docSave, stockHistorySheetList);
				}
			}
	
			// remove first sheet
			docSave.removeSheet(docSave.getSheetName(0));
			docSave.store(URL_REPORT);
			logger.info("output {}", URL_REPORT);
		}
	}

	public static void main(String[] args) {
		logger.info("START");
		
		List<StockHistory>stockHistoryList = UpdateStockHistory.getStockHistoryList();
		logger.info("stockHistoryList = {}", stockHistoryList.size());

		generateReport(stockHistoryList);

		logger.info("STOP");
		System.exit(0);
	}
}
