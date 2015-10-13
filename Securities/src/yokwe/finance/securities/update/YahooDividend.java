package yokwe.finance.securities.update;

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

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.CSVUtil;

public class YahooDividend {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(YahooDividend.class);
	
	enum Field_In {
		DATE("Date"), DIVIDENDS("Dividends");
		
		private final String name;
		private Field_In(String name) {
			this.name = name;
		}
		public String toString() {
			return name;
		}
	}

	enum Field_Out {
		SYMBOL("Symbol"), DATE("Date"), DIVIDENDS("Dividends");
		
		private final String name;
		private Field_Out(String name) {
			this.name = name;
		}
		public String toString() {
			return name;
		}
	}

	public static void save(String dirPath, String csvPath) {
		File root = new File(dirPath);
		if (!root.isDirectory()) {
			logger.error("Not directory  path = {}", dirPath);
			throw new SecuritiesException("not directory");
		}
		
		File[] fileList = root.listFiles();
		Arrays.sort(fileList, (a, b) -> a.getName().compareTo(b.getName()));
		
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(csvPath), 65536);
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
					
					// 1.022000 => 1.022
					String dividend = String.format("%.3f", Float.valueOf(inRecord.get(Field_In.DIVIDENDS)));
					
					outRecord.put(Field_Out.DATE,      inRecord.get(Field_In.DATE));
					outRecord.put(Field_Out.DIVIDENDS, dividend);

					printer.printRecord(outRecord.values());
				}
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
