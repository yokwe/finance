package yokwe.finance.securities.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
	
	// CSV file should not have header, because sqlite .import read header as data
	
	// Save List<E> data as CSV file.
	// Load CSV file as List<E> data.
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public static @interface ColumnName {
		String value();
	}

	
	private static int BUFFER_SIZE = 64 * 1024;

	public static <E> List<E> loadWithHeader(String path, Class<E> clazz, String header) {
		CSVFormat csvFormat = CSVFormat.DEFAULT.withHeader(header.split(",")).withRecordSeparator("\n");
		return load(path, clazz, csvFormat);
	}

	public static <E> List<E> loadWithHeader(String path, Class<E> clazz) {
		String[] names;
		{
			List<String> nameList = new ArrayList<>();
			for(Field field: clazz.getDeclaredFields()) {
				// Skip static field
				if (Modifier.isStatic(field.getModifiers())) continue;
				
				ColumnName columnName = field.getDeclaredAnnotation(ColumnName.class);
				nameList.add((columnName == null) ? field.getName() : columnName.value());
			}
			names = nameList.toArray(new String[0]);
		}
		
		CSVFormat csvFormat = CSVFormat.DEFAULT.withHeader(names).withRecordSeparator("\n");
		return load(path, clazz, csvFormat);
	}

	public static <E> List<E> load(String path, Class<E> clazz) {
		CSVFormat csvFormat = CSVFormat.DEFAULT.withRecordSeparator("\n");
		return load(path, clazz, csvFormat);
	}
	
	public static <E> List<E> load(String path, Class<E> clazz, CSVFormat csvFormat) {
		try {
			return load(new BufferedReader(new FileReader(path), BUFFER_SIZE), clazz, csvFormat);
		} catch (FileNotFoundException e) {
			logger.error("FileNotFoundException {}", e.toString());
			throw new SecuritiesException("FileNotFoundException");
		}
	}

	public static <E> List<E> load(Reader reader, Class<E> clazz, CSVFormat csvFormat) {
		Field[]  fields;
		String[] types;
		int      size;
		{
			List<Field>  fieldList = new ArrayList<>();
			List<String> typeList  = new ArrayList<>();
			for(Field field: clazz.getDeclaredFields()) {
				// Skip static field
				if (Modifier.isStatic(field.getModifiers())) continue;

				fieldList.add(field);
				typeList.add(field.getType().getName());
			}
			
			fields = fieldList.toArray(new Field[0]);
			types  = typeList.toArray(new String[0]);
			size   = fieldList.size();
		}
				
		String[] names = csvFormat.getHeader();
		
		try (CSVParser csvParser = csvFormat.parse(reader)) {
			List<E> dataList = new ArrayList<>();
			for(CSVRecord record: csvParser) {
				// Sanity check
				if (record.size() != size) {
					logger.error("record.size != size  {} != {}", record.size(), size);
					logger.error("record = {}", record);
					throw new SecuritiesException("record.size != size");
				}
				
				if (names != null && record.getRecordNumber() == 1) {
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
				
				E data = clazz.newInstance();
				for(int i = 0; i < size; i++) {
					String value = record.get(i);
					switch(types[i]) {
					case "int":
						fields[i].setInt(data, Integer.valueOf(value));
						break;
					case "long":
						fields[i].setLong(data, Long.valueOf(value));
						break;
					case "double":
						fields[i].setDouble(data, Double.valueOf(value));
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
		List<Field> fieldList = new ArrayList<>();
		for(Field field: o.getClass().getDeclaredFields()) {
			// Skip static field
			if (Modifier.isStatic(field.getModifiers())) continue;
			fieldList.add(field);
		}
		Field[] fields = fieldList.toArray(new Field[0]);
		String[] names = new String[fields.length];
		for(int i = 0; i < names.length; i++) {
			ColumnName columnName = fields[i].getDeclaredAnnotation(ColumnName.class);
			names[i] = (columnName == null) ? fields[i].getName() : columnName.value();
		}
		Object[] values = new Object[fields.length];
		
		// Create parent dirs and file if not exists.
		{
			File file = new File(path);
			
			File fileParent = file.getParentFile();
			if (!fileParent.exists()) {
				fileParent.mkdirs();
			}
			if (!file.exists()) {
				try {
					file.createNewFile();
				} catch (IOException e) {
					logger.error("IOException {}", e.toString());
					throw new SecuritiesException("IOException");
				}
			}
		}

		CSVFormat csvFormat = CSVFormat.DEFAULT.withHeader(names).withRecordSeparator("\n");
		try (CSVPrinter csvPrint = new CSVPrinter(new BufferedWriter(new FileWriter(path), BUFFER_SIZE), csvFormat)) {
			for(E entry: dataList) {
				for(int i = 0; i < fields.length; i++) {
					Object value = fields[i].get(entry);
					if (value == null) {
						logger.error("value is null.  {} {} {}", o.getClass().getName(), fields[i].getName(), fields[i].getType().getName());
						logger.error("entry {}", entry.toString());
						throw new SecuritiesException("value is null");
					}
					values[i] = value.toString();
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

	public static <E> void save(List<E> dataList, String path) {
		Object o = dataList.get(0);
		List<Field> fieldList = new ArrayList<>();
		for(Field field: o.getClass().getDeclaredFields()) {
			// Skip static field
			if (Modifier.isStatic(field.getModifiers())) continue;
			fieldList.add(field);
		}
		Field[] fields = fieldList.toArray(new Field[0]);
//		String[] names = new String[fields.length];
//		for(int i = 0; i < names.length; i++) {
//			ColumnName columnName = fields[i].getDeclaredAnnotation(ColumnName.class);
//			names[i] = (columnName == null) ? fields[i].getName() : columnName.value();
//		}
		Object[] values = new Object[fields.length];

		// Create parent dirs and file if not exists.
		{
			File file = new File(path);
			
			File fileParent = file.getParentFile();
			if (!fileParent.exists()) {
				fileParent.mkdirs();
			}
			if (!file.exists()) {
				try {
					file.createNewFile();
				} catch (IOException e) {
					logger.error("IOException {}", e.toString());
					throw new SecuritiesException("IOException");
				}
			}
		}

		CSVFormat csvFormat = CSVFormat.DEFAULT.withRecordSeparator("\n");
		try (CSVPrinter csvPrint = new CSVPrinter(new BufferedWriter(new FileWriter(path), BUFFER_SIZE), csvFormat)) {
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
