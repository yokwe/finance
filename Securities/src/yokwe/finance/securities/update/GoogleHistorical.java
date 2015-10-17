package yokwe.finance.securities.update;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class GoogleHistorical {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(GoogleHistorical.class);
	
//	Date,Open,High,Low,Close,Volume
//	15-Oct-15,37.79,37.79,37.79,37.79,103
//	14-Oct-15,-,-,-,37.83,0
//	13-Oct-15,-,-,-,37.83,0
//	12-Oct-15,38.06,38.06,37.59,37.83,5581

	
	public static final class CSVRecord {
		public static final String HEADER = "\uFEFFDate,Open,High,Low,Close,Volume";
		public static final int NUMBER_OF_FIELDS = 6;
		
		public static void checkHeader(String line) {
			if (!line.equals(HEADER)) {
				logger.error("header  line = {}", line);
				throw new SecuritiesException("header");
			}
		}
		
		private static final DateTimeFormatter parseDate  = DateTimeFormatter.ofPattern("d-MMM-yy");
		private static final DateTimeFormatter formatDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		public static String toCSV(String symbol, String line) {
			String[] fields = line.split(",");
			if (fields.length != NUMBER_OF_FIELDS) {
				logger.error("fields  {}  line = {}", symbol, line);
				throw new SecuritiesException("fields");
			}
			
			// Fix format of date  02-Jan-14 => 2014-01-02
			fields[0] = formatDate.format(parseDate.parse(fields[0]));
			
			// Special when field (open, high and low) contains dash
			if (fields[1].equals("-")) {
				fields[1] = fields[4];
			}
			if (fields[2].equals("-")) {
				fields[2] = fields[4];
			}
			if (fields[3].equals("-")) {
				fields[3] = fields[4];
			}
			
			String date     = fields[0];
			double open     = Double.valueOf(fields[1]);
			double high     = Double.valueOf(fields[2]);
			double low      = Double.valueOf(fields[3]);
			double close    = Double.valueOf(fields[4]);
			long   volume   = Long.valueOf(fields[5]);
			
			return String.format("%s,%s,%.2f,%.2f,%.2f,%.2f,%d", date, symbol, open, high, low, close, volume);
		}
	}
	
	private static final String CRLF = "\r\n";

	public static void save(String dirPath, String csvPath) {
		File root = new File(dirPath);
		if (!root.isDirectory()) {
			logger.error("Not directory  path = {}", dirPath);
			throw new SecuritiesException("not directory");
		}
		
		File[] fileList = root.listFiles();
		Arrays.sort(fileList, (a, b) -> a.getName().compareTo(b.getName()));
		
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(csvPath), 65536)) {
			
			int totalSize = 0;
			for(File file: fileList) {
				if (file.length() == 0) continue;
				
				String fileName = file.getName();
				String symbol = fileName.substring(0, fileName.length() - 4);
				
				int size = 0;
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
						bw.append(CSVRecord.toCSV(symbol, line)).append(CRLF);
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
