package yokwe.finance.securities.update;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public final class Nasdaq {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(Nasdaq.class);
	
	private static final int BUFFER_SIZE = 256 * 1024;
	
	private static Map<String, String> exchNameMap = new TreeMap<>();
	static {
		exchNameMap.put("A", "NYSEMKT");
		exchNameMap.put("N", "NYSE");
		exchNameMap.put("P", "NYSEARCA");
		exchNameMap.put("Q", "NASDAQ");
		exchNameMap.put("Z", "BATS");
	}
	
	private static final String HEADER           = "Nasdaq Traded|Symbol|Security Name|Listing Exchange|Market Category|ETF|Round Lot Size|Test Issue|Financial Status|CQS Symbol|NASDAQ Symbol";
	private static final int    NUMBER_OF_FIELDS = 11;

	private static final String NEWLINE = "\n";
	
	//  # NASDAQ                                NASDAQ   YAHOO     GOOGLE    NASDAQ-WEB
	//  # X     X                               IBM      IBM       IBM       IBM
	//  # X=    X units                         GRP=     GRP-U     GRP.UN    GRP.U
	//  # X-    X preferred                     AA-      AA-P      AA-       AA^
	//  # X*    X called                        ARY*     ARY-CL    ARY.CL    ARY.CL
	//  # X#    X when issued                   HPE#     HPE-WI    HPE*      HPE.WI
	//  # X$    X when issued2                  AC$      AC-WD     AC.WD     AC.WD
	//  # X+    X warrants                      AIG+     AIG-WT    ???       AIG.WS
	//  # X-A   X preferred class A             ABR-A    ABR-PA    ABR-A     ABR^A
	//  # X-A*  X preferred class A called      BIR-A*   BIR-PA.A  BIR-A.CL  BIR^A.CL
	//  # X.A   X class A                       AKO.A    AKO-A     AKO.A     AKO.A
	//  # X+A   X warrants class A              GM+A     GM-WTA    ???       GM.WS.A
	
	//  DRI$ became DRI
	
	private static String PAT_NORMAL            = "^([A-Z]+)$";
	private static String PAT_UNIT              = "^([A-Z]+)=$";
	private static String PAT_PREF              = "^([A-Z]+)-$";
	private static String PAT_CALL              = "^([A-Z]+)\\*$";
	private static String PAT_WHEN_ISSUED       = "^([A-Z]+)#$";
	private static String PAT_WHEN_ISSUED2      = "^([A-Z]+)\\$$";
	private static String PAT_WAR               = "^([A-Z]+)\\+$";
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
		baseConverter.addConverter1("^([A-Z]+).*$", "%s");
		
		//
		yahooConverter.addConverter1(PAT_NORMAL,            "%s");       // Normal
		yahooConverter.addConverter1(PAT_UNIT,              "%s-U");     // Units
		yahooConverter.addConverter1(PAT_PREF,              "%s-P");     // Preferred
		yahooConverter.addConverter1(PAT_CALL,              "%s-CL");    // Called
		yahooConverter.addConverter1(PAT_WHEN_ISSUED,       "%s-WI");    // When Issued
		yahooConverter.addConverter1(PAT_WHEN_ISSUED2,      "%s-WD");    // When Issued2
		yahooConverter.addConverter1(PAT_WAR,               "%s-WT");    // Warrants
		
		yahooConverter.addConverter2(PAT_PREF_CLASS,        "%s-P%s");   // Preferred class A
		yahooConverter.addConverter2(PAT_PREF_CLASS_CALLED, "%s-P%s.A"); // Preferred class A called
		yahooConverter.addConverter2(PAT_CLASS,             "%s-%s");    //           class A
		yahooConverter.addConverter2(PAT_WAR_CLASS,         "%s-WT%s");  // Warrants  class A
		
		//
		googleConverter.addConverter1(PAT_NORMAL,            "%s");       // Normal
		googleConverter.addConverter1(PAT_UNIT,              "%s.UN");    // Units
		googleConverter.addConverter1(PAT_PREF,              "%s-");      // Preferred
		googleConverter.addConverter1(PAT_CALL,              "%s.CL");    // Called
		googleConverter.addConverter1(PAT_WHEN_ISSUED,       "%s*");      // When Issued
		googleConverter.addConverter1(PAT_WHEN_ISSUED2,      "%s.WD");    // When Issued2
		googleConverter.addConverter1(PAT_WAR,               "???");      // Warrants -- UNKNOWN
		
		googleConverter.addConverter2(PAT_PREF_CLASS,        "%s-%s");    // Preferred class A
		googleConverter.addConverter2(PAT_PREF_CLASS_CALLED, "%s-%s.CL"); // Preferred class A called
		googleConverter.addConverter2(PAT_CLASS,             "%s.%s");    //           class A
		googleConverter.addConverter2(PAT_WAR_CLASS,         "???");      // Warrants  class A -- UNKNOWN
		
		//
		nasdaqConverter.addConverter1(PAT_NORMAL,            "%s");       // Normal
		nasdaqConverter.addConverter1(PAT_UNIT,              "%s.U");     // Units
		nasdaqConverter.addConverter1(PAT_PREF,              "%s^");      // Preferred
		nasdaqConverter.addConverter1(PAT_CALL,              "%s.CL");    // Called
		nasdaqConverter.addConverter1(PAT_WHEN_ISSUED,       "%s.WI");    // When Issued
		nasdaqConverter.addConverter1(PAT_WHEN_ISSUED2,      "%s.WD");    // When Issued2
		nasdaqConverter.addConverter1(PAT_WAR,               "%s.WS");    // Warrants
		
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
	
	public static void save(String filePath, String csvPath) {
		File file = new File(filePath);
		if (!file.isFile()) {
			logger.error("Not file  path = {}", filePath);
			throw new SecuritiesException("not file");
		}
		
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(csvPath), BUFFER_SIZE);
			BufferedReader br = new BufferedReader(new FileReader(file), BUFFER_SIZE)) {
			String header = br.readLine();
			if (header == null) {
				logger.error("{} header == null", file.getAbsolutePath());
				throw new SecuritiesException("not directory");
			}
			if (!header.equals(HEADER)) {
				logger.error("header = {}", header);
				throw new SecuritiesException("header");
			}

			for(;;) {
				String line = br.readLine();
				if (line == null) break;
				if (line.startsWith("File Creation Time")) continue;
				
				String[] fields = line.split("\\|");
				if (fields.length != NUMBER_OF_FIELDS) {
					logger.error("number of fields {}  line = {}", fields.length, line);
					throw new SecuritiesException("fields");
				}
				
				String traded    = fields[0];
//				String actSymbol = fields[1];
				String name      = fields[2];
				String exch      = fields[3];
//				String category  = fields[4];
				String etf       = fields[5];
//				String lotSize   = fields[6];
				String test      = fields[7];
				String status    = fields[8];
//				String cqsSymbol = fields[9];
				String symbol    = fields[10]; // nasdaq symbol
				
				if (status.length() == 0) {
					status = "N";
				}
				
				// D = Deficient: Issuer Failed to Meet NASDAQ Continued Listing Requirements
				// E = Delinquent: Issuer Missed Regulatory Filing Deadline
				// Q = Bankrupt: Issuer Has Filed for Bankruptcy
				if (status.equals("Q")) continue;  // Skip Bankruptcy record
				// N = Normal (Default): Issuer Is NOT Deficient, Delinquent, or Bankrupt.
				// G = Deficient and Bankrupt
				if (status.equals("G")) continue;  // Skip Bankruptcy record
				// H = Deficient and Delinquent
				// J = Delinquent and Bankrupt
				if (status.equals("J")) continue;  // Skip Bankruptcy record
				// K = Deficient, Delinquent, and Bankrupt
				if (status.equals("K")) continue;  // Skip Bankruptcy record
				
				if (traded.equals("Y") && test.equals("N")) {
					// name
					name = name.replace("\"", "\"\"");
					if (name.contains(",")) {
						name = "\"" + name + "\"";
					}
					// exchName
					if (!exchNameMap.containsKey(exch)) {
						logger.error("exch = {}", exch);
						throw new SecuritiesException("exch");
					}
					String exchName = exchNameMap.get(exch);
					
					String baseSymbol   = baseConverter.convert(symbol);
					String yahooSymbol  = yahooConverter.convert(symbol);
					String googleSymbol = googleConverter.convert(symbol);
					String nasdaqSymbol = nasdaqConverter.convert(symbol);
					
					bw.append(etf);
					bw.append(",").append(exchName);
					bw.append(",").append(symbol);
					bw.append(",").append(baseSymbol);
					bw.append(",").append(yahooSymbol);
					bw.append(",").append(googleSymbol);
					bw.append(",").append(nasdaqSymbol);
					bw.append(",").append(name);
					bw.append(NEWLINE);
				}
			}
		} catch (IOException e) {
			logger.error("IOException {}", e);
			throw new SecuritiesException("IOException");
		}
	}
	public static void main(String[] args) {
		String filePath = args[0];	
		String csvPath  = args[1];
		
		logger.info("filePath = {}", filePath);
		logger.info("csvPath  = {}", csvPath);
		save(filePath, csvPath);
	}
}
