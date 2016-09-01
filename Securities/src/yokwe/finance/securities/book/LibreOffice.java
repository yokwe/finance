package yokwe.finance.securities.book;

import java.io.Closeable;

import org.slf4j.LoggerFactory;

import com.sun.star.beans.PropertyState;
import com.sun.star.beans.PropertyValue;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.document.UpdateDocMode;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XModel;
import com.sun.star.io.IOException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheets;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.CloseVetoException;
import com.sun.star.util.XCloseable;

import yokwe.finance.securities.SecuritiesException;

public class LibreOffice implements Closeable {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(LibreOffice.class);

	private static final PropertyValue[] props = new PropertyValue[] {
			// Set document as read only
			new PropertyValue("ReadOnly", 0, true, PropertyState.DIRECT_VALUE),
			// Update linked reference
			new PropertyValue("UpdateDocMode", 0, UpdateDocMode.QUIET_UPDATE, PropertyState.DIRECT_VALUE),
	};
	
	private static final String[] bootstrapOptions = {
			"--minimized",
			"--headless",
			"--invisible",
	};
	
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
	
	public LibreOffice(String url) {
		try {
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
		XSpreadsheets ret = UnoRuntime.queryInterface(XSpreadsheetDocument.class, component).getSheets();
		if (ret == null) {
			logger.info("component {}", component.toString());
			throw new SecuritiesException("Unexpected component");
		}
		return ret;
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
}
