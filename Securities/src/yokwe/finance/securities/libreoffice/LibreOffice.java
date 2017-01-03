package yokwe.finance.securities.libreoffice;

import java.io.Closeable;

import org.slf4j.LoggerFactory;

import com.sun.star.beans.PropertyState;
import com.sun.star.beans.PropertyValue;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.document.UpdateDocMode;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XModel;
import com.sun.star.frame.XStorable;
import com.sun.star.io.IOException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.CloseVetoException;
import com.sun.star.util.XCloseable;

import yokwe.finance.securities.SecuritiesException;

public class LibreOffice implements Closeable {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(LibreOffice.class);

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
	
	protected final XComponent component;
		
	protected LibreOffice(String url, boolean readOnly) {
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
			XStorable storable = UnoRuntime.queryInterface(XStorable.class, component);
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
