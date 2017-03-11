package yokwe.finance.securities.util;

import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;

@Sheet.SheetName("equityStats-header")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class Equity extends Sheet {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Equity.class);
	
	@ColumnName("symbol")
	public String symbol;
	@ColumnName("name")
	public String name;
	@ColumnName("price")
	public double price;
	@ColumnName("sd")
	public double sd;
	@ColumnName("div")
	public double div;
	@ColumnName("freq")
	public double freq;
	@ColumnName("changepct")
	public double changepct;
	@ColumnName("count")
	public int ount;
	@ColumnName("hv")
	public double hv;
	@ColumnName("change")
	public double change;
	@ColumnName("var95")
	public double var95;
	@ColumnName("var99")
	public double var99;
	@ColumnName("rsi")
	public double rsi;
	@ColumnName("price200")
	public double price200;
	@ColumnName("divYield")
	public double divYield;
	@ColumnName("beta")
	public double beta;
	@ColumnName("r2")
	public double r2;
	@ColumnName("vol5")
	public int vol5;
	@ColumnName("min")
	public double min;
	@ColumnName("max")
	public double max;
	@ColumnName("minpct")
	public double minpct;
	@ColumnName("maxpct")
	public double maxpct;
	
	private static final String URL_EQUITY = "file:///home/hasegawa/Dropbox/Trade/equityStats-header.csv";

	// key is date
	private static Map<String, Equity> map = new TreeMap<>();
	
	static {
		logger.info("Start load {}", URL_EQUITY);
		try (SpreadSheet spreadSheet = new SpreadSheet(URL_EQUITY, true)) {
			for(Equity equity: Sheet.extractSheet(spreadSheet, Equity.class)) {
				map.put(equity.symbol, equity);
			}
		}
		logger.info("map {}", map.size());
	}
	
	public static boolean contains(String symbol) {
		String newSymbol = symbol.replace(".PR.", "-");
		return map.containsKey(newSymbol);
	}
	
	public static Equity get(String symbol) {
		// special handling for preferred stock symbol
		String newSymbol = symbol.replace(".PR.", "-");
		Equity equity = map.get(newSymbol);
		if (equity == null) {
			logger.error("no symbol in map  {} - {}", symbol, newSymbol);
			throw new SecuritiesException("no symbol in mapEquity");
		}
		return equity;
	}
}
