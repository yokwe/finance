package yokwe.finance.securities.eod;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
	public String symbol;
	@ColumnName("name")
	public String name;
	
	// last values
	@ColumnName("date")
	public String date;
	@ColumnName("price")
	public double price;
	
	// price
	@ColumnName("pricec")
	public int    pricec;
	
	@ColumnName("sd")
	@NumberFormat("#,##0.0000")
	public double sd;
//	@ColumnName("hv")
	public double hv;
//	@ColumnName("rsi")
	public double rsi;

//	@ColumnName("min")
	public double min;
//	@ColumnName("max")
	public double max;
//	@ColumnName("minpct")
	public double minpct;
//	@ColumnName("maxpct")
	public double maxpct;

	// dividend
//	@ColumnName("div")
	public double div;
//	@ColumnName("divc")
	public int    divc;
//	@ColumnName("yield")
	public double yield;
	
	// volume
//	@ColumnName("vol")
	public long   vol;
//	@ColumnName("vol5")
	public long   vol5;
//	@ColumnName("vol30")
	public long   vol30;
	
	// price change detection
//	@ColumnName("sma5")
	public double sma5;
//	@ColumnName("sma20")
	public double sma20;
//	@ColumnName("sma50")
	public double sma50;
//	@ColumnName("sma200")
	public double sma200;
	
//	@ColumnName("sma5pct")
	public double sma5pct;
//	@ColumnName("sma20pct")
	public double sma20pct;
//	@ColumnName("sma50pct")
	public double sma50pct;
//	@ColumnName("sma200pct")
	public double sma200pct;

//	@ColumnName("last")
	public double last;
//	@ColumnName("lastpct")
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
		List<Stats> statsList = new ArrayList<>();
		Stats stats = new Stats();
		stats.symbol = "XYZZY";
		stats.name   = "ZORK";
		stats.date   = "2123-45-67";
		stats.price  = 123.4;
		stats.pricec = 250;
		stats.sd     = 0.01234;
		statsList.add(stats);
		docSave.importSheet(docLoad, sheetName, docSave.getSheetCount());
		Sheet.saveSheet(docSave, Stats.class, statsList);

		// remove first sheet
		docSave.removeSheet(docSave.getSheetName(0));

		docSave.store(urlReport);
		docLoad.close();

		logger.info("STOP");
		System.exit(0);
	}
}
