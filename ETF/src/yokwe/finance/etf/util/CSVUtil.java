package yokwe.finance.etf.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.LoggerFactory;

import yokwe.finance.etf.ETFException;

public class CSVUtil {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(CSVUtil.class);
	
	public static <E extends Enum<E>> void save(Writer writer, List<Map<E, String>> values) {
		if (values.size() == 0) return;
		
		try (BufferedWriter bw = new BufferedWriter(writer, 65536);
			CSVPrinter printer = new CSVPrinter(bw, CSVFormat.DEFAULT)) {
			// Use key of first record as header
			//printer.printRecord(values.get(0).keySet());			
			for(Map<E, String> record: values) {
				printer.printRecord(record.values());
			}
		} catch (IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new ETFException();
		}
	}
	public static <E extends Enum<E>> void save(File file, List<Map<E, String>> values) {
		try (FileWriter writer = new FileWriter(file)) {
			save(writer, values);
		} catch (IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new ETFException();
		}
	}
	public static <E extends Enum<E>> void save(OutputStream os, List<Map<E, String>> values) {
		try (OutputStreamWriter writer = new OutputStreamWriter(os)) {
			save(writer, values);
		} catch (IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new ETFException();
		}
	}
	
	public static <E extends Enum<E>> List<Map<E, String>>load(Reader reader, Class<E> eClass) {
		List<E> keyList = new ArrayList<>();
		for(E e: eClass.getEnumConstants()) {
			keyList.add(e);
		}

		List<Map<E, String>> ret = new ArrayList<>();
		
		try (BufferedReader br = new BufferedReader(reader, 65536);
			CSVParser parser = CSVFormat.DEFAULT.withHeader().parse(br)) {
			Iterator<CSVRecord> i = parser.iterator();
			while(i.hasNext()) {
				CSVRecord csvRecord = i.next();

				Map<E, String> record = new TreeMap<>();
				
				for(E key: keyList) {
					if (!csvRecord.isSet(key.toString())) {
						logger.error("IS_SET {}", key.toString());
						logger.error("csvRecord {}", csvRecord.toString());
						throw new ETFException("IS_SET");
					}
					String value = csvRecord.get(key);
					
					// format floating point value for ichart  123.4567 => 12346
					//if (value.matches("[0-9]+\\.[0-9][0-9](000|999)[0-9]+")) {
					if (value.matches("[0-9][0-9]+\\.[0-9][0-9][0-9]*")) {
						double v = Float.parseFloat(value);
						value = String.format("%.2f", v);
					}
					if (value.compareTo("000") == 0) {
						value = "0";
					}
					
					record.put(key, value);
				}
//				logger.debug("record {}", record);
				
				ret.add(record);
			}
			
		} catch (IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new ETFException();
		}		
		
		return ret;
	}
	public static <E extends Enum<E>> List<Map<E, String>>load(File file, Class<E> eClass) {
		try (FileReader reader = new FileReader(file)) {
			return load(reader, eClass);
		} catch (IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new ETFException();
		}
	}
	public static <E extends Enum<E>> List<Map<E, String>>load(InputStream is, Class<E> eClass) {
		try (InputStreamReader reader = new InputStreamReader(is)) {
			return load(reader, eClass);
		} catch (IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new ETFException();
		}
	}
	
	
//	enum Field {
//		DATE("Date"), OPEN("Open"), HIGH("High"), LOW("Low"), CLOSE("Close"), VOLUME("Volume");
//		
//		private final String name;
//		private Field(String name) {
//			this.name = name;
//		}
//		public String toString() {
//			return name;
//		}
//	}
//	public static void main(String[] args) throws FileNotFoundException {
//		FileReader reader = new FileReader("tmp/fetch/etf/ichart/QQQ.csv");
//		//List<Map<Field, String>> records = load(reader, Field.class);
//		load(reader, Field.class);
//	}
}
