package yokwe.finance.securities.book;

import org.slf4j.LoggerFactory;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNamed;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheets;
import com.sun.star.uno.UnoRuntime;

public class T001 {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(T001.class);
	
	public static void main(String[] args) {
		String url = "file:///home/hasegawa/Dropbox/Trade/投資損益計算_2016_SAVE.ods";
		
		logger.info("START");
		try (LibreOffice lo = new LibreOffice(url, true)) {
			XSpreadsheets spreadSheets = lo.getSpreadSheets();
			
			// Enumerate all sheet using name access
			{
				XNameAccess nameAccess = UnoRuntime.queryInterface(XNameAccess.class, spreadSheets);
				logger.info("name {}", nameAccess.getElementNames().length);
				try {
					for (String name : nameAccess.getElementNames()) {
						XSpreadsheet sheet;
							sheet = UnoRuntime.queryInterface(XSpreadsheet.class, nameAccess.getByName(name));
						XNamed named = UnoRuntime.queryInterface(XNamed.class, sheet);
						logger.info("Name {} - {}", name, named.getName());
					}
				} catch (NoSuchElementException | WrappedTargetException e) {
					logger.info("Exception {}", e.toString());
				}
			}
		}
		logger.info("STOP");
		
		System.exit(0);
	}
}
