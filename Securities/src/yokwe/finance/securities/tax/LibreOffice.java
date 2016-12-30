package yokwe.finance.securities.tax;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.sun.star.beans.PropertyState;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.document.UpdateDocMode;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XModel;
import com.sun.star.frame.XStorable;
import com.sun.star.io.IOException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
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
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.CloseVetoException;
import com.sun.star.util.XCloseable;
import com.sun.star.util.XNumberFormats;
import com.sun.star.util.XNumberFormatsSupplier;

import yokwe.finance.securities.SecuritiesException;

public class LibreOffice implements Closeable {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(LibreOffice.class);

	private static final String[] bootstrapOptions = {
			"--minimized",
			"--headless",
			"--invisible",
	};
	
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
	
	private static final XComponentLoader componentLoader;

	static {
		XComponentLoader temp = null;
		try {
			XComponentContext componentContext = Bootstrap.bootstrap(bootstrapOptions);
			XMultiComponentFactory serviceManager = componentContext.getServiceManager();
			Object desktop = serviceManager.createInstanceWithContext("com.sun.star.frame.Desktop", componentContext);
			temp = UnoRuntime.queryInterface(XComponentLoader.class, desktop);
		} catch (BootstrapException | com.sun.star.uno.Exception e) {
			logger.info("Exception {}", e.toString());
			temp = null;
		} finally {
			componentLoader = temp;
		}
	}
	
	private final XComponent component;
		
	public LibreOffice(String url, boolean readOnly) {
		try {
			PropertyValue[] props = new PropertyValue[] {
					// Set document as read only
					new PropertyValue("ReadOnly",      0, readOnly,                PropertyState.DIRECT_VALUE),
					// Choose NO_UPDATE for faster operation
					new PropertyValue("UpdateDocMode", 0, UpdateDocMode.NO_UPDATE, PropertyState.DIRECT_VALUE),
			};

			component = componentLoader.loadComponentFromURL(url, "_blank", 0, props);
		} catch (IllegalArgumentException | IOException e) {
			logger.info("Exception {}", e.toString());
			throw new SecuritiesException("Unexpected exception");
		}
	}
	
	public static String NEW_SPREADSHEET_URL = "private:factory/scalc";
	public static LibreOffice getNewSpreadSheet() {
		LibreOffice libreOffice = new LibreOffice(NEW_SPREADSHEET_URL, false);
		return libreOffice;
	}
	
	
	public XComponent getComponent() {
		return component;
	}
	public XSpreadsheetDocument getSpreadSheetDocument() {
		XSpreadsheetDocument spreadsheetDocument = UnoRuntime.queryInterface(XSpreadsheetDocument.class, component);
		if (spreadsheetDocument == null) {
			logger.info("component {}", component.toString());
			throw new SecuritiesException("Unexpected component");
		}
		return spreadsheetDocument;
	}
	public XSpreadsheets2 getSpreadSheets() {
		XSpreadsheetDocument spreadsheetDocument = getSpreadSheetDocument();
		XSpreadsheets        spreadSheets        = spreadsheetDocument.getSheets();
		XSpreadsheets2       spreadSheets2       = UnoRuntime.queryInterface(XSpreadsheets2.class, spreadSheets);
		if (spreadSheets2 == null) {
			logger.info("spreadSheets2 == null {}", component.toString());
			throw new SecuritiesException("spreadSheets2 == null");
		}
		return spreadSheets2;
	}
	public void importSheet(XSpreadsheetDocument oldDoc, String oldName, int newSheetPosition) {
		XSpreadsheets2 spreadSheets = getSpreadSheets();
		if (spreadSheets == null) {
			logger.info("component {}", component.toString());
			throw new SecuritiesException("Unexpected component");
		}
		try {
			spreadSheets.importSheet(oldDoc, oldName, newSheetPosition);
		} catch (IllegalArgumentException e) {
			logger.info("IllegalArgumentException {}", e.toString());
			throw new SecuritiesException("IllegalArgumentException");
		} catch (IndexOutOfBoundsException e) {
			logger.info("IndexOutOfBoundsException {}", e.toString());
			throw new SecuritiesException("IndexOutOfBoundsException");
		}
	}
	public void copyByName(String oldSheetName, String newSheetName, int newSheetPosition) {
		XSpreadsheets2 spreadSheets = getSpreadSheets();
		spreadSheets.copyByName(oldSheetName, newSheetName, (short)newSheetPosition);
	}
	public void moveByName(String sheetName, int sheetPosition) {
		XSpreadsheets2 spreadSheets = getSpreadSheets();
		spreadSheets.moveByName(sheetName, (short)sheetPosition);
	}
	public void removeByName(String sheetName) {
		XSpreadsheets2 spreadSheets = getSpreadSheets();
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
	public XSpreadsheet getSpreadSheet(String name) {
		try {
			XNameAccess nameAccess = UnoRuntime.queryInterface(XNameAccess.class, getSpreadSheets());
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
	public int countSheet() {
		XIndexAccess indexAccess = UnoRuntime.queryInterface(XIndexAccess.class, getSpreadSheets());
		return indexAccess.getCount();
	}
	public String getSheetName(int index) {
		XNameAccess nameAccess = UnoRuntime.queryInterface(XNameAccess.class, getSpreadSheets());
		String[] names = nameAccess.getElementNames();
		if (0 <= index && index <= names.length) {
			return names[index];
		}
		logger.info("Index out of range {}", index);
		throw new SecuritiesException("Index out of range");
	}
	
	
	public XSpreadsheet getSpreadSheet(int index) {
		try {
			XIndexAccess indexAccess = UnoRuntime.queryInterface(XIndexAccess.class, getSpreadSheets());
			XSpreadsheet sheet = UnoRuntime.queryInterface(XSpreadsheet.class, indexAccess.getByIndex(index));
			return sheet;
		} catch (IndexOutOfBoundsException | WrappedTargetException e) {
			logger.info("Exception {}", e.toString());
			throw new SecuritiesException("Unexpected exception");
		}
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
	
	public void close() {
		XCloseable closeable = null;
		{
			XModel xModel = UnoRuntime.queryInterface(XModel.class, component);
			if (xModel != null) {
				closeable = UnoRuntime.queryInterface(XCloseable.class, xModel);
			}
		}
		
		if (closeable != null) {
			try {
				closeable.close(true);
			} catch (CloseVetoException e) {
				logger.info("CloseVetoException {}", e.toString());
			}
		} else {
			component.dispose();
		}
	}
	
	// Store Document to URL
	public void store(String urlStore) {
		try {
			XStorable storable = UnoRuntime.queryInterface(XStorable.class, getComponent());
			PropertyValue[] values = new PropertyValue[] {
				new PropertyValue("Overwrite", 0, true, PropertyState.DIRECT_VALUE),
			};
			storable.storeAsURL(urlStore, values);
		} catch (IOException e) {
			logger.info("Exception {}", e.toString());
			throw new SecuritiesException("Unexpected exception");
		}
	}

}
