package yokwe.finance.securities.update;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
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

	private static final String CRLF = "\r\n";
	
	//  # NASDAQ                                NASDAQ   YAHOO     GOOGLE
	//  # X     X                               IBM      IBM       IBM
	//  # X=    X units                         GRP=     GRP-U     GRP.UN
	//  # X-    X preferred                     AA-      AA-P      AA-
	//  # X*    X called                        ARY*     ARY-CL    ARY.CL
	//  # X#    X when issued                   HPE#     HPE-WI    HPE*
	//  # X+    X warrants                      AIG+     AIG-WT    ???
	//  # X-A   X preferred class A             ABR-A    ABR-PA    ABR-A
	//  # X-A*  X preferred class A called      BIR-A*   BIR-PA.A  BIR-A.CL
	//  # X.A   X class A                       AKO.A    AKO-A     AKO.A
	//  # X+A   X warrants class A              GM+A     GM-WTA    ???
	
	private static Matcher MAT_NORMAL            = Pattern.compile("^([A-Z]+)$").matcher("");
	private static Matcher MAT_UNIT              = Pattern.compile("^([A-Z]+)=$").matcher("");
	private static Matcher MAT_PREF              = Pattern.compile("^([A-Z]+)-$").matcher("");
	private static Matcher MAT_CALL              = Pattern.compile("^([A-Z]+)\\*$").matcher("");
	private static Matcher MAT_WHEN_ISSUED       = Pattern.compile("^([A-Z]+)#$").matcher("");
	private static Matcher MAT_WAR               = Pattern.compile("^([A-Z]+)\\+$").matcher("");
	private static Matcher MAT_PREF_CLASS        = Pattern.compile("^([A-Z]+)-([A-Z])$").matcher("");
	private static Matcher MAT_PREF_CLASS_CALLED = Pattern.compile("^([A-Z]+)-([A-Z])\\*$").matcher("");
	private static Matcher MAT_CLASS             = Pattern.compile("^([A-Z]+)\\.([A-Z])$").matcher("");
	private static Matcher MAT_WAR_CLASS         = Pattern.compile("^([A-Z]+)\\+([A-Z])$").matcher("");
	
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
				
				String traded       = fields[0];
				String actSymbol    = fields[1];
				String name         = fields[2];
				String exch         = fields[3];
				String category     = fields[4];
				String etf          = fields[5];
				String lotSize      = fields[6];
				String test         = fields[7];
				String status       = fields[8];
				String cqsSymbol    = fields[9];
				String nasdaqSymbol = fields[10];
				
				if (status.length() == 0) status = "N";

				if (traded.equals("Y") && status.equals("N") && test.equals("N")) {
					name = name.replace("\"", "\"\"");
					if (name.contains(",")) {
						name = "\"" + name + "\"";
					}
					if (!exchNameMap.containsKey(exch)) {
						logger.error("exch = {}", exch);
						throw new SecuritiesException("exch");
					}
					String exchName = exchNameMap.get(exch);
					
					
					bw.append(etf);
					bw.append(",").append(exchName);
					bw.append(",").append(nasdaqSymbol);
					bw.append(",").append(name);
					bw.append(CRLF);
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
