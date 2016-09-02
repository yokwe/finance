package yokwe.finance.securities.book;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.sheet.XSheetCellRange;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellContentType;
import com.sun.star.table.XCell;
import com.sun.star.text.XText;
import com.sun.star.uno.UnoRuntime;

import yokwe.finance.securities.SecuritiesException;

public class SheetData {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(SheetData.class);
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public static @interface SheetName {
		String value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface ColumnName {
		String value();
	}
	
	public static <E extends SheetData> List<E> getInstance(XSpreadsheet spreadsheet, Class<E> clazz) {
		SheetName sheetName = clazz.getDeclaredAnnotation(SheetName.class);
		if (sheetName == null) {
			logger.error("No SheetName annotation = {}", clazz.getName());
			throw new SecuritiesException("No SheetName annotation");
		}
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
		XSheetCellRange cellRange = spreadsheet.getSpreadsheet();
		// Build columnMap - column name to column index
		Map<String, Integer> columnMap = new HashMap<>();
		{
			try {
				// Build header map
				for(int i = 0; i < 100; i++) {
					final XCell cell = cellRange.getCellByPosition(i, 0);
					final CellContentType type = cell.getType();
					if (type.equals(CellContentType.EMPTY)) break;
					
					XText text = UnoRuntime.queryInterface(XText.class, cell);
					String value = text.getString();
					columnMap.put(value, i);
					logger.info("{} - {} {}", i, LibreOffice.toString(type), value);
				}
			} catch (IndexOutOfBoundsException e) {
				logger.info("Exception {}", e.toString());
			}
			
			// Sanity check
			for(String name: fieldMap.keySet()) {
				if (columnMap.containsKey(name)) continue;
				logger.error("columnMap contains no field name = {}", name);
				throw new SecuritiesException("Unexpected");
			}
		}
		
		// Build ret
		List<E> ret = new ArrayList<>();
		{
			try {
				// Build header map
				for(int i = 1; i < 65535; i++) {
					final XCell firstCell = cellRange.getCellByPosition(0, i);
					if (firstCell.getType().equals(CellContentType.EMPTY)) break;
					
					E instance = clazz.newInstance();
					for(String columnName: fieldMap.keySet()) {
						Field field = fieldMap.get(columnName);
						int index = columnMap.get(columnName);
						XCell cell = cellRange.getCellByPosition(index, i);
						CellContentType cellType = cell.getType();
						
						Class<?> fieldType = field.getType();
						if (fieldType.equals(String.class)) {
							// String
							if (cellType.equals(CellContentType.TEXT)) {
								XText text = UnoRuntime.queryInterface(XText.class, cell);
								field.set(instance, text.getString());
							} else if (cellType.equals(CellContentType.EMPTY)) {
								field.set(instance, "");
							} else if (cellType.equals(CellContentType.VALUE)) {
								XText text = UnoRuntime.queryInterface(XText.class, cell);
								field.set(instance, text.getString());
							} else {
								logger.error("cellType = {}", LibreOffice.toString(cellType));
								throw new SecuritiesException("Unexpected");
							}
						} else if (fieldType.equals(Integer.TYPE)) {
							// int
							if (cellType.equals(CellContentType.VALUE)) {
								double value = cell.getValue();
								field.set(instance, (int)value);
							} else if (cellType.equals(CellContentType.EMPTY)) {
								field.set(instance, 0);
							} else {
								logger.error("cellType = {}", LibreOffice.toString(cellType));
								throw new SecuritiesException("Unexpected");
							}
						} else if (fieldType.equals(Double.TYPE)) {
							// double
							if (cellType.equals(CellContentType.VALUE)) {
								double value = cell.getValue();
								field.set(instance, value);
							} else if (cellType.equals(CellContentType.EMPTY)) {
								field.set(instance, 0);
							} else if (cellType.equals(CellContentType.FORMULA)) {
								double value = cell.getValue();
								field.set(instance, value);
							} else {
								logger.error("cellType = {}", LibreOffice.toString(cellType));
								throw new SecuritiesException("Unexpected");
							}
						} else {
							logger.error("Unknow field type = {}", fieldType.getName());
							throw new SecuritiesException("Unexpected");
						}
					}
					
					ret.add(instance);
				}
				return ret;
			} catch (IndexOutOfBoundsException | InstantiationException | IllegalAccessException e) {
				logger.info("Exception {}", e.toString());
				throw new SecuritiesException("Unexpected");
			}
		}
	}

	public static void main(String[] args) {
		String url = "file:///home/hasegawa/Dropbox/Trade/投資損益計算_2016_SAVE.ods";
		
		logger.info("START");
		try (LibreOffice lo = new LibreOffice(url)) {			
			XSpreadsheet spreadsheet = lo.getSpreadSheet("売買履歴");
			
			List<Transaction> transactionList = SheetData.getInstance(spreadsheet, Transaction.class);
			for(Transaction transaction: transactionList) {
				logger.info("{}", transaction);
			}
		}
	}
}
