package yokwe.finance.securities.eod;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;
import yokwe.finance.securities.util.CSVUtil;

@Sheet.SheetName("stats")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class Stats extends Sheet {
//	public String exchange;
	@ColumnName("symbol")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public String symbol;
	
	@ColumnName("name")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public String name;
	
	// last values
	@ColumnName("date")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public String date;
	
	@ColumnName("price")
	@NumberFormat(SpreadSheet.FORMAT_PRICE)
	public double price;
	
	// price
	@ColumnName("pricec")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public int    pricec;
	
	@ColumnName("sd")
	@NumberFormat("#,##0.0000")
	public double sd;
	
	@ColumnName("hv")
	@NumberFormat("#,##0.0000")
	public double hv;
	
	@ColumnName("rsi")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public double rsi;

	@ColumnName("min")
	@NumberFormat(SpreadSheet.FORMAT_PRICE)
	public double min;
	
	@ColumnName("max")
	@NumberFormat(SpreadSheet.FORMAT_PRICE)
	public double max;
	
	@ColumnName("minpct")
	@NumberFormat(SpreadSheet.FORMAT_PERCENT)
	public double minpct;
	
	@ColumnName("maxpct")
	@NumberFormat(SpreadSheet.FORMAT_PERCENT)
	public double maxpct;

	// dividend
	@ColumnName("div")
	@NumberFormat("#,##0.0000")
	public double div;
	
	@ColumnName("divc")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public int    divc;
	
	@ColumnName("yield")
	@NumberFormat(SpreadSheet.FORMAT_PERCENT)
	public double yield;
	
	// volume
	@ColumnName("vol")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public long   vol;
	
	@ColumnName("vol5")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public long   vol5;
	
	@ColumnName("vol30")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public long   vol30;
	
	// price change detection
	@ColumnName("sma5")
	@NumberFormat(SpreadSheet.FORMAT_PRICE)
	public double sma5;
	
	@ColumnName("sma20")
	@NumberFormat(SpreadSheet.FORMAT_PRICE)
	public double sma20;
	
	@ColumnName("sma50")
	@NumberFormat(SpreadSheet.FORMAT_PRICE)
	public double sma50;
	
	@ColumnName("sma200")
	@NumberFormat(SpreadSheet.FORMAT_PRICE)
	public double sma200;
	
	@ColumnName("sma5pct")
	@NumberFormat(SpreadSheet.FORMAT_PERCENT)
	public double sma5pct;
	
	@ColumnName("sma20pct")
	@NumberFormat(SpreadSheet.FORMAT_PERCENT)
	public double sma20pct;
	
	@ColumnName("sma50pct")
	@NumberFormat(SpreadSheet.FORMAT_PERCENT)
	public double sma50pct;
	
	@ColumnName("sma200pct")
	@NumberFormat(SpreadSheet.FORMAT_PERCENT)
	public double sma200pct;

	@ColumnName("last")
	@NumberFormat(SpreadSheet.FORMAT_PRICE)
	public double last;
	
	@ColumnName("lastpct")
	@NumberFormat(SpreadSheet.FORMAT_PERCENT)
	public double lastpct;
	
	public static void save(List<Stats> statsList) {
		CSVUtil.saveWithHeader(statsList, UpdateStats.PATH_STATS);
	}
	
	public static List<Stats> load() {
		return CSVUtil.loadWithHeader(UpdateStats.PATH_STATS, Stats.class);
	}

	public static void main(String[] args) {
		final org.slf4j.Logger logger = LoggerFactory.getLogger(Stats.class);

		logger.info("START");
		final String timeStamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());

		final String urlTemplate      = "file:///home/hasegawa/Dropbox/Trade/STATS_REPORT_TEMPLATE.ods";
		final String urlReport        = String.format("file:///home/hasegawa/Dropbox/Trade/STATS_REPORT_%s.ods", timeStamp);

		SpreadSheet docLoad = new SpreadSheet(urlTemplate, true);
		SpreadSheet docSave = new SpreadSheet();
		
		String sheetName = "stats";
		List<Stats> statsList = Stats.load();
		docSave.importSheet(docLoad, sheetName, docSave.getSheetCount());
		Sheet.saveStatsSheet(docSave, statsList, sheetName);

		// remove first sheet
		docSave.removeSheet(docSave.getSheetName(0));

		docSave.store(urlReport);
		docLoad.close();

		logger.info("STOP");
		System.exit(0);
	}
}
