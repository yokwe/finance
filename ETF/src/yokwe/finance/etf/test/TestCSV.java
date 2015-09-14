package yokwe.finance.etf.test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.etf.util.CSVUtil;

public class TestCSV {
	static final Logger logger = LoggerFactory.getLogger(TestCSV.class);

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

	public static void main(String[] args) throws FileNotFoundException {
		FileReader reader = new FileReader("tmp/fetch/yahoo-daily/QQQ.csv");
		List<Map<Field, String>> records = CSVUtil.load(reader, Field.class);
		int count = 0;
		for(Map<Field, String> record: records) {
			count++;
			logger.info(String.format("%6d %s", count, record));
		}
		logger.info("size = {}", records.size());
	}
}
