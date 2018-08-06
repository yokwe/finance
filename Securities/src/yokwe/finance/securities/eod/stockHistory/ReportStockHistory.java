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
import yokwe.finance.securities.eod.StockDividend;
import yokwe.finance.securities.eod.StockDividend.PayDiv;
import yokwe.finance.securities.eod.StockHistory;
import yokwe.finance.securities.eod.UpdateStockDividend;
import yokwe.finance.securities.eod.UpdateStockHistory;
import yokwe.finance.securities.eod.tax.Transaction;
import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;
import yokwe.finance.securities.util.DoubleUtil;

public class ReportStockHistory {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ReportStockHistory.class);
	
	public static final String TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());

	public static final String URL_TEMPLATE      = "file:///home/hasegawa/Dropbox/Trade/TEMPLATE_EOD_STOCK_HISTORY.ods";
	public static final String URL_REPORT        = String.format("file:///home/hasegawa/Dropbox/Trade/Report/EOD_STOCK_HISTORY_%s.ods", TIMESTAMP);
	
	private static final LocalDate TODAY = LocalDate.now();

	private static void generateReport(List<StockHistory> stockHistoryList) {
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
						String date     = TODAY.toString();
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
			
			// Build dividend sheet
			{
				List<DividendSheet> dividendSheetList = new ArrayList<>();
				
				{
					List<StockDividend> stockDividendList = UpdateStockDividend.getStockDividendList();
					logger.info("stockDividendList {}", stockDividendList.size());

					for(StockDividend stockDividend: stockDividendList) {
						DividendSheet dividendSheet = new DividendSheet();
						
						dividendSheet.symbol   = stockDividend.symbol;
						dividendSheet.quantity = stockDividend.quantity;
						dividendSheet.cost     = stockDividend.cost;
//						dividendSheet.interest = DoubleUtil.round(stockDividend.div / stockDividend.cost, 4);
						dividendSheet.interest = (double)0;

						if (stockDividend.payDivMap.containsKey(1)) {
							PayDiv payDiv = stockDividend.payDivMap.get(1);
							dividendSheet.pay1 = payDiv.pay.toString();
							dividendSheet.div1 = payDiv.div;
						}

						if (stockDividend.payDivMap.containsKey(2)) {
							PayDiv payDiv = stockDividend.payDivMap.get(2);
							dividendSheet.pay2 = payDiv.pay.toString();
							dividendSheet.div2 = payDiv.div;
						}

						if (stockDividend.payDivMap.containsKey(3)) {
							PayDiv payDiv = stockDividend.payDivMap.get(3);
							dividendSheet.pay3 = payDiv.pay.toString();
							dividendSheet.div3 = payDiv.div;
						}

						if (stockDividend.payDivMap.containsKey(4)) {
							PayDiv payDiv = stockDividend.payDivMap.get(4);
							dividendSheet.pay4 = payDiv.pay.toString();
							dividendSheet.div4 = payDiv.div;
						}

						if (stockDividend.payDivMap.containsKey(5)) {
							PayDiv payDiv = stockDividend.payDivMap.get(5);
							dividendSheet.pay5 = payDiv.pay.toString();
							dividendSheet.div5 = payDiv.div;
						}

						if (stockDividend.payDivMap.containsKey(6)) {
							PayDiv payDiv = stockDividend.payDivMap.get(6);
							dividendSheet.pay6 = payDiv.pay.toString();
							dividendSheet.div6 = payDiv.div;
						}

						if (stockDividend.payDivMap.containsKey(7)) {
							PayDiv payDiv = stockDividend.payDivMap.get(7);
							dividendSheet.pay7 = payDiv.pay.toString();
							dividendSheet.div7 = payDiv.div;
						}

						if (stockDividend.payDivMap.containsKey(8)) {
							PayDiv payDiv = stockDividend.payDivMap.get(8);
							dividendSheet.pay8 = payDiv.pay.toString();
							dividendSheet.div8 = payDiv.div;
						}

						if (stockDividend.payDivMap.containsKey(9)) {
							PayDiv payDiv = stockDividend.payDivMap.get(9);
							dividendSheet.pay9 = payDiv.pay.toString();
							dividendSheet.div9 = payDiv.div;
						}

						if (stockDividend.payDivMap.containsKey(10)) {
							PayDiv payDiv = stockDividend.payDivMap.get(10);
							dividendSheet.pay10 = payDiv.pay.toString();
							dividendSheet.div10 = payDiv.div;
						}

						if (stockDividend.payDivMap.containsKey(11)) {
							PayDiv payDiv = stockDividend.payDivMap.get(11);
							dividendSheet.pay11 = payDiv.pay.toString();
							dividendSheet.div11 = payDiv.div;
						}

						if (stockDividend.payDivMap.containsKey(12)) {
							PayDiv payDiv = stockDividend.payDivMap.get(12);
							dividendSheet.pay12 = payDiv.pay.toString();
							dividendSheet.div12 = payDiv.div;
						}
						
						dividendSheetList.add(dividendSheet);
					}
					logger.info("dividendSheetList {}", dividendSheetList.size());
				}

				{
					String sheetName = Sheet.getSheetName(DividendSheet.class);
					docSave.importSheet(docLoad, sheetName, docSave.getSheetCount());
					Sheet.fillSheet(docSave, dividendSheetList);
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
