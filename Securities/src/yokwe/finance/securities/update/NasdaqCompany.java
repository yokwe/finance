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

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.database.CompanyTable;
import yokwe.finance.securities.database.NasdaqTable;
import yokwe.finance.securities.util.NasdaqUtil;

public final class NasdaqCompany {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(NasdaqCompany.class);
	
	private static final int BUFFER_SIZE = 256 * 1024;
	private static final String NA  = "n/a";
	private static final String ETF = "*ETF*";
	
	// key of nasdaqMap is nasdaq-web-symbol
	private static final Map<String, NasdaqTable>  nasdaqWebSymbolMap  = new TreeMap<>();
	static {
		for(NasdaqTable table: NasdaqUtil.getAll()) {
			nasdaqWebSymbolMap.put(table.nasdaq, table);
		}
	}
	private static final Map<String, String>       lineMap    = new TreeMap<>();
	
	public static final class CSVRecord {
		public static final String HEADER = "\"Symbol\",\"Name\",\"LastSale\",\"MarketCap\",\"ADR TSO\",\"Country\",\"IPOyear\",\"Sector\",\"Industry\",\"Summary Quote\",";
		public static final int NUMBER_OF_FIELDS = 10;
		
		public static void checkHeader(String line) {
			if (!line.equals(HEADER)) {
				logger.error("header  line = {}", line);
				throw new SecuritiesException("header");
			}
		}
		public static CompanyTable toCompanyTable(String line) {
			String[] fields = line.split("\",");
			if (fields.length != NUMBER_OF_FIELDS) {
				logger.error("line = {}", line);
				throw new SecuritiesException("fields");
			}
			String nasdaq     = fields[0].substring(1).trim();
			String name       = fields[1].substring(1).trim();
//			double lastSale   = Double.valueOf(fields[2].substring(1).trim());
			String marketCap  = fields[3].substring(1).trim();
//			String adrTSO     = fields[4].substring(1).trim();
			String country    = fields[5].substring(1).trim();
//			String ip0Year    = fields[6].substring(1).trim();
			String sector     = fields[7].substring(1).trim();
			String industry   = fields[8].substring(1).trim();
//			String url        = fields[9].substring(1).trim();
			
			// Translate nasdaq web symbol to symbol
			NasdaqTable nasdaqTable = nasdaqWebSymbolMap.get(nasdaq);
			if (nasdaqTable == null) {
				logger.warn("Unknown = {}|{}|{}|{}|{}", nasdaq, marketCap, sector, industry, name);
				return null;
			}
			String nasdaqSymbol = nasdaqTable.symbol;
			
			// Handle duplicate symbol
			if (lineMap.containsKey(nasdaqSymbol)) {
				String lineOld = lineMap.get(nasdaqSymbol);
				if (line.equals(lineOld)) {
					return null;
				} else {
					logger.warn("DUPLICATE  old {}  {}", nasdaqSymbol, line);
					logger.warn("DUPLICATE  new {}  {}", nasdaqSymbol, lineMap.get(nasdaqSymbol));
					throw new SecuritiesException("duplicate");
				}
			} else {
				lineMap.put(nasdaqSymbol, line);
			}
			
			// Handle n/a
			if (sector.equals(NA) && industry.equals(NA)) {
				sector = industry = nasdaqTable.etf.equals("Y") ? ETF : "*NA*";
			}
			if (country.equals(NA)) {
				country = "*NA*";
			}
			
			return new CompanyTable(nasdaqSymbol, Math.round(Double.valueOf(marketCap)), country, sector, industry);
		}
		public static String toCSV(String line) {
			final CompanyTable table = toCompanyTable(line);
			return (table == null) ? null : table.toCSV();
		}
	}
	
	private static final String NEWLINE = "\n";

	public static void save(String dirPath, String csvPath) {
		File root = new File(dirPath);
		if (!root.isDirectory()) {
			logger.error("Not directory  path = {}", dirPath);
			throw new SecuritiesException("not directory");
		}
		
		File[] fileList = root.listFiles();
		Arrays.sort(fileList, (a, b) -> a.getName().compareTo(b.getName()));
		
		Map<String, CompanyTable> companyMap = new TreeMap<>();
		
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(csvPath), BUFFER_SIZE)) {
			int totalRecord = 0;
			int totalSymbol = 0;
			int totalNull   = 0;
			for(File file: fileList) {
				if (file.length() == 0) continue;
				
				String fileName = file.getName();
				if (!fileName.startsWith("companies")) continue;
				
				int size = 0;
				try (BufferedReader br = new BufferedReader(new FileReader(file), BUFFER_SIZE)) {
					String header = br.readLine();
					if (header == null) {
						logger.error("{} header == null", file.getAbsolutePath());
						throw new SecuritiesException("not directory");
					}
					CSVRecord.checkHeader(header);
					for(;;) {
						String line = br.readLine();
						if (line == null) break;
						CompanyTable company = CSVRecord.toCompanyTable(line);
						if (company == null) {
							totalNull++;
							continue;
						}
						companyMap.put(company.symbol, company);
						size++;
					}
				}
				totalRecord += size;
				if (0 < size) totalSymbol++;
			}
			// Output csv
			Map<String, NasdaqTable> nasdaqMap = NasdaqUtil.getMap();
			for(String symbol: nasdaqMap.keySet()) {
				final CompanyTable company;
				if (companyMap.containsKey(symbol)) {
					company = companyMap.get(symbol);
				} else {
					NasdaqTable nasdaq = nasdaqMap.get(symbol);
					final String country   = "*NA*";
					final long   marketCap = -1;
					final String sector;
					final String industry;
					if (nasdaq.etf.equals("Y")) {
						sector   = ETF;
						industry = ETF;
					} else {
						sector   = "*NA*";
						industry = "*NA*";
					}
					company = new CompanyTable(symbol, marketCap, country, sector, industry);
				}
				bw.append(company.toCSV()).append(NEWLINE);
			}
			
			logger.info("RECORD {}", totalRecord);
			logger.info("SYMBOL {}", totalSymbol);
			logger.info("NULL   {}", totalNull);
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
