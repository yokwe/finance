package yokwe.finance.securities.libreoffice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.sun.star.util.XNumberFormats;

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
	
	private static final int HASHCODE_CLASS_INTEGER = Integer.class.hashCode();
	private static final int HASHCODE_CLASS_DOUBLE  = Double.class.hashCode();
	private static final int HASHCODE_CLASS_LONG    = Long.class.hashCode();
	private static final int HASHCODE_CLASS_STRING  = String.class.hashCode();
	private static final int HASHCODE_INT           = Integer.TYPE.hashCode();
	private static final int HASHCODE_DOUBLE        = Double.TYPE.hashCode();
	private static final int HASHCODE_LONG          = Long.TYPE.hashCode();
	
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
				
				if (fieldTypeHash == HASHCODE_CLASS_STRING) {
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
								field.set(instance, SpreadSheet.toDateString((Double)value));
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
				} else if (fieldTypeHash == HASHCODE_DOUBLE) {
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
				} else if (fieldTypeHash == HASHCODE_INT) {
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

	
	private static class ColumnInfo {
		public static final int MAX_COLUMN = 99;
		
		public final String   name;
		public final int      index;
		public final Field    field;
		public final int      fieldType;					
		public final String   numberFormat;
		public final boolean  isDate;
		
		public ColumnInfo(String name, int index, Field field) {
			this.name         = name;
			this.index        = index;
			this.field        = field;
			this.fieldType    = field.getType().hashCode();
			
			NumberFormat numberFormat = field.getDeclaredAnnotation(NumberFormat.class);
			this.numberFormat = (numberFormat == null) ? null : numberFormat.value();
			this.isDate       = SpreadSheet.FORMAT_DATE.equals(this.numberFormat);
		}
		
		public static List<ColumnInfo> getColumnInfoList(XSpreadsheet xSpreadsheet, int headerRow, Field[] fields) {
			try {
				// build fieldMap
				Map<String, Field> fieldMap = new TreeMap<>();
				for(Field field: fields) {
					ColumnName columnName = field.getDeclaredAnnotation(ColumnName.class);
					if (columnName == null) continue;
					fieldMap.put(columnName.value(), field);
				}
				
				XCellRange cellRange = xSpreadsheet.getCellRangeByPosition(0, headerRow, MAX_COLUMN, headerRow);
				XCellRangeData cellRangeData = UnoRuntime.queryInterface(XCellRangeData.class, cellRange);
				Object data[][] = cellRangeData.getDataArray();

				// build columnInfoList
				List<ColumnInfo> columnInfoList = new ArrayList<>();
				int dataSize = data[0].length;
				for(int index = 0; index < dataSize; index++) {
					String name = data[0][index].toString();
					if (name.length() == 0) continue;
					
					Field field = fieldMap.get(name);
					if (field == null) {
						logger.warn("No field {}", name);
						continue;
					}
					
					ColumnInfo columnInfo = new ColumnInfo(name, index, field);
					columnInfoList.add(columnInfo);
				}
				
				return columnInfoList;
			} catch (IndexOutOfBoundsException e) {
				logger.error("Exception {}", e.toString());
				throw new SecuritiesException("Unexpected");
			}
		}
		public static ColumnInfo findByName(List<ColumnInfo> columnInfoList, String name) {
			for(ColumnInfo columnInfo: columnInfoList) {
				if (columnInfo.name.equals(name)) return columnInfo;
			}
			logger.error("Unknown name {}", name);
			throw new SecuritiesException("Unknown name");
		}
	}
	private static class RowRange {
		public static final int MAX_ROW = 9999;
		
		public final int      rowBegin;
		public final int      rowEnd;
		public final int      rowSize;
		public final String[] keys;
		
		public RowRange(int rowBegin, int rowEnd, String[] keys) {
			this.rowBegin = rowBegin;
			this.rowEnd   = rowEnd;
			this.rowSize  = rowEnd - rowBegin + 1;
			this.keys     = keys;
		}
		
		public static List<RowRange> getRowRangeList(XSpreadsheet xSpreadsheet, int keyColumn, int dataRow) {
			try {
				XCellRange cellRange = xSpreadsheet.getCellRangeByPosition(keyColumn, dataRow, keyColumn, dataRow + MAX_ROW);
				XCellRangeData cellRangeData = UnoRuntime.queryInterface(XCellRangeData.class, cellRange);
				Object data[][] = cellRangeData.getDataArray();
				
				// Build rowRangeList
				List<RowRange> rowRangeList = new ArrayList<>();
				int row = 0;
				for(;;) {
					if (MAX_ROW <= row) break; // reached to end

					int rowBegin = -1;
					int rowEnd   = -1;
					List<String> keyList = new ArrayList<>();
					
					// Skip empty value
					for(; row < MAX_ROW; row++) {
						String value = data[row][0].toString();
						if (0 < value.length()) break;
					}
					if (MAX_ROW <= row) break; // reached to end
					rowBegin = dataRow + row;
					
					// add until empty value
					for(; row < MAX_ROW; row++) {
						String value = data[row][0].toString();
						if (value.length() == 0) break;
						keyList.add(value);
					}
					rowEnd = dataRow + row - 1;
					
					RowRange rowRange = new RowRange(rowBegin, rowEnd, keyList.toArray(new String[0]));
					rowRangeList.add(rowRange);
				}
				return rowRangeList;
			} catch (IndexOutOfBoundsException e) {
				logger.error("Exception {}", e.toString());
				throw new SecuritiesException("Unexpected");
			}
		}

		public static List<RowRange> getRowRangeList(int rowBegin, int rowEnd) {
			List<RowRange> rowRangeList = new ArrayList<>();
			
			RowRange rowRange = new RowRange(rowBegin, rowEnd, null);
			rowRangeList.add(rowRange);
			
			return rowRangeList;
		}
	}
	public static <E extends Sheet> void fillSheet(SpreadSheet spreadSheet, Map<String, E> dataMap, String keyColumnName, String sheetName) {
		final XSpreadsheet   xSpreadsheet   = spreadSheet.getSheet(sheetName);
		final XNumberFormats xNumberFormats = spreadSheet.getNumberFormats();
		final int            headerRow;
		final int            dataRow;
		final Field[]        fields;
		
		{
			E o = dataMap.values().iterator().next();
			HeaderRow headerRowAnnotation = o.getClass().getDeclaredAnnotation(HeaderRow.class);
			DataRow   dataRowAnnotation   = o.getClass().getDeclaredAnnotation(DataRow.class);
			
			if (headerRowAnnotation == null) {
				logger.error("No HeaderRow annotation = {}", o.getClass().getName());
				throw new SecuritiesException("No HeaderRow annotation");
			}
			if (dataRowAnnotation == null) {
				logger.error("No DataRow annotation = {}", o.getClass().getName());
				throw new SecuritiesException("No DataRow annotation");
			}
			headerRow = headerRowAnnotation.value();
			dataRow   = dataRowAnnotation.value();
			fields    = o.getClass().getDeclaredFields();
		}

		{
			List<ColumnInfo> columnInfoList = ColumnInfo.getColumnInfoList(xSpreadsheet, headerRow, fields);
			
			// Build rowRangeList
			ColumnInfo     keyColumn    = ColumnInfo.findByName(columnInfoList, keyColumnName);
			List<RowRange> rowRangeList = RowRange.getRowRangeList(xSpreadsheet, keyColumn.index, dataRow);
			
			// Remove keyColumn to prevent from update
			columnInfoList.remove(keyColumn);
			
			// Build fillMap
			Map<String, Object> fillMap = new HashMap<>();
			for(RowRange rowRange: rowRangeList) {
				final int rowBegin = rowRange.rowBegin;
				final int rowSize  = rowRange.rowSize;
				
				for(int i = 0; i < rowSize; i++) {
					E data = dataMap.get(rowRange.keys[i]);
					if (data == null) {
						logger.warn("no entry in dataMap  key = {}", rowRange.keys[i]);
						continue;
					}
					buildFillMap(columnInfoList, rowBegin + i, data, fillMap);
				}
			}
			
			// Apply fillMap
			applyFillMap(xSpreadsheet, xNumberFormats, columnInfoList, rowRangeList, fillMap);
		}
	}

	public static <E extends Sheet> void fillSheet(SpreadSheet spreadSheet, List<E> dataList, String sheetName) {
		final XSpreadsheet   xSpreadsheet   = spreadSheet.getSheet(sheetName);
		final XNumberFormats xNumberFormats = spreadSheet.getNumberFormats();
		final int            headerRow;
		final int            dataRow;
		final Field[]        fields;
		
		{
			E o = dataList.iterator().next();
			HeaderRow      headerRowAnnotation    = o.getClass().getDeclaredAnnotation(HeaderRow.class);
			DataRow        dataRowAnnotation      = o.getClass().getDeclaredAnnotation(DataRow.class);
			
			if (headerRowAnnotation == null) {
				logger.error("No HeaderRow annotation = {}", o.getClass().getName());
				throw new SecuritiesException("No HeaderRow annotation");
			}
			if (dataRowAnnotation == null) {
				logger.error("No DataRow annotation = {}", o.getClass().getName());
				throw new SecuritiesException("No DataRow annotation");
			}
			headerRow = headerRowAnnotation.value();
			dataRow   = dataRowAnnotation.value();
			fields    = o.getClass().getDeclaredFields();
		}
		
		{
			List<ColumnInfo> columnInfoList = ColumnInfo.getColumnInfoList(xSpreadsheet, headerRow, fields);
			
			// Build rowRangeList
			List<RowRange> rowRangeList = RowRange.getRowRangeList(dataRow, dataRow + dataList.size() - 1);
			
			// Build fillMap
			Map<String, Object> fillMap = new HashMap<>();
			for(RowRange rowRange: rowRangeList) {
				final int rowBegin = rowRange.rowBegin;
				final int rowSize  = rowRange.rowSize;
				
				for(int i = 0; i < rowSize; i++) {
					E data = dataList.get(i);
					buildFillMap(columnInfoList, rowBegin + i, data, fillMap);
				}
			}
			
			// Apply fillMap
			applyFillMap(xSpreadsheet, xNumberFormats, columnInfoList, rowRangeList, fillMap);
		}
	}
	
	private static <E extends Sheet> void buildFillMap(List<ColumnInfo> columnInfoList, int row, E data, Map<String, Object> fillMap) {
		try {
			for(ColumnInfo columnInfo: columnInfoList) {
				String fillMapKey = row + "-" + columnInfo.index;
				
				// Type of value must be String or Double
				Object value;
				{
					Object o = columnInfo.field.get(data);
					if (o == null) {
						value = "";
					} else {
						if (columnInfo.fieldType == HASHCODE_CLASS_STRING) {
							String string = o.toString();
							if (columnInfo.isDate && string.length() == SpreadSheet.FORMAT_DATE.length()) {
								value = Double.valueOf(SpreadSheet.toDateNumber(string)); // Convert to double for date number
							} else {
								value = string;
							}
						} else if (columnInfo.fieldType == HASHCODE_CLASS_DOUBLE) {
							value = Double.valueOf((Double)o);
						} else if (columnInfo.fieldType == HASHCODE_CLASS_INTEGER) {
							value = Double.valueOf((Integer)o);
						} else if (columnInfo.fieldType == HASHCODE_CLASS_LONG) {
							value = Double.valueOf((Long)o);
						} else if (columnInfo.fieldType == HASHCODE_DOUBLE) {
							value = columnInfo.field.getDouble(data);
						} else if (columnInfo.fieldType == HASHCODE_INT) {
							value = Double.valueOf(columnInfo.field.getInt(data));  // Convert to double for numeric value
						} else if (columnInfo.fieldType == HASHCODE_LONG) {
							value = Double.valueOf(columnInfo.field.getLong(data)); // Convert to double for numeric value
						} else {
							logger.error("Unknow field type = {}", columnInfo.field.getType().getName());
							throw new SecuritiesException("Unexpected");
						}
					}
				}
				fillMap.put(fillMapKey, value);
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			logger.error("Exception {}", e.toString());
			throw new SecuritiesException("Unexpected");
		}
	}

	private static <E extends Sheet> void applyFillMap(XSpreadsheet xSpreadsheet, XNumberFormats xNumberFormats, List<ColumnInfo> columnInfoList, List<RowRange> rowRangeList, Map<String, Object> fillMap) {
		try {
			for(RowRange rowRange: rowRangeList) {
				final int rowBegin = rowRange.rowBegin;
				final int rowEnd   = rowRange.rowEnd;
				final int rowSize  = rowRange.rowSize;

				for(ColumnInfo columnInfo: columnInfoList) {
					// left top right bottom
					XCellRange xCellRange = xSpreadsheet.getCellRangeByPosition(columnInfo.index, rowBegin, columnInfo.index, rowEnd);
					
					// apply numberFormat
					if (columnInfo.numberFormat != null) {
						SpreadSheet.setNumberFormat(xCellRange, columnInfo.numberFormat, xNumberFormats);
					}
					
					// fill data
					XCellRangeData xCellRangeData = UnoRuntime.queryInterface(XCellRangeData.class, xCellRange);
					Object data[][] = new Object[rowSize][1]; // row column
					for(int i = 0; i < rowSize; i++) {
						String fillMapKey = (rowBegin + i) + "-" + columnInfo.index;
						Object value = fillMap.get(fillMapKey);
						data[i][0] = (value == null) ? "*NA*" : value;
					}
					xCellRangeData.setDataArray(data);
				}
			}
		} catch (IndexOutOfBoundsException e) {
			logger.error("Exception {}", e.toString());
			throw new SecuritiesException("Unexpected");
		}
	}
	public static <E extends Sheet> void fillSheet(SpreadSheet spreadSheet, List<E> dataList) {
		E o = dataList.iterator().next();
		String sheetName = getSheetName(o.getClass());
		fillSheet(spreadSheet, dataList, sheetName);
	}
}
