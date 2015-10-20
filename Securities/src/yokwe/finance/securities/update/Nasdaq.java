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
	
	private static Map<String, String> exchangeMap = new TreeMap<>();
	static {
		exchangeMap.put("A", "NYSEMKT");
		exchangeMap.put("N", "NYSE");
		exchangeMap.put("P", "NYSEARCA");
		exchangeMap.put("Q", "NASDAQ");
		exchangeMap.put("Z", "BATS");
	}
	
	private static final String HEADER           = "Nasdaq Traded|Symbol|Security Name|Listing Exchange|Market Category|ETF|Round Lot Size|Test Issue|Financial Status|CQS Symbol|NASDAQ Symbol";
	private static final int    NUMBER_OF_FIELDS = 11;

	private static final String CRLF = "\r\n";

	public static void save(String dirPath, String csvPath) {
		File root = new File(dirPath);
		if (!root.isDirectory()) {
			logger.error("Not directory  path = {}", dirPath);
			throw new SecuritiesException("not directory");
		}
		
		File[] fileList = root.listFiles();
		Arrays.sort(fileList, (a, b) -> a.getName().compareTo(b.getName()));
		
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(csvPath), BUFFER_SIZE)) {
			
			int totalSize = 0;
			for(File file: fileList) {
				if (file.length() == 0) continue;
				
				String fileName = file.getName();
				String symbol = fileName.substring(0, fileName.length() - 4);
				
				int size = 0;
				try (BufferedReader br = new BufferedReader(new FileReader(file), BUFFER_SIZE)) {
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
						
						String[] fields = line.split("|");
						if (fields.length != NUMBER_OF_FIELDS) {
							logger.error("fields  {}  line = {}", symbol, line);
							throw new SecuritiesException("fields");
						}
						
						final String traded      = fields[0];
						final String actSymbol   = fields[1];
						final String name        = fields[2];
						final String exchange    = fields[3];
						final String category    = fields[4];
						final String etf         = fields[5];
						final String lotSize     = fields[6];
						final String test        = fields[7];
						final String status      = fields[8];
						final String cqsSymbol   = fields[9];
						final String nasdqSymbol = fields[10];
						
						if (traded.equals("Y") && status.equals("N") && test.equals("N")) {
							final String quatedName = name.contains("\"") ? ("\"" + name + "\"") : name;
							
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
							
							Matcher matcherNormal     = Pattern.compile("^([A-Z]+)$").matcher("");
							Matcher matcherUnit       = Pattern.compile("^([A-Z]+)=$").matcher("");
							Matcher matcherPreferred  = Pattern.compile("^([A-Z]+)-$").matcher("");
							Matcher matcherCalled     = Pattern.compile("^([A-Z]+)\\*$").matcher("");
							Matcher matcherWhenIssued = Pattern.compile("^([A-Z]+)#$").matcher("");
							Matcher matcherWarrants   = Pattern.compile("^([A-Z]+)\\+$").matcher("");
							Matcher matcherPreferredClass = Pattern.compile("^([A-Z]+)-([A-Z])$").matcher("");
							Matcher matcherPreferredClassCalled = Pattern.compile("^([A-Z]+)-([A-Z])\\*$").matcher("");
							Matcher matcherClass         = Pattern.compile("^([A-Z]+)\\.([A-Z])$").matcher("");
							Matcher matcherWarrantsClass = Pattern.compile("^([A-Z]+)\\+([A-Z])$").matcher("");
							
							
							

							
							bw.append(CRLF);
						}

						size++;
					}
				}
				totalSize += size;
				logger.info(String.format("%-6s %6d", symbol, size));
			}
			
			logger.info("TOTAL {}", totalSize);
		} catch (IOException e) {
			logger.error("IOException {}", e);
			throw new SecuritiesException("IOException");
		}
	}
	public static void main(String[] args) {
		String dirPath = args[0];	
		String csvPath = args[1];
		
		logger.info("dirPath = {}", dirPath);
		logger.info("csvPath = {}", csvPath);
		save(dirPath, csvPath);
	}
}
