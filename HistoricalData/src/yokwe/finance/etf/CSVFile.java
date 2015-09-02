package yokwe.finance.etf;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSVFile {
	private static final Logger logger = LoggerFactory.getLogger(CSVFile.class);
	
	public CSVFile() {}
	
	public static <F extends Enum<F>> void write(OutputStream outputStream, List<Map<F, String>> list) {
		if (list.size() == 0) return;
		
		try {
			PrintStream print = new PrintStream(outputStream);
			StringBuilder line = new StringBuilder();
			
			// Output header
			line.setLength(0);
			Map<F, String> zero = list.get(0);
			for(F key: zero.keySet()) {
				line.append(",").append(key.name());
			}
			print.println(line.substring(1));
			
			// Output data
			for(Map<F, String> t: list) {
				line.setLength(0);
				for(F key: zero.keySet()) {
					String value = t.get(key);
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

		} catch (IllegalArgumentException e) {
			logger.error("EXCEPTION {}", e.toString());
			throw new RuntimeException("EXCEPTION");
		}
	}
}
