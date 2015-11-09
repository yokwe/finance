package yokwe.finance.securities.update;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.database.DividendTable;

public final class YahooDividend {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(YahooDividend.class);

	public static final class CSVRecord {
		public static final String HEADER = "Date,Dividends";
		public static final int NUMBER_OF_FIELDS = 2;
		
		public static void checkHeader(String line) {
			if (!line.equals(HEADER)) {
				logger.error("header  line = {}", line);
				throw new SecuritiesException("header");
			}
		}
		public static DividendTable toDividendTable(String symbol, String line) {
			String[] fields = line.split(",");
			if (fields.length != NUMBER_OF_FIELDS) {
				logger.error("fields  {}  line = {}", symbol, line);
				throw new SecuritiesException("fields");
			}
			String date     = fields[0];
			double dividend = Double.valueOf(fields[1]);
			
			return new DividendTable(date, symbol, dividend);
		}
		public static String toCSV(String symbol, String line) {
			return toDividendTable(symbol, line).toCSV();
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
		
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(csvPath), 65536)) {
			
			int totalRecord = 0;
			int totalSymbol = 0;
			for(File file: fileList) {
				if (file.length() == 0) continue;
				
				String fileName = file.getName();
				String symbol = fileName.substring(0, fileName.lastIndexOf('.'));
				
				int count = 0;
				try (BufferedReader br = new BufferedReader(new FileReader(file))) {
					String header = br.readLine();
					if (header == null) {
						logger.error("{} header == null", file.getAbsolutePath());
						throw new SecuritiesException("not directory");
					}
					CSVRecord.checkHeader(header);
					for(;;) {
						String line = br.readLine();
						if (line == null) break;
						bw.append(CSVRecord.toCSV(symbol, line)).append(NEWLINE);
						count++;
					}
				}
				totalRecord += count;
				if (0 < count) totalSymbol++;
				logger.info(String.format("%-6s %6d", symbol, count));
			}
			
			logger.info("RECORD {}", totalRecord);
			logger.info("SYMBOL {}", totalSymbol);
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
