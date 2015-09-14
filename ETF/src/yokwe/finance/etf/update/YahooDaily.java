package yokwe.finance.etf.update;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.LoggerFactory;

import yokwe.finance.etf.ETFException;
import yokwe.finance.etf.util.CSVUtil;

public class YahooDaily {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(YahooDaily.class);
	
	private static final String DIR_PATH = "tmp/fetch/yahoo-daily";
	private static final String CSV_PATH = "tmp/sqlite/yahoo-daily.csv";
	
	enum Field_In {
		DATE("Date"), OPEN("Open"), HIGH("High"), LOW("Low"), CLOSE("Close"), VOLUME("Volume");
		
		private final String name;
		private Field_In(String name) {
			this.name = name;
		}
		public String toString() {
			return name;
		}
	}

	enum Field_Out {
		SYMBOL("Symbol"), DATE("Date"), OPEN("Open"), HIGH("High"), LOW("Low"), CLOSE("Close"), VOLUME("Volume");
		
		private final String name;
		private Field_Out(String name) {
			this.name = name;
		}
		public String toString() {
			return name;
		}
	}

	public static void save(String path) {
		File root = new File(path);
		if (!root.isDirectory()) {
			logger.error("Not directory  path = {}", path);
			throw new ETFException("not directory");
		}
		
		File[] fileList = root.listFiles();
		Arrays.sort(fileList, (a, b) -> a.getName().compareTo(b.getName()));
		
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(CSV_PATH), 65536);
			CSVPrinter printer = new CSVPrinter(bw, CSVFormat.DEFAULT)) {
			
			int totalSize = 0;
			for(File file: fileList) {
				if (file.length() == 0) continue;
				
				String fileName = file.getName();
				String symbol = fileName.substring(0, fileName.length() - 4);
				
				List<Map<Field_In, String>> inRecords = CSVUtil.load(file, Field_In.class);
				
				logger.info("SYMBOL {}", String.format("%-8s %6d", symbol, inRecords.size()));
				totalSize += inRecords.size();
				
				for(Map<Field_In, String> inRecord: inRecords) {
					Map<Field_Out, String> outRecord = new TreeMap<>();
					outRecord.put(Field_Out.SYMBOL, symbol);
					
					outRecord.put(Field_Out.DATE,   inRecord.get(Field_In.DATE));
					outRecord.put(Field_Out.OPEN,   inRecord.get(Field_In.OPEN));
					outRecord.put(Field_Out.HIGH,   inRecord.get(Field_In.HIGH));
					outRecord.put(Field_Out.LOW,    inRecord.get(Field_In.LOW));
					outRecord.put(Field_Out.CLOSE,  inRecord.get(Field_In.CLOSE));
					outRecord.put(Field_Out.VOLUME, inRecord.get(Field_In.VOLUME));

					printer.printRecord(outRecord.values());
				}
			}
			
			logger.info("TOTAL {}", totalSize);
		} catch (IOException e) {
			logger.error("IOException {}", e);
			throw new ETFException("IOException");
		}
	}
	private static void save() {
		save(DIR_PATH);
	}
	public static void main(String[] args) {
		save();
	}
}
