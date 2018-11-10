package yokwe.finance.stock.report;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.stock.data.StockHistory;
import yokwe.finance.stock.libreoffice.Sheet;
import yokwe.finance.stock.libreoffice.SpreadSheet;
import yokwe.finance.stock.util.DoubleUtil;

public class Report {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Report.class);
	
	public static final boolean MODE_TEST = false;
	
	public static final String TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());

	public static final String URL_TEMPLATE      = "file:///home/hasegawa/Dropbox/Trade/TEMPLATE_STOCK_REPORT.ods";
	public static final String URL_REPORT        = String.format("file:///home/hasegawa/Dropbox/Trade/Report/STOCK_REPORT_%s.ods", TIMESTAMP);

	private static void generateReportStockHistory(SpreadSheet docLoad, SpreadSheet docSave, String prefix, List<Transaction> transactionList) {
		Collection<List<StockHistory>> collectionList = UpdateStockHistory.filter(UpdateStockHistory.getStockHistoryListWithDividend(transactionList), true, true);
		
		List<StockHistory> stockHistoryList = new ArrayList<>();
		
		for(List<StockHistory> list: collectionList) {
			stockHistoryList.addAll(list);
			stockHistoryList.add(new StockHistory());
		}
		
		String sheetName = Sheet.getSheetName(StockHistory.class);
		docSave.importSheet(docLoad, sheetName, docSave.getSheetCount());
		Sheet.fillSheet(docSave, stockHistoryList);
		
		String newSheetName = String.format("%s-%s", prefix, sheetName);
		logger.info("sheet {}", newSheetName);
		docSave.renameSheet(sheetName, newSheetName);			
	}
	
	private static void generateReportTransfer(SpreadSheet docLoad, SpreadSheet docSave, String prefix, List<Transaction> transactionList) {
		Collection<List<StockHistory>> collectionList = UpdateStockHistory.filter(UpdateStockHistory.getStockHistoryListWithoutDividend(transactionList), true, false);

		List<Transfer> transferList = new ArrayList<>();
		for(List<StockHistory> stockHistoryList: collectionList) {
			for(StockHistory stockHistory: stockHistoryList) {
				final Transfer transfer;
				if (stockHistory.buyQuantity != 0 && stockHistory.sellQuantity == 0) {
					double buyPrice = DoubleUtil.roundQuantity((stockHistory.buy - stockHistory.buyFee) / stockHistory.buyQuantity);
					double averagePrice = DoubleUtil.roundQuantity(stockHistory.totalCost / stockHistory.totalQuantity);
					transfer = Transfer.buy(stockHistory.symbol, stockHistory.date,
							stockHistory.buyQuantity, buyPrice, stockHistory.buyFee, stockHistory.buy,
							stockHistory.totalQuantity, stockHistory.totalCost, averagePrice);
				} else if (stockHistory.buyQuantity == 0 && stockHistory.sellQuantity != 0) {
					double sellPrice = DoubleUtil.roundQuantity((stockHistory.sell + stockHistory.sellFee) / stockHistory.sellQuantity);
					transfer = Transfer.sell(stockHistory.symbol, stockHistory.date,
							stockHistory.sellQuantity, sellPrice, stockHistory.sellFee, stockHistory.sell, stockHistory.sellCost, stockHistory.sellProfit);
				} else if (stockHistory.buyQuantity != 0 && stockHistory.sellQuantity != 0) {
					logger.debug("stockHistory {}", stockHistory);
					double buyPrice = DoubleUtil.roundQuantity((stockHistory.buy - stockHistory.buyFee) / stockHistory.buyQuantity);
					// Calculate averagePrice before sell
					double averagePrice = DoubleUtil.roundPrice((stockHistory.totalCost + stockHistory.sell) / (stockHistory.totalQuantity + stockHistory.sellQuantity));					
					double sellPrice = DoubleUtil.roundQuantity((stockHistory.sell - stockHistory.sellFee) / stockHistory.sellQuantity);
					transfer = Transfer.buySell(stockHistory.symbol, stockHistory.date,
							stockHistory.buyQuantity, buyPrice, stockHistory.buyFee, stockHistory.buy,
							stockHistory.totalQuantity, stockHistory.totalCost, averagePrice,
							stockHistory.date, stockHistory.sellQuantity, sellPrice, stockHistory.sellFee, stockHistory.sell, stockHistory.sellCost, stockHistory.sellProfit);
				} else {
//					logger.error("Unexpected  {}", stockHistory);
//					throw new UnexpectedException("Unexpected");
					transfer = null;
					logger.warn("  {}", stockHistory);
				}
				if (transfer != null) transferList.add(transfer);
			}
			transferList.add(new Transfer());
		}
		
		String sheetName = Sheet.getSheetName(Transfer.class);
		docSave.importSheet(docLoad, sheetName, docSave.getSheetCount());
		Sheet.fillSheet(docSave, transferList);
		
		String newSheetName = String.format("%s-%s", prefix, sheetName);
		logger.info("sheet {}", newSheetName);
		docSave.renameSheet(sheetName, newSheetName);						
	}
	
	private static void generateReport(SpreadSheet docLoad, SpreadSheet docSave, String prefix, List<Transaction> transactionList) {
		generateReportStockHistory(docLoad, docSave, prefix, transactionList);
		generateReportTransfer(docLoad, docSave, prefix, transactionList);
	}
	
	public static void generateReport() {
		try (SpreadSheet docLoad = new SpreadSheet(URL_TEMPLATE, true)) {
			SpreadSheet docSave = new SpreadSheet();
			
			generateReport(docLoad, docSave, "monex",     Transaction.getMonex());
			generateReport(docLoad, docSave, "firstrade", Transaction.getFirstrade());

			// remove first sheet
			docSave.removeSheet(docSave.getSheetName(0));

			docSave.store(URL_REPORT);
			logger.info("output {}", URL_REPORT);
		}
	}
	
	public static void main(String[] args) {
		logger.info("START");
		
		generateReport();

		logger.info("STOP");
		System.exit(0);
	}
}
