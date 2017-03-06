package yokwe.finance.securities.libreoffice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.sheet.XCellRangeData;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellContentType;
import com.sun.star.table.XCell;
import com.sun.star.table.XCellRange;
import com.sun.star.text.XText;
import com.sun.star.uno.UnoRuntime;

import yokwe.finance.securities.SecuritiesException;

public class Sheet {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Sheet.class);
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public static @interface SheetName {
		String value();
	}

	// First row is zero
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public static @interface HeaderRow {
		int value();
	}

	// First row is zero
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public static @interface DataRow {
		int value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface ColumnName {
		String value();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface NumberFormat {
		String value();
	}
	
	public static <E extends Sheet> List<E> getInstance(SpreadSheet spreadSheet, Class<E> clazz) {
		String sheetName = getSheetName(clazz);
		return getInstance(spreadSheet, clazz, sheetName);
	}
	public static <E extends Sheet> List<E> getInstance(SpreadSheet spreadSheet, Class<E> clazz, String sheetName) {
		HeaderRow headerRow = clazz.getDeclaredAnnotation(HeaderRow.class);
		DataRow   dataRow   = clazz.getDeclaredAnnotation(DataRow.class);
		if (sheetName == null) {
			logger.error("sheetName == null");
			throw new SecuritiesException("sheetName == null");
		}
		if (headerRow == null) {
			logger.error("No HeaderRow annotation = {}", clazz.getName());
			throw new SecuritiesException("No HeaderRow annotation");
		}
		if (dataRow == null) {
			logger.error("No DataRow annotation = {}", clazz.getName());
			throw new SecuritiesException("No DataRow annotation");
		}
//		logger.info("Sheet {}  headerRow {}  dataRow {}", sheetName.value(), headerRow.value(), dataRow.value());
		XSpreadsheet spreadsheet = spreadSheet.getSheet(sheetName);
		
		Map<String, Field> fieldMap = new TreeMap<>();
		for(Field field: clazz.getDeclaredFields()) {
			ColumnName columnName = field.getDeclaredAnnotation(ColumnName.class);
			if (columnName == null) continue;
			fieldMap.put(columnName.value(), field);
		}
		if (fieldMap.size() == 0) {
			logger.error("No ColumnName annotation = {}", clazz.getName());
			throw new SecuritiesException("No ColumnName annotation");
		}
		
		//
		// Take information from SpreadSheet
		//
		// Build columnMap - column name to column index
		Map<String, Integer> columnMap = new HashMap<>();
		
		{
			try {
				// Build header map
				int row = headerRow.value();
				
				for(int i = 0; i < 100; i++) {
					final XCell cell = spreadsheet.getCellByPosition(i, row);
					final CellContentType type = cell.getType();
					if (type.equals(CellContentType.EMPTY)) break;
					
					XText text = UnoRuntime.queryInterface(XText.class, cell);
					String value = text.getString();
					columnMap.put(value, i);
//					logger.info("{} - {} {}", i, LibreOffice.toString(type), value);
				}
			} catch (IndexOutOfBoundsException e) {
				logger.error("Exception {}", e.toString());
				throw new SecuritiesException("Unexpected");
			}
			
			// Sanity check
			for(String name: fieldMap.keySet()) {
				if (columnMap.containsKey(name)) continue;
				logger.error("columnMap contains no field name = {}", name);
				throw new SecuritiesException("Unexpected");
			}
		}
		
		Field[]    fieldArray;
		int[]      indexArray;
		final int  colSize = fieldMap.size();

		{
			fieldArray = new Field[colSize];
			indexArray = new int[colSize];
			int i = 0;
			for(String columnName: fieldMap.keySet()) {
				fieldArray[i] = fieldMap.get(columnName);
				indexArray[i] = columnMap.get(columnName);
				i++;
			}
		}
		final int stringHash  = String.class.hashCode();
		final int integerHash = Integer.TYPE.hashCode();
		final int doubleHash  = Double.TYPE.hashCode();
		
		final LocalDate dateEpoch = LocalDate.of(1899, 12, 30);

		try {
			List<E> ret = new ArrayList<>();

			int rowFirst = dataRow.value();
			int rowLast = SpreadSheet.getLastDataRow(spreadsheet, 0, rowFirst, 65536);
			int rowSize = rowLast - rowFirst + 1;
			// cellRange is [rowFirst..rowLast)
			logger.info("rowSize [{} .. {}] = {}  colSize {}", rowFirst, rowLast, rowSize, colSize);
			
			@SuppressWarnings("unchecked")
			E[] instanceArray = (E[]) Array.newInstance(clazz, rowSize);
			for(int row = 0; row < rowSize; row++) {
				E instance = clazz.newInstance();
				instanceArray[row] = instance;
				ret.add(instance);
			}
			
			// Capture data in dataArray for each column
			Object[][] dataArray = new Object[rowSize][colSize];
			for(int col = 0; col < colSize; col++) {
				int column = indexArray[col];
				
				// left top right bottom
				//logger.info("cellRange {} {} {} {}", column, rowFirst, column, rowLast);
				XCellRange cellRange = spreadsheet.getCellRangeByPosition(column, rowFirst, column, rowLast);
				XCellRangeData cellRangeData = UnoRuntime.queryInterface(XCellRangeData.class, cellRange);
				Object data[][] = cellRangeData.getDataArray();
				
				if (data.length != rowSize) {
					logger.error("Unexpected rowSize = {}  data.length = {}", rowSize, data.length);
					throw new SecuritiesException("Unexpected");
				}
				for(int row = 0; row < rowSize; row++) dataArray[row][col] = data[row][0];
			}

			// Assign value to field of each instance
			for(int col = 0; col < colSize; col++) {
				Field field = fieldArray[col];
				Class<?> fieldType = field.getType();
				int fieldTypeHash = fieldType.hashCode();
				
				if (fieldTypeHash == stringHash) {
					// Convert double value to date string if necessary.
					XCell   cell         = spreadsheet.getCellByPosition(indexArray[col], rowFirst);
					String  formatString = spreadSheet.getFormatString(cell);
					boolean isYYYYMMDD   = formatString.equals("YYYY-MM-DD");
					if (isYYYYMMDD) {
						for(int row = 0; row < rowSize; row++) {
							E instance = instanceArray[row];
							Object value = dataArray[row][col];
//							logger.info("S {} {} - {}", row, col, value);
							if (value instanceof String) {
								field.set(instance, (String)value);
							} else if (value instanceof Double) {
								int daysSinceEpoch = ((Double) value).intValue();
								field.set(instance, DateTimeFormatter.ISO_LOCAL_DATE.format(dateEpoch.plusDays(daysSinceEpoch)));
							} else {
								logger.error("Unknow value type = {}", value.getClass().getName());
								throw new SecuritiesException("Unexpected");
							}
						}
					} else {
						for(int row = 0; row < rowSize; row++) {
							E instance = instanceArray[row];
							Object value = dataArray[row][col];
//							logger.info("S {} {} - {}", row, col, value);
							if (value instanceof String) {
								field.set(instance, (String)value);
							} else if (value instanceof Double) {
								field.set(instance, value.toString());
							} else {
								logger.error("Unknow value type = {}", value.getClass().getName());
								throw new SecuritiesException("Unexpected");
							}
						}
					}
				} else if (fieldTypeHash == doubleHash) {
					for(int row = 0; row < rowSize; row++) {
						E instance = instanceArray[row];
						Object value = dataArray[row][col];
//						logger.info("D {} {} - {}", row, col, value);
						if (value instanceof Double) {
							field.setDouble(instance, ((Double)value).doubleValue());
						} else if (value instanceof Integer) {
							field.setDouble(instance, ((Integer)value).intValue());
						} else if (value instanceof String) {
							String stringValue = (String)value;
							if (stringValue.length() == 0) {
								field.setDouble(instance, 0);
							} else if (stringValue.equals("NaN")) {
								field.setDouble(instance, 0);
							} else {
								logger.error("Unexpeced dobuleHash stringValue = {} - {} - {}", col, row, stringValue);
								throw new SecuritiesException("Unexpected");
							}
						} else {
							logger.error("Unknow value type = {}", value.getClass().getName());
							throw new SecuritiesException("Unexpected");
						}
					}						
				} else if (fieldTypeHash == integerHash) {
					for(int row = 0; row < rowSize; row++) {
						E instance = instanceArray[row];
						Object value = dataArray[row][col];
//						logger.info("I {} {} - {}", row, col, value);
						if (value instanceof Integer) {
							field.setInt(instance, ((Integer)value).intValue());
						} else if (value instanceof Double) {
							field.setInt(instance, ((Double)value).intValue());
						} else if (value instanceof String) {
							String stringValue = (String)value;
							if (stringValue.length() == 0) {
								field.setInt(instance, 0);
							} else if (stringValue.equals("NaN")) {
								field.setInt(instance, 0);
							} else {
								logger.error("Unexpeced integerHash stringValue = {} - {} - {}", col, row, stringValue);
								throw new SecuritiesException("Unexpected");
							}
						} else {
							logger.error("Unknow value type = {}", value.getClass().getName());
							throw new SecuritiesException("Unexpected");
						}
					}						
				} else {
					logger.error("Unknow field type = {}", fieldType.getName());
					throw new SecuritiesException("Unexpected");
				}
			}
			return ret;
		} catch (IndexOutOfBoundsException | IllegalAccessException | InstantiationException e) {
			logger.error("Exception {}", e.toString());
			throw new SecuritiesException("Unexpected");
		}
	}
	
	public static <E extends Sheet> String getSheetName(Class<E> clazz) {
		SheetName sheetName = clazz.getDeclaredAnnotation(SheetName.class);
		if (sheetName == null) {
			logger.error("No SheetName annotation = {}", clazz.getName());
			throw new SecuritiesException("No SheetName annotation");
		}
		
		return sheetName.value();
	}

	public static <E extends Sheet> void saveSheet(SpreadSheet spreadSheet, List<E> dataList, String sheetName) {
		XSpreadsheet xSpreadsheet = spreadSheet.getSheet(sheetName);
		HeaderRow    headerRow    = null;
		DataRow      dataRow      = null;
		Field[]      fields       = null;
		String       className    = null;
		
		{
			E o = dataList.iterator().next();
			headerRow    = o.getClass().getDeclaredAnnotation(HeaderRow.class);
			dataRow      = o.getClass().getDeclaredAnnotation(DataRow.class);
			fields       = o.getClass().getDeclaredFields();
			className    = o.getClass().getName();
		}

		if (sheetName == null) {
			logger.error("sheetName is null");
			throw new SecuritiesException("sheetName is null");
		}
		if (headerRow == null) {
			logger.error("No HeaderRow annotation = {}", className);
			throw new SecuritiesException("No HeaderRow annotation");
		}
		if (dataRow == null) {
			logger.error("No DataRow annotation = {}", className);
			throw new SecuritiesException("No DataRow annotation");
		}
		//logger.info("Sheet {}  headerRow {}  dataRow {}", sheetName.value(), headerRow.value(), dataRow.value());
		
		// Insertion order is important, So we use LinkedHashMap instead of HashMap
		//   for(Map.Entry<String, Field> entry: fieldMap.entrySet()) { }
		Map<String, Field> fieldMap = new LinkedHashMap<>();
		// key is columnName from ColumnName annotation
		for(Field field: fields) {
			ColumnName columnName = field.getDeclaredAnnotation(ColumnName.class);
			if (columnName == null) continue;
			fieldMap.put(columnName.value(), field);
		}
		if (fieldMap.size() == 0) {
			logger.error("No ColumnName annotation = {}", className);
			throw new SecuritiesException("No ColumnName annotation");
		}
		
		//
		// Take information from SpreadSheet
		//
		// Build columnMap - column name to column index
		try {
			Map<String, Integer> columnMap = new HashMap<>();
			// key is columnName from header and it's column index
			{
				int row = headerRow.value();
				// Build header map
				for(int column = 0; column < 100; column++) {
					final XCell cell = xSpreadsheet.getCellByPosition(column, row);
					final CellContentType type = cell.getType();
					if (type.equals(CellContentType.EMPTY)) break;
					
					XText text = UnoRuntime.queryInterface(XText.class, cell);
					String value = text.getString();
					columnMap.put(value, column);
//					logger.info("{} - {} {}", i, LibreOffice.toString(type), value);
				}
				
				// Sanity check
				for(String name: fieldMap.keySet()) {
					if (columnMap.containsKey(name)) continue;
					logger.error("columnMap contains no field name = {}", name);
					throw new SecuritiesException("Unexpected");
				}
			}
			
			{
				int row = dataRow.value();
				for(E data: dataList) {
					for(Map.Entry<String, Field> entry: fieldMap.entrySet()) {
						int   column = columnMap.get(entry.getKey());
						XCell cell   = xSpreadsheet.getCellByPosition(column, row);
						
						Field        field        = entry.getValue();
						Class<?>     fieldType    = field.getType();						
						NumberFormat numberFormat = field.getDeclaredAnnotation(NumberFormat.class);
						
						if (numberFormat != null) {
							spreadSheet.setNumberFormat(cell, numberFormat.value());
						}
						
						if (fieldType.equals(String.class)) {
							Object value = field.get(data);
							cell.setFormula(value.toString());
						} else if (fieldType.equals(Integer.TYPE)) {
							int value = field.getInt(data);
							cell.setValue(value);
						} else if (fieldType.equals(Double.TYPE)) {
							double value = field.getDouble(data);
							cell.setValue(value);
						} else {
							logger.error("Unknow field type = {}", fieldType.getName());
							throw new SecuritiesException("Unexpected");
						}
					}
					row++;
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException | IndexOutOfBoundsException e) {
			logger.error("Exception {}", e.toString());
			throw new SecuritiesException("Unexpected");
		}
	}
	public static <E extends Sheet> void saveSheet(SpreadSheet spreadSheet, List<E> dataList) {
		E o = dataList.iterator().next();
		String sheetName = getSheetName(o.getClass());
		saveSheet(spreadSheet, dataList, sheetName);
	}
	
	private static class ColumnInfo {
		public final String name;
		public final int    index;
		public final Field  field;
		
		public ColumnInfo(String name, int index, Field field) {
			this.name  = name;
			this.index = index;
			this.field = field;
		}
	}
	public static <E extends Sheet> void saveSheet(SpreadSheet spreadSheet, Map<String, E> dataMap, String keyColumnName, String sheetName) {
		XSpreadsheet xSpreadsheet = spreadSheet.getSheet(sheetName);
		HeaderRow    headerRow    = null;
		DataRow      dataRow      = null;
		Field[]      fields       = null;
		String       className    = null;
		
		{
			E o = dataMap.values().iterator().next();
			headerRow    = o.getClass().getDeclaredAnnotation(HeaderRow.class);
			dataRow      = o.getClass().getDeclaredAnnotation(DataRow.class);
			fields       = o.getClass().getDeclaredFields();
			className    = o.getClass().getName();
		}
		

		if (headerRow == null) {
			logger.error("No HeaderRow annotation = {}", className);
			throw new SecuritiesException("No HeaderRow annotation");
		}
		if (dataRow == null) {
			logger.error("No DataRow annotation = {}", className);
			throw new SecuritiesException("No DataRow annotation");
		}

		// key is column name
		Map<String, Field> fieldMap = new TreeMap<>();
		for(Field field: fields) {
			ColumnName columnName = field.getDeclaredAnnotation(ColumnName.class);
			if (columnName == null) continue;
			fieldMap.put(columnName.value(), field);
		}
		
		try {
			List<ColumnInfo> columnInfoList = new ArrayList<>();
			
			// Build columnInfoList
			{
				int row = headerRow.value();
				// Build header map
				for(int column = 0; column < 100; column++) {
					final XCell cell = xSpreadsheet.getCellByPosition(column, row);
					final CellContentType type = cell.getType();
					if (type.equals(CellContentType.EMPTY)) continue;
					
					XText text = UnoRuntime.queryInterface(XText.class, cell);
					String name = text.getString();
					
					ColumnInfo columnInfo = new ColumnInfo(name, column, fieldMap.get(name));
					if (columnInfo.field == null) {
						logger.error("No field {}", name);
						throw new SecuritiesException("No field");
					}
					
					columnInfoList.add(columnInfo);
				}				
			}
			
			int keyIndex = -1;
			{
				for(ColumnInfo columnInfo: columnInfoList) {
					if (columnInfo.name.equals(keyColumnName)) {
						keyIndex = columnInfo.index;
						break;
					}
				}
				if (keyIndex == -1) {
					logger.error("No keyColumnName {}", keyColumnName);
					throw new SecuritiesException("No keyColumnName");
				}
			}
			
			{
				int row = dataRow.value();
				for(;;) {
					String key;
					{
						XCell cell = xSpreadsheet.getCellByPosition(keyIndex, row);
						CellContentType type = cell.getType();
						if (type.equals(CellContentType.EMPTY)) break;
						
						XText text = UnoRuntime.queryInterface(XText.class, cell);
						key = text.getString();
					}
					
					E data = dataMap.get(key);
					if (data == null) {
						logger.error("Unknonw key {}", key);
						throw new SecuritiesException("Unknown key");
					}
					
					for(ColumnInfo columnInfo: columnInfoList) {
						int   column = columnInfo.index;
						XCell cell   = xSpreadsheet.getCellByPosition(column, row);
						
						Field        field        = columnInfo.field;
						Class<?>     fieldType    = field.getType();						
						NumberFormat numberFormat = field.getDeclaredAnnotation(NumberFormat.class);
						
						if (numberFormat != null) {
							spreadSheet.setNumberFormat(cell, numberFormat.value());
						}
						
						if (fieldType.equals(String.class)) {
							Object value = field.get(data);
							cell.setFormula(value.toString());
						} else if (fieldType.equals(Integer.TYPE)) {
							int value = field.getInt(data);
							cell.setValue(value);
						} else if (fieldType.equals(Double.TYPE)) {
							double value = field.getDouble(data);
							cell.setValue(value);
						} else {
							logger.error("Unknow field type = {}", fieldType.getName());
							throw new SecuritiesException("Unexpected");
						}
					}
					
					row++;
				}
			}
		} catch (IndexOutOfBoundsException | IllegalArgumentException | IllegalAccessException e) {
			logger.error("Exception {}", e.toString());
			throw new SecuritiesException("Unexpected");
		}
	}
}
