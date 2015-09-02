package yokwe.finance.etf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.LoggerFactory;

public final class Util {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Util.class);

	public static String getContents(File file) {
		char[] buffer = new char[65536];
		
		StringBuilder ret = new StringBuilder();
		
		try (BufferedReader bfr = new BufferedReader(new FileReader(file), buffer.length)) {
			for(;;) {
				int len = bfr.read(buffer);
				if (len == -1) break;
				
				ret.append(buffer, 0, len);
			}
		} catch (FileNotFoundException e) {
			logger.error("FileNotFoundException {}", file.getName());
			throw new RuntimeException("FileNotFoundException");
		} catch (IOException e1) {
			logger.error("IOException {}", file.getName());
			throw new RuntimeException("IOException");
		}
		return ret.toString();
	}
	
	public static <E extends Enum<E>> void save(Appendable appendable, List<Map<E, String>> values) {
		if (values.size() == 0) return;
		
		try (CSVPrinter printer = new CSVPrinter(appendable, CSVFormat.DEFAULT)) {
			// Use key of first record as header
			printer.printRecord(values.get(0).keySet());			
			for(Map<E, String> record: values) {
				printer.printRecord(record.values());
			}
		} catch (IOException e) {
			logger.error("IOException {}", e);
			throw new RuntimeException("IOException");
		}
	}
	
	public static <E extends Enum<E>> List<Map<E, String>>load(Reader reader, Class<E> eClass) {
		List<E> keyList = new ArrayList<>();
		for(E e: eClass.getEnumConstants()) {
			keyList.add(e);
		}
		
		logger.debug("keyList = {}", keyList);

		List<Map<E, String>> ret = new ArrayList<>();
		
		try {
			Iterator<CSVRecord> i = CSVFormat.DEFAULT.withHeader().parse(reader).iterator();
			while(i.hasNext()) {
				CSVRecord csvRecord = i.next();

				Map<E, String> record = new TreeMap<>();
				
				for(E key: keyList) {
					if (!csvRecord.isSet(key.name())) {
						logger.error("IS_SET {}", key.name());
						logger.error("csvRecord {}", csvRecord.toString());
						throw new RuntimeException("IS_SET");
					}
					String value = csvRecord.get(key);
					
					// format floating point value
					if (value.matches("[0-9]+\\.[0-9]{4,}")) {
						double v = Float.parseFloat(value);
						value = String.format("%.2f", v);
					}
					
					record.put(key, value);
				}
				logger.debug("record {}", record);
				
				ret.add(record);
			}
			
		} catch (IOException e) {
			logger.error("IOException {}", e);
			throw new RuntimeException("IOException");
		}		
		
		return ret;
	}
	
	enum Field {
		Date, Open, High, Low, Close, Volume
	}
	public static void main(String[] args) throws FileNotFoundException {
		FileReader reader = new FileReader("tmp/fetch/ichart/AADR.csv");
		//List<Map<Field, String>> records = load(reader, Field.class);
		load(reader, Field.class);
	}
}
