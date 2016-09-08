package yokwe.finance.securities.book;

import org.slf4j.LoggerFactory;

import com.sun.star.beans.Property;
import com.sun.star.beans.PropertyState;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNamed;
import com.sun.star.frame.XStorable;
import com.sun.star.io.IOException;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheets;
import com.sun.star.table.XCell;
import com.sun.star.table.XCellRange;
import com.sun.star.uno.UnoRuntime;

public class T002 {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(T002.class);
	
	public static void main(String[] args) {
		String urlSave = "file:///home/hasegawa/Dropbox/Trade/T002_SAVE.ods";
		
		logger.info("START");
		try (LibreOffice libreOffice = new LibreOffice(LibreOffice.NEW_SPREADSHEET_URL, false)) {
			XSpreadsheet sheet = libreOffice.getSpreadSheet(0);
			
			// Change sheet name
			{
				XNamed named = UnoRuntime.queryInterface(XNamed.class, sheet);
				named.setName("XXX");
			}
			
			// Set cell value
			try {
				// Access cell by absolute position
				{
					sheet.getCellByPosition(0, 0).setFormula("abcd");
					sheet.getCellByPosition(1, 0).setValue(123.4);
				}
				
				// Access cell by relative positon of XCellRange
				{
					XCellRange cellRange = sheet.getCellRangeByPosition(1, 1, 4, 4); // left top right bottom
					cellRange.getCellByPosition(0, 0).setFormula("AAA"); // position 0-0 is relative to cellRange
				}
				
				// Access cell Property
				{
					XCell cell = sheet.getCellByPosition(0, 0);
					XPropertySet propSet = UnoRuntime.queryInterface(XPropertySet.class, cell);
					
					for(Property prop: propSet.getPropertySetInfo().getProperties()) {
						Object value = propSet.getPropertyValue(prop.Name);
						logger.info("prop  {} - {} - {}", prop.Name, value.getClass().getSimpleName(), value.toString());
					}
				}
				
				
			} catch (IndexOutOfBoundsException | UnknownPropertyException | WrappedTargetException e) {
				logger.info("Exception {}", e.toString());
			}

			// Save Document to URL
			{
				XStorable storable = UnoRuntime.queryInterface(XStorable.class, libreOffice.getComponent());
				PropertyValue[] values = new PropertyValue[] {
					new PropertyValue("Overwrite", 0, true, PropertyState.DIRECT_VALUE),
				};
				storable.storeToURL(urlSave, values);
			}
		} catch (IOException e) {
			logger.info("Exception {}", e.toString());
		}
		logger.info("STOP");
		
		System.exit(0);
	}
}
