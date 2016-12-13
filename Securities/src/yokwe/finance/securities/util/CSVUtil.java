package yokwe.finance.securities.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class CSVUtil {
	private static final Logger logger = LoggerFactory.getLogger(CSVUtil.class);

	public static <E> List<E> loadWithoutHeader(String path, Class<E> clazz) {
		return load(path, clazz, false);
	}
	public static <E> List<E> loadWithHeader(String path, Class<E> clazz) {
		return load(path, clazz, true);
	}

	public static <E> List<E> load(String path, Class<E> clazz, boolean withHeader) {
		List<E> dataList = new ArrayList<>();
		
		Field[] fields = clazz.getDeclaredFields();
		final int size = fields.length;
		String[] names = new String[size];
		String[] types = new String[size];
		for(int i = 0; i < size; i++) {
			names[i] = fields[i].getName();
			types[i] = fields[i].getType().getName();
		}
		CSVFormat csvFormat = CSVFormat.DEFAULT.withHeader(names).withRecordSeparator("\n");
		File file = new File(path);
		
		try (CSVParser csvParser = csvFormat.parse(new FileReader(file))) {
			for(CSVRecord record: csvParser) {
				if (record.size() != size) {
					logger.error("record.size != size  {} != {}", record.size(), size);
					throw new SecuritiesException("record.size != size");
				}
				
				if (record.getRecordNumber() == 1) {
					if (withHeader) {
						// Sanity check
						int headerSize = record.size();
						if (headerSize != size) {
							logger.error("headerSize != size  {} != {}", headerSize, size);
							throw new SecuritiesException("headerSize != size");
						}
						for(int i = 0; i < size; i++) {
							String headerName = record.get(i);
							if (!headerName.equals(names[i])) {
								logger.error("headerName != name  {}  {} != {}", i, headerName, names[i]);
								throw new SecuritiesException("headerName != name");
							}
						}
						continue;
					}
				}
				
				E data = clazz.newInstance();
				for(int i = 0; i < size; i++) {
					String name = names[i];
					String value = record.get(name);
					switch(types[i]) {
					case "int":
						fields[i].setInt(data, Integer.valueOf(value));
						break;
					case "long":
						fields[i].setLong(data, Long.valueOf(value));
						break;
					case "boolean":
						fields[i].setBoolean(data, Boolean.valueOf(value));
						break;
					case "java.lang.String":
						fields[i].set(data, value);
						break;
					default:
						logger.error("Unexptected type {}", types[i]);
						throw new SecuritiesException("Unexptected type");
					}
				}
				dataList.add(data);
			}
			return dataList;
		} catch (FileNotFoundException e) {
			logger.error("FileNotFoundException {}", e.toString());
			throw new SecuritiesException("FileNotFoundException");
		} catch (IOException e) {
			logger.error("IOException {}", e.toString());
			throw new SecuritiesException("IOException");
		} catch (InstantiationException e) {
			logger.error("InstantiationException {}", e.toString());
			throw new SecuritiesException("InstantiationException");
		} catch (IllegalAccessException e) {
			logger.error("IllegalAccessException {}", e.toString());
			throw new SecuritiesException("IllegalAccessException");
		}
	}
	
	public static <E> void saveWithHeader(List<E> dataList, String path) {
		Object o = dataList.get(0);
		Field[] fields = o.getClass().getDeclaredFields();
		String[] names = new String[fields.length];
		for(int i = 0; i < names.length; i++) {
			names[i] = fields[i].getName();
		}
		Object[] values = new Object[fields.length];
		CSVFormat csvFormat = CSVFormat.DEFAULT.withHeader(names).withRecordSeparator("\n");

		try (CSVPrinter csvPrint = new CSVPrinter(new BufferedWriter(new FileWriter(path)), csvFormat)) {
			for(E entry: dataList) {
				for(int i = 0; i < fields.length; i++) {
					values[i] = fields[i].get(entry).toString();
				}
				csvPrint.printRecord(values);
			}
		} catch (FileNotFoundException e) {
			logger.error("FileNotFoundException {}", e.toString());
			throw new SecuritiesException("FileNotFoundException");
		} catch (IOException e) {
			logger.error("IOException {}", e.toString());
			throw new SecuritiesException("IOException");
		} catch (IllegalArgumentException e) {
			logger.error("IllegalArgumentException {}", e.toString());
			throw new SecuritiesException("IllegalArgumentException");
		} catch (IllegalAccessException e) {
			logger.error("IllegalAccessException {}", e.toString());
			throw new SecuritiesException("IllegalAccessException");
		}
	}

}
