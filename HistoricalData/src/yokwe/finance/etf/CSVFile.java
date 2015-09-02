package yokwe.finance.etf;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSVFile {
	private static final Logger logger = LoggerFactory.getLogger(CSVFile.class);
	
	public CSVFile() {}
	
	public static <T> List<T> read(Reader reader, Class<T> clazz) throws InstantiationException, IllegalAccessException {
		logger.info("XXX");
		
		T record = clazz.newInstance();
		
		for(Field field: clazz.getFields()) {
			String name = field.getName();
			String type = field.getType().getSimpleName();
			
			logger.debug("{}  {}", name, type);
		}
		
		
		
		List<T> ret = new ArrayList<T>();
		
		
		logger.info("YYY");
		return ret;
	}
	
	public static <T> void write(OutputStream outputStream, List<T> list) {
		if (list.size() == 0) return;
		
		try {
			Field[] fields = list.get(0).getClass().getFields();
			PrintStream print = new PrintStream(outputStream);
			StringBuilder line = new StringBuilder();
			
			// Output header
			line.setLength(0);
			for(Field field: fields) {
				line.append(",").append(field.getName());
			}
			print.println(line.substring(1));
			
			// Output data
			for(T t: list) {
				line.setLength(0);
				for(Field field: fields) {
					String value = field.get(t).toString();
					if (value.contains("\"")) {
						logger.error("DOUBLE_QUOTE  {}", value);
						throw new RuntimeException("DOUBLE_QUOTE");
					}
					if (value.contains(",")) value = "\"" + value + "\"";
					line.append(",").append(value);
				}
				print.println(line.substring(1));
			}
			print.flush();

		} catch (IllegalArgumentException | IllegalAccessException e) {
			logger.error("EXCEPTION {}", e.toString());
		}
	}
	
	public static class ABC {
		public int a;
		public String b;
		public int c;
		
		ABC(int a, String b, int c) {
			this.a = a;
			this.b = b;
			this.c = c;
		}
	}

	public static void main(String[] args) {
		logger.info("START");
		
		List<ABC> list = new ArrayList<>();
		list.add(new ABC(1, "One", 101));
		list.add(new ABC(2, "Two", 102));
		
		write(System.out, list);
		logger.info("STOP");
	}
}
