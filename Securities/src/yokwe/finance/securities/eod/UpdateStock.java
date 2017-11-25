package yokwe.finance.securities.eod;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.CSVUtil;

public class UpdateStock {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateStock.class);
	
	public static final String PATH_NASDAQ_TRADED       = "tmp/fetch/nasdaq/nasdaqtraded.txt";
	public static final String PATH_COMPANIES_BY_REGION = "tmp/fetch/nasdaq/companies-by-region.csv";
	public static final String PATH_STOCK               = "tmp/eod/stock.csv";
	
	public static class NasdaqTraded {
		public String traded;
		public String actSymbol;
		public String name;
		public String exch;
		public String category;
		public String etf;
		public String lotSize;
		public String test;
		public String status;
		public String cqsSymbol;
		public String symbol;    // TOO-A
		public String dummy;
		
		@Override
		public String toString() {
			return String.format("%s %s", traded, actSymbol);
		}
		
		public static List<NasdaqTraded> load() {
			try (BufferedReader bfr = new BufferedReader(new FileReader(PATH_NASDAQ_TRADED))) {
				StringBuilder content = new StringBuilder();
				for(;;) {
					String line = bfr.readLine();
					if (line == null) break;
					
					// Skip header
					if (line.startsWith("Nasdaq Traded")) continue;
					
					// Skip last line
					if (line.startsWith("File Creation Time")) continue;
					
					content.append(line).append("\n");
				}
				CSVFormat csvFormat = CSVFormat.DEFAULT.withRecordSeparator("\n").withDelimiter('|');
				
				return CSVUtil.load(new StringReader(content.toString()), NasdaqTraded.class, csvFormat);
			} catch (IOException e) {
				logger.error(e.getClass().getName());
				logger.error(e.getMessage());
				throw new SecuritiesException();
			}
		}
	}
	public static class CompaniesByRegion {
		// "Symbol","Name","LastSale","MarketCap","ADR TSO","Country","IPOyear","Sector","Industry","Summary Quote"
		public String symbol;    // TOO^A
		public String name;
		public String lastSale;
		public double marketCap;
		public String adrTSO;
		public String country;
		public String ipoYear;
		public String sector;
		public String industry;
		public String url;
		public String dummy;
		
		@Override
		public String toString() {
			return String.format("%s", symbol);
		}
		
		public static List<CompaniesByRegion> load() {
			try (BufferedReader bfr = new BufferedReader(new FileReader(PATH_COMPANIES_BY_REGION))) {
				StringBuilder content = new StringBuilder();
				for(;;) {
					String line = bfr.readLine();
					if (line == null) break;
					
					// Skip header
					if (line.startsWith("\"Symbol\",")) continue;
					
					// remove trailing ','
//					if (line.charAt(line.length() - 1) == ',') line = line.substring(0, line.length() - 1);
					
					content.append(line).append("\n");
				}
				CSVFormat csvFormat = CSVFormat.DEFAULT.withRecordSeparator("\n").withDelimiter(',').withQuote('"').withTrim();
				
				return CSVUtil.load(new StringReader(content.toString()), CompaniesByRegion.class, csvFormat);
			} catch (IOException e) {
				logger.error(e.getClass().getName());
				logger.error(e.getMessage());
				throw new SecuritiesException();
			}
		}
	}
	
	//
	// exchange name map
	//
	private static Map<String, String> exchNameMap = new TreeMap<>();
	static {
		exchNameMap.put("A", "NYSEMKT");
		exchNameMap.put("N", "NYSE");
		exchNameMap.put("P", "NYSEARCA");
		exchNameMap.put("Q", "NASDAQ");
		exchNameMap.put("Z", "BATS");
	}
	

	
	//
	// Symbol Converter
	//
	
	//  # NASDAQ                                NASDAQ   YAHOO     GOOGLE    NASDAQ-WEB
	//  # X     X                               IBM      IBM       IBM       IBM
	//  # X=    X units                         GRP=     GRP-U     GRP.UN    GRP.U
	//  # X-    X preferred                     AA-      AA-P      AA-       AA^
	//  # X*    X called                        ARY*     ARY-CL    ARY.CL    ARY.CL
	//  # X^    X right                         UTG^     URG-RI    ???       URG~
	//  # X#    X when issued                   HPE#     HPE-WI    HPE*      HPE.WI
	//  # X$    X when issued2                  AC$      AC-WD     AC.WD     AC.WD
	//  # X^#   X when issued3                  UTG^#    UTG-RWI   ???       UTG~$
	//  # X+    X warrants                      AIG+     AIG-WT    ???       AIG.WS
	//  # X-A   X preferred class A             ABR-A    ABR-PA    ABR-A     ABR^A
	//  # X-A*  X preferred class A called      BIR-A*   BIR-PA.A  BIR-A.CL  BIR^A.CL
	//  # X.A   X class A                       AKO.A    AKO-A     AKO.A     AKO.A
	//  # X+A   X warrants class A              GM+A     GM-WTA    ???       GM.WS.A
	//  # X-*   X preferred called              IRET-*   IRET-P    IRET-CL   IRET^.CL
	
	//  DRI$ became DRI
	
	private static String PAT_NORMAL            = "^([A-Z]+)$";
	private static String PAT_UNIT              = "^([A-Z]+)=$";
	private static String PAT_PREF              = "^([A-Z]+)-$";
	private static String PAT_CALL              = "^([A-Z]+)\\*$";
	private static String PAT_RIGHT             = "^([A-Z]+)\\^$";
	private static String PAT_WHEN_ISSUED       = "^([A-Z]+)#$";
	private static String PAT_WHEN_ISSUED2      = "^([A-Z]+)\\$$";
	private static String PAT_WHEN_ISSUED3      = "^([A-Z]+)\\^#$";
	private static String PAT_WAR               = "^([A-Z]+)\\+$";
	private static String PAT_PREF_CALLED       = "^([A-Z]+)-\\*$";
	//
	private static String PAT_PREF_CLASS        = "^([A-Z]+)-([A-Z])$";
	private static String PAT_PREF_CLASS_CALLED = "^([A-Z]+)-([A-Z])\\*$";
	private static String PAT_CLASS             = "^([A-Z]+)\\.([A-Z])$";
	private static String PAT_WAR_CLASS         = "^([A-Z]+)\\+([A-Z])$";
	
	static ConverterList baseConverter   = new ConverterList();
	static ConverterList yahooConverter  = new ConverterList();
	static ConverterList googleConverter = new ConverterList();
	static ConverterList nasdaqConverter = new ConverterList();
	
	static {
		// Remove suffix for called and when issued
		baseConverter.addConverter1("^([^\\*\\#\\$]+)[\\*\\#\\$\\^]{0,2}$", "%s");
		
		//
		yahooConverter.addConverter1(PAT_NORMAL,            "%s");       // Normal
		yahooConverter.addConverter1(PAT_UNIT,              "%s-U");     // Units
		yahooConverter.addConverter1(PAT_PREF,              "%s-P");     // Preferred
		yahooConverter.addConverter1(PAT_CALL,              "%s-CL");    // Called
		yahooConverter.addConverter1(PAT_RIGHT,             "%s-RI");    // Right
		yahooConverter.addConverter1(PAT_WHEN_ISSUED,       "%s-WI");    // When Issued
		yahooConverter.addConverter1(PAT_WHEN_ISSUED2,      "%s-WD");    // When Issued2
		yahooConverter.addConverter1(PAT_WHEN_ISSUED3,      "%s-RWI");   // When Issued3
		yahooConverter.addConverter1(PAT_WAR,               "%s-WT");    // Warrants
		yahooConverter.addConverter1(PAT_PREF_CALLED,       "%s-P");     // Preferred Called
		
		yahooConverter.addConverter2(PAT_PREF_CLASS,        "%s-P%s");   // Preferred class A
		yahooConverter.addConverter2(PAT_PREF_CLASS_CALLED, "%s-P%s.A"); // Preferred class A called
		yahooConverter.addConverter2(PAT_CLASS,             "%s-%s");    //           class A
		yahooConverter.addConverter2(PAT_WAR_CLASS,         "%s-WT%s");  // Warrants  class A
		
		//
		googleConverter.addConverter1(PAT_NORMAL,            "%s");       // Normal
		googleConverter.addConverter1(PAT_UNIT,              "%s.UN");    // Units
		googleConverter.addConverter1(PAT_PREF,              "%s-");      // Preferred
		googleConverter.addConverter1(PAT_CALL,              "%s.CL");    // Called
		googleConverter.addConverter1(PAT_RIGHT,             "???");      // Right
		googleConverter.addConverter1(PAT_WHEN_ISSUED,       "%s*");      // When Issued
		googleConverter.addConverter1(PAT_WHEN_ISSUED2,      "%s.WD");    // When Issued2
		googleConverter.addConverter1(PAT_WHEN_ISSUED3,      "???");      // When Issued3
		googleConverter.addConverter1(PAT_WAR,               "???");      // Warrants -- UNKNOWN
		googleConverter.addConverter1(PAT_PREF_CALLED,       "%s.CL");    // Preferred Called
		
		googleConverter.addConverter2(PAT_PREF_CLASS,        "%s-%s");    // Preferred class A
		googleConverter.addConverter2(PAT_PREF_CLASS_CALLED, "%s-%s.CL"); // Preferred class A called
		googleConverter.addConverter2(PAT_CLASS,             "%s.%s");    //           class A
		googleConverter.addConverter2(PAT_WAR_CLASS,         "???");      // Warrants  class A -- UNKNOWN
		
		//
		nasdaqConverter.addConverter1(PAT_NORMAL,            "%s");       // Normal
		nasdaqConverter.addConverter1(PAT_UNIT,              "%s.U");     // Units
		nasdaqConverter.addConverter1(PAT_PREF,              "%s^");      // Preferred
		nasdaqConverter.addConverter1(PAT_CALL,              "%s.CL");    // Called
		nasdaqConverter.addConverter1(PAT_RIGHT,             "%s^");      // Right
		nasdaqConverter.addConverter1(PAT_WHEN_ISSUED,       "%s.WI");    // When Issued
		nasdaqConverter.addConverter1(PAT_WHEN_ISSUED2,      "%s.WD");    // When Issued2
		nasdaqConverter.addConverter1(PAT_WHEN_ISSUED3,      "%s~$");     // When Issued3
		nasdaqConverter.addConverter1(PAT_WAR,               "%s.WS");    // Warrants
		nasdaqConverter.addConverter1(PAT_PREF_CALLED,       "%s^.CL");   // Preferred Called
		
		nasdaqConverter.addConverter2(PAT_PREF_CLASS,        "%s^%s");    // Preferred class A
		nasdaqConverter.addConverter2(PAT_PREF_CLASS_CALLED, "%s^%s.CL"); // Preferred class A called
		nasdaqConverter.addConverter2(PAT_CLASS,             "%s.%s");    //           class A
		nasdaqConverter.addConverter2(PAT_WAR_CLASS,         "%s.WS.%s"); // Warrants  class A
	}
	
	static class ConverterList {
		protected List<Converter> converterList = new ArrayList<>();
		
		void addConverter1(String pattern, String format) {
			converterList.add(new Converter1(pattern, format));
		}
		void addConverter2(String pattern, String format) {
			converterList.add(new Converter2(pattern, format));
		}
		
		String convert(String string) {
			for(Converter converter: converterList) {
				if (converter.canConvert(string)) return converter.convert(string);
			}
			logger.error("convertString string = {}", string);
			throw new SecuritiesException("convertString");
		}
	}
	
	static class Converter1 extends Converter {
		private final String formatString;
		
		Converter1(String patternString, String formatString) {
			super(patternString, 1);
			this.formatString = formatString;
		}
		protected String convert() {
			return String.format(formatString, matcher.group(1));
		}
	}
	
	static class Converter2 extends Converter {
		private final String formatString;
		
		Converter2(String patternString, String formatString) {
			super(patternString, 2);
			this.formatString = formatString;
		}
		protected String convert() {
			return String.format(formatString, matcher.group(1), matcher.group(2));
		}
	}
	
	static abstract class Converter {
		final protected Matcher matcher;
		final protected int     expectCount;
		
		protected String  string;
		protected int     actualCount;
		
		protected Converter(String patternString, int expectCount) {
			this.matcher     = Pattern.compile(patternString).matcher("");
			this.expectCount = expectCount;
		}
		
		public boolean canConvert(String targetString) {
			string = targetString;
			matcher.reset(string);
			boolean ret = matcher.matches();
			actualCount = matcher.groupCount();
			return ret;
		}
		
		public String convert(String targetString) {
			if (canConvert(targetString)) {
				if (expectCount == actualCount) return convert();
				
				logger.error("covert expectCount =  {}  actualCount = {}", string, expectCount, actualCount);
				throw new SecuritiesException("convert count");
			}
			
			logger.error("convert {}", string);
			throw new SecuritiesException("convert canConvert");
		}
		
		abstract protected String convert();
	}

	
	public static void main(String[] args) {
		logger.info("START");
		// key is nasdaqSymbol;
		Map<String, Stock> stockMap = new TreeMap<>();

		List<NasdaqTraded>      tradedList  = NasdaqTraded.load();
		List<CompaniesByRegion> companyList = CompaniesByRegion.load();
		logger.info("tradedList  {}", tradedList.size());
		logger.info("companyList {}", companyList.size());
		
		for(NasdaqTraded e: tradedList) {
			// STATUS
			// D = Deficient: Issuer Failed to Meet NASDAQ Continued Listing Requirements
			// E = Delinquent: Issuer Missed Regulatory Filing Deadline
			// Q = Bankrupt: Issuer Has Filed for Bankruptcy
			// N = Normal (Default): Issuer Is NOT Deficient, Delinquent, or Bankrupt.
			// G = Deficient and Bankrupt
			// H = Deficient and Delinquent
			// J = Delinquent and Bankrupt
			// K = Deficient, Delinquent, and Bankrupt
			String status = e.status;
			if (status.length() == 0) {
				status = "N";
			}
			if (!status.equals("N")) {
				logger.info("SKIP status {} {}", status, e.symbol);
				continue;
			}
			if (!e.traded.equals("Y")) {
				logger.info("SKIP trade  {} {}", e.traded, e.symbol);
				continue;
			}
			if (!e.test.equals("N")) {
				logger.info("SKIP test   {} {}", e.test, e.symbol);
				continue;
			}
			
			if (!exchNameMap.containsKey(e.exch)) {
				logger.error("exch = {}", e.exch);
				throw new SecuritiesException("exch");
			}

			// name
			String name = e.name;
			name = name.replace("\"", "\"\"");

			Stock stock = new Stock();
			
			stock.symbol       = baseConverter.convert(e.symbol); //
			stock.symbolGoogle = googleConverter.convert(e.symbol);
			stock.symbolNasdaq = nasdaqConverter.convert(e.symbol);
			stock.symbolYahoo  = yahooConverter.convert(e.symbol);
			stock.exchange     = exchNameMap.get(e.exch);
			stock.etf          = e.etf;
			stock.marketCap    = -1;
			stock.name         = name;
			stock.country      = "*NA*";
			stock.industry     = "*NA*";
			stock.sector       = "*NA*";
			
			stockMap.put(nasdaqConverter.convert(e.symbol), stock);
		}
		logger.info("stockMap {}", stockMap.size());
		
		for(CompaniesByRegion e: companyList) {
			if (!stockMap.containsKey(e.symbol)) continue;
			Stock stock = stockMap.get(e.symbol);
			
			stock.marketCap = Math.round(e.marketCap);
			stock.country   = e.country.equals("n/a")  ? "*NA*" : e.country;
			stock.sector    = e.sector.equals("n/a")   ? "*NA*" : e.sector;
			stock.industry  = e.industry.equals("n/a") ? "*NA*" : e.industry;
			
			// Use longer name for stock
			if (stock.name.length() < e.name.length()) {
				stock.name = e.name;
			}
			if (stock.name.contains("&#")) {
				stock.name = stock.name.replaceAll("\\&\\#39\\;",  "'");
				stock.name = stock.name.replaceAll("\\&\\#128\\;", "€");
				stock.name = stock.name.replaceAll("\\&\\#147\\;", "");
			}
		}
		
		List<Stock> stockList = new ArrayList<>(stockMap.values());
		stockList.sort((a, b) -> a.symbol.compareTo(b.symbol));
		logger.info("stockList {}", stockList.size());
		
		Stock.save(stockList);
		
		logger.info("STOP");
	}
}
