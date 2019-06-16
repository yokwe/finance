package yokwe.finance.stock.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.stock.UnexpectedException;

public class FileUtil {
	private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

	public static String read(String path) {
		File file = new File(path);
		if (file.canRead()) {
			return read(file);
		} else {
			logger.error("File cannot read.  path = {}", path);
			throw new UnexpectedException();
		}
	}

	public static String read(String path, String encoding) {
		File file = new File(path);
		if (file.canRead()) {
			return read(file, encoding);
		} else {
			logger.error("File cannot read.  path = {}", path);
			throw new UnexpectedException();
		}
	}

	public static String read(File file) {
		String encoding = Charset.defaultCharset().name();
		return read(file, encoding);
	}
	
	public static String read(File file, String encoding) {
		char[] buffer = new char[65536];		
		try (BufferedReader bfr = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding), buffer.length)) {
			StringBuilder ret = new StringBuilder();
			
			for(;;) {
				int len = bfr.read(buffer);
				if (len == -1) break;
				
				ret.append(buffer, 0, len);
			}
			return ret.toString();
		} catch (IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new UnexpectedException();
		}
	}
	
	public static void write(File file, String content) {
		char[] buffer = new char[65536];
		
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file), buffer.length)) {
			// Make parent directory if necessary.
			file.getParentFile().mkdirs();
			file.createNewFile();
			
			bw.append(content);
		} catch (IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new UnexpectedException();
		}
	}
}
