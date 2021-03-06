package yokwe.finance.etf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.slf4j.LoggerFactory;

public final class Util {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Util.class);

	public static String getContents(File file) {
		char[] buffer = new char[65536];
		
		StringBuilder ret = new StringBuilder();
		
		try (BufferedReader bfr = new BufferedReader(new FileReader(file), buffer.length)) {
			for(;;) {
				int len = bfr.read(buffer);
				if (len == -1) break;
				
				ret.append(buffer, 0, len);
			}
		} catch (IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new ETFException();
		}
		return ret.toString();
	}
}
