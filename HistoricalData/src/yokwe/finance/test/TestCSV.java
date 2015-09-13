package yokwe.finance.test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.etf.CSVUtil;

public class TestCSV {
	private static final Logger logger = LoggerFactory.getLogger(TestCSV.class);

	enum Field {
		DATE("Date"), OPEN("Open"), HIGH("High"), LOW("Low"), CLOSE("Close"), VOLUME("Volume");
		
		private final String name;
		private Field(String name) {
			this.name = name;
		}
		public String toString() {
			return name;
		}
	}
	public static void main(String[] args) {
		logger.info("START");
		try {
			FileReader reader = new FileReader("tmp/fetch/etf/yahoo-daily/QQQ.csv");
			//List<Map<Field, String>> records = load(reader, Field.class);
			List<Map<Field, String>>list = CSVUtil.load(reader, Field.class);
			logger.info("list {}", list.size());
			logger.info("{}", list.get(0));
		} catch (FileNotFoundException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
		}
		logger.info("STOP");
	}

}
