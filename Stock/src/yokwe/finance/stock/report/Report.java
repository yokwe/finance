package yokwe.finance.stock.report;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.stock.data.StockHistory;
import yokwe.finance.stock.libreoffice.Sheet;
import yokwe.finance.stock.libreoffice.SpreadSheet;

public class Report {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Report.class);
	
	public static final boolean MODE_TEST = false;
	
	public static final String TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());

	public static final String URL_TEMPLATE      = "file:///home/hasegawa/Dropbox/Trade/TEMPLATE_STOCK_REPORT.ods";
	public static final String URL_REPORT        = String.format("file:///home/hasegawa/Dropbox/Trade/Report/STOCK_REPORT_%s.ods", TIMESTAMP);

	public static void generateReport() {
		try (SpreadSheet docLoad = new SpreadSheet(URL_TEMPLATE, true)) {
			SpreadSheet docSave = new SpreadSheet();
			
			{
				List<StockHistory> stockHistoryList = new ArrayList<>();
				int lastSession = -1;
				for(StockHistory stockHistory: UpdateStockHistory.getStockHistoryList(Transaction.getMonex())) {
					if (stockHistory.session != lastSession) {
						lastSession = stockHistory.session;
						if (stockHistoryList.size() != 0) {
							stockHistoryList.add(new StockHistory());
						}
					}
					stockHistoryList.add(stockHistory);
				}
				
				String sheetName = Sheet.getSheetName(StockHistory.class);
				docSave.importSheet(docLoad, sheetName, docSave.getSheetCount());
				Sheet.fillSheet(docSave, stockHistoryList);
				
				String newSheetName = String.format("%s-%s",  sheetName, "monex");
				logger.info("sheet {}", newSheetName);
				docSave.renameSheet(sheetName, newSheetName);
			}

			{
				List<StockHistory> stockHistoryList = new ArrayList<>();
				int lastSession = -1;
				for(StockHistory stockHistory: UpdateStockHistory.getStockHistoryList(Transaction.getFirstrade())) {
					if (stockHistory.session != lastSession) {
						lastSession = stockHistory.session;
						if (stockHistoryList.size() != 0) {
							stockHistoryList.add(new StockHistory());
						}
					}
					stockHistoryList.add(stockHistory);
				}
				
				String sheetName = Sheet.getSheetName(StockHistory.class);
				docSave.importSheet(docLoad, sheetName, docSave.getSheetCount());
				Sheet.fillSheet(docSave, stockHistoryList);
				
				String newSheetName = String.format("%s-%s",  sheetName, "firstrade");
				logger.info("sheet {}", newSheetName);
				docSave.renameSheet(sheetName, newSheetName);
			}
			
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
