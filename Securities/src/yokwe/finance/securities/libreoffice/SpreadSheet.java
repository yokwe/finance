package yokwe.finance.securities.libreoffice;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.XCellRangeData;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheets;
import com.sun.star.sheet.XSpreadsheets2;
import com.sun.star.table.CellContentType;
import com.sun.star.table.XCell;
import com.sun.star.table.XCellRange;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XNumberFormats;
import com.sun.star.util.XNumberFormatsSupplier;

import yokwe.finance.securities.SecuritiesException;

public class SpreadSheet extends LibreOffice {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SpreadSheet.class);

	private static final Map<CellContentType, String> cellContentTypeMap = new HashMap<>();
	static {
		cellContentTypeMap.put(CellContentType.EMPTY,   "EMPTY");
		cellContentTypeMap.put(CellContentType.FORMULA, "FORMULA");
		cellContentTypeMap.put(CellContentType.TEXT,    "TEXT");
		cellContentTypeMap.put(CellContentType.VALUE,   "VALUE");
	}
	public static String toString(CellContentType type) {
		String ret = cellContentTypeMap.get(type);
		if (ret == null) {
			logger.info("Unexpected {}", type.toString());
			throw new SecuritiesException("Unexpected type");
		}
		return ret;
	}
	
	
	public SpreadSheet(String url, boolean readOnly) {
		super(url, readOnly);
	}
	
	public static final String NEW_SPREADSHEET_URL = "private:factory/scalc";
	public SpreadSheet() {
		super(NEW_SPREADSHEET_URL, false);
	}

	
	private XSpreadsheetDocument getXSpreadsheetDocument() {
		XSpreadsheetDocument spreadsheetDocument = UnoRuntime.queryInterface(XSpreadsheetDocument.class, component);
		if (spreadsheetDocument == null) {
			logger.info("component {}", component.toString());
			throw new SecuritiesException("Unexpected component");
		}
		return spreadsheetDocument;
	}
	public XSpreadsheets2 getSheets() {
		XSpreadsheetDocument spreadsheetDocument = getXSpreadsheetDocument();
		XSpreadsheets        spreadSheets        = spreadsheetDocument.getSheets();
		XSpreadsheets2       spreadSheets2       = UnoRuntime.queryInterface(XSpreadsheets2.class, spreadSheets);
		if (spreadSheets2 == null) {
			logger.info("spreadSheets2 == null {}", component.toString());
			throw new SecuritiesException("spreadSheets2 == null");
		}
		return spreadSheets2;
	}


	public int getSheetCount() {
		XIndexAccess indexAccess = UnoRuntime.queryInterface(XIndexAccess.class, getSheets());
		return indexAccess.getCount();
	}
	public int getSheetIndex(String sheetName) {
		XNameAccess nameAccess = UnoRuntime.queryInterface(XNameAccess.class, getSheets());
		String[] names = nameAccess.getElementNames();
		for(int i = 0; i < names.length; i++) {
			if (names[i].equals(sheetName)) return i;
		}
		logger.info("No sheet {}", sheetName);
		throw new SecuritiesException("No sheet");
	}
	public String getSheetName(int index) {
		XNameAccess nameAccess = UnoRuntime.queryInterface(XNameAccess.class, getSheets());
		String[] names = nameAccess.getElementNames();
		if (0 <= index && index <= names.length) {
			return names[index];
		}
		logger.info("Index out of range {}", index);
		throw new SecuritiesException("Index out of range");
	}

	public XSpreadsheet getSheet(int index) {
		try {
			XIndexAccess indexAccess = UnoRuntime.queryInterface(XIndexAccess.class, getSheets());
			XSpreadsheet sheet = UnoRuntime.queryInterface(XSpreadsheet.class, indexAccess.getByIndex(index));
			return sheet;
		} catch (IndexOutOfBoundsException | WrappedTargetException e) {
			logger.info("Exception {}", e.toString());
			throw new SecuritiesException("Unexpected exception");
		}
	}
	public XSpreadsheet getSheet(String name) {
		try {
			XNameAccess nameAccess = UnoRuntime.queryInterface(XNameAccess.class, getSheets());
			XSpreadsheet sheet = UnoRuntime.queryInterface(XSpreadsheet.class, nameAccess.getByName(name));
			return sheet;
		} catch (NoSuchElementException e) {
			logger.info("NoSuchElementException {}", e.toString());
			throw new SecuritiesException("NoSuchElementException");
		} catch (WrappedTargetException e) {
			logger.info("WrappedTargetException {}", e.toString());
			throw new SecuritiesException("WrappedTargetException");
		}
	}

	public void importSheet(SpreadSheet oldSheet, String oldName, int newSheetPosition) {
		XSpreadsheets2 spreadSheets = getSheets();
		if (spreadSheets == null) {
			logger.info("component {}", component.toString());
			throw new SecuritiesException("Unexpected component");
		}
		
		try {
			XSpreadsheetDocument oldDoc = oldSheet.getXSpreadsheetDocument();
			spreadSheets.importSheet(oldDoc, oldName, newSheetPosition);
		} catch (IllegalArgumentException e) {
			logger.info("IllegalArgumentException {}", e.toString());
			throw new SecuritiesException("IllegalArgumentException");
		} catch (IndexOutOfBoundsException e) {
			logger.info("IndexOutOfBoundsException {}", e.toString());
			throw new SecuritiesException("IndexOutOfBoundsException");
		}
	}
	
	public void renameSheet(String oldSheetName, String newSheetName) {
		int index = getSheetIndex(oldSheetName);
		copySheet(oldSheetName, newSheetName, index);
		removeSheet(oldSheetName);
	}
	public void copySheet(String oldSheetName, String newSheetName, int newSheetPosition) {
		XSpreadsheets2 spreadSheets = getSheets();
		spreadSheets.copyByName(oldSheetName, newSheetName, (short)newSheetPosition);
	}
	public void removeSheet(String sheetName) {
		XSpreadsheets2 spreadSheets = getSheets();
		try {
			spreadSheets.removeByName(sheetName);
		} catch (NoSuchElementException e) {
			logger.info("NoSuchElementException {}", e.toString());
			throw new SecuritiesException("NoSuchElementException");
		} catch (WrappedTargetException e) {
			logger.info("WrappedTargetException {}", e.toString());
			throw new SecuritiesException("WrappedTargetException");
		}
	}
	public void removeSheet(int index) {
		String sheetName = getSheetName(index);
		removeSheet(sheetName);
	}


	// column, rowFirst and rowLast are zero based
	public static int getLastDataRow(XSpreadsheet spreadsheet, int column, int rowFirst, int rowLast) {
		try {
			XCellRange cellRange = spreadsheet.getCellRangeByPosition(column, 0, column, rowLast);
			XCellRangeData cellRangeData = UnoRuntime.queryInterface(XCellRangeData.class, cellRange);
			Object data[][] = cellRangeData.getDataArray();
			int ret = -1;
			for(int i = rowFirst; i < data.length; i++) {
				Object o = data[i][0];
				// if o is empty string return ret
				if (o instanceof String && ((String)o).length() == 0) return ret;
				ret = i;
			}
			return ret;
		} catch (IndexOutOfBoundsException e) {
			logger.info("Exception {}", e.toString());
			throw new SecuritiesException("Unexpected exception");
		}
	}
	public XNumberFormats getNumberFormats() {
		XNumberFormatsSupplier numberFormatsSupplier = UnoRuntime.queryInterface(XNumberFormatsSupplier.class, component);
		return numberFormatsSupplier.getNumberFormats();
	}
	public String getFormatString(XCell cell) {
		try {
			XNumberFormats numberFormats     = getNumberFormats();
			XPropertySet   cellProps         = UnoRuntime.queryInterface(XPropertySet.class, cell);
			int            numberFormatIndex = AnyConverter.toInt(cellProps.getPropertyValue("NumberFormat"));
			XPropertySet   numberFormatProps = numberFormats.getByKey(numberFormatIndex);
			String         formatString      = AnyConverter.toString(numberFormatProps.getPropertyValue("FormatString"));
			return formatString;
		} catch (IllegalArgumentException | UnknownPropertyException | WrappedTargetException e) {
			logger.info("Exception {}", e.toString());
			throw new SecuritiesException("Unexpected exception");
		}
	}





}
