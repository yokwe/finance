package yokwe.finance.securities.update;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public final class GoogleGetPrices {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(GoogleGetPrices.class);
	
	private static final int BUFFER_SIZE = 256 * 1024;

	static final class MyDate {
		private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
		
		long       epochSecond    = 0;
		long       intervalSecond = 0;
		ZoneOffset zoneOffset     = ZoneOffset.ofHoursMinutes(0, 0);
		
		void setInterval(String number) {
			intervalSecond = Integer.valueOf(number);
		}
		
		void setTimeZone(String number) {
			final int offsetMinutes = Integer.valueOf(number);
			zoneOffset = ZoneOffset.ofHoursMinutes(offsetMinutes / 60, offsetMinutes % 60);
		}
		
		void setTime(String number) {
			epochSecond = Integer.valueOf(number);
		}
		
		long getTime(int count) {
			return epochSecond + intervalSecond * count;
		}
		
		String toDate(String number) {
			if (number.charAt(0) == 'a') {
				setTime(number.substring(1));
				LocalDateTime localDateTime = LocalDateTime.ofEpochSecond(getTime(0), 0, zoneOffset);
				return formatter.format(localDateTime);
			} else {
				final int count = Integer.valueOf(number);
				LocalDateTime localDateTime = LocalDateTime.ofEpochSecond(getTime(count), 0, zoneOffset);
				return formatter.format(localDateTime);
			}
		}
	}
	
	private static MyDate myDate = new MyDate();
	
	public static final class CSVRecord {
		public static final int NUMBER_OF_FIELDS = 6;
		
		public static String toCSV(String symbol, String line) {
			String[] fields = line.split(",");
			if (fields.length != NUMBER_OF_FIELDS) {
				logger.error("fields  {}  line = {}", symbol, line);
				throw new SecuritiesException("fields");
			}
			String dateStr  = fields[0];
			double close    = Double.valueOf(fields[1]);
			double high     = Double.valueOf(fields[2]);
			double low      = Double.valueOf(fields[3]);
			double open     = Double.valueOf(fields[4]);
			long   volume   = Long.valueOf(fields[5]);
			
			// Special when volume equals zero
			if (volume == 0) {
				high = low = open = close;
			}
			
			String date = myDate.toDate(dateStr);

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
					
//					EXCHANGE%3DNYSEARCA
//					MARKET_OPEN_MINUTE=570
//					MARKET_CLOSE_MINUTE=960
//					INTERVAL=86400
//					COLUMNS=DATE,CLOSE,HIGH,LOW,OPEN,VOLUME
//					DATA=
//					TIMEZONE_OFFSET=-240
//					a1279742400,24.7,25.1,24.7,25.1,41999
//					1,25.26,25.42,25.13,25.42,17480
//					2,25.28,25.54,25.08,25.54,8625
//					5,25.37,25.4,25.22,25.4,18850
					
					for(;;) {
						String line = br.readLine();
						if (line == null) break;
						
						if (line.startsWith("EXCHANGE")) continue;
						if (line.startsWith("MARKET_")) continue;
						
						if (line.startsWith("INTERVAL=")) {
							String[] values = line.split("=");
							myDate.setInterval(values[1]);
							continue;
						}
						if (line.startsWith("COLUMNS=")) continue;
						if (line.startsWith("DATA=")) break;
					}
					
					for(;;) {
						String line = br.readLine();
						if (line == null) break;
						if (line.startsWith("TIMEZONE_OFFSET=")) {
							String[] values = line.split("=");
							myDate.setTime(values[1]);
							continue;
						}
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
