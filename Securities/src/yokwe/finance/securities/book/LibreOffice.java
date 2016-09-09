package yokwe.finance.securities.book;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.sun.star.beans.PropertyState;
import com.sun.star.beans.PropertyValue;
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
import com.sun.star.table.CellContentType;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheets;
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
		cellContentTypeMap.put(CellContentType.EMPTY, "EMPTY");
		cellContentTypeMap.put(CellContentType.FORMULA, "FORMULA");
		cellContentTypeMap.put(CellContentType.TEXT, "TEXT");
		cellContentTypeMap.put(CellContentType.VALUE, "VALUE");
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
	
	public static String NEW_SPREADSHEET_URL = "private:factory/scalc";
	
	public LibreOffice(String url, boolean readOnly) {
		try {
			PropertyValue[] props = new PropertyValue[] {
					// Set document as read only
					new PropertyValue("ReadOnly", 0, readOnly, PropertyState.DIRECT_VALUE),
					// Update linked reference
					new PropertyValue("UpdateDocMode", 0, UpdateDocMode.QUIET_UPDATE, PropertyState.DIRECT_VALUE),
			};

			component = componentLoader.loadComponentFromURL(url, "_blank", 0, props);
		} catch (IllegalArgumentException | IOException e) {
			logger.info("Exception {}", e.toString());
			throw new SecuritiesException("Unexpected exception");
		}
	}
	

	public XComponent getComponent() {
		return component;
	}
	public XSpreadsheets getSpreadSheets() {
		XSpreadsheets spreadSheets = UnoRuntime.queryInterface(XSpreadsheetDocument.class, component).getSheets();
		if (spreadSheets == null) {
			logger.info("component {}", component.toString());
			throw new SecuritiesException("Unexpected component");
		}
		return spreadSheets;
	}
	public XSpreadsheet getSpreadSheet(String name) {
		try {
			XNameAccess nameAccess = UnoRuntime.queryInterface(XNameAccess.class, getSpreadSheets());
			XSpreadsheet sheet = UnoRuntime.queryInterface(XSpreadsheet.class, nameAccess.getByName(name));
			return sheet;
		} catch (NoSuchElementException | WrappedTargetException e) {
			logger.info("Exception {}", e.toString());
			throw new SecuritiesException("Unexpected exception");
		}
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
	
	public XNumberFormats getNumberFormats() {
		XNumberFormatsSupplier numberFormatsSupplier = UnoRuntime.queryInterface(XNumberFormatsSupplier.class, component);
		return numberFormatsSupplier.getNumberFormats();
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
			storable.storeToURL(urlStore, values);
		} catch (IOException e) {
			logger.info("Exception {}", e.toString());
			throw new SecuritiesException("Unexpected exception");
		}
	}

}
