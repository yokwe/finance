package yokwe.finance.securities.quandl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.HttpUtil;

public class DatasetList {
	private static final Logger logger = LoggerFactory.getLogger(DatasetList.class);
	
	public static final String PATH_DIR = "metadata/datasets";
	
	private static final int BUFFER_SIZE = 64 * 1024;
	
	private static final CSVFormat csvFormat = CSVFormat.DEFAULT.withRecordSeparator("\n");

	
	public static String getPath(String database_code) {
		String path = String.format("%s/%s.csv", PATH_DIR, database_code);
		
		return Quandl.getPath(path);
	}
	
	//https://www.quandl.com/api/v3/datasets.json?database_code=ODA&page=1
	public static String getURL(String database_code, int page) {
		String path  = "datasets.json";
		String query = String.format("database_code=%s&page=%d", database_code, page);
		
		return Quandl.getURL(path, query);
	}

	public static String toString(String[] strings) {
		StringBuilder ret = new StringBuilder();
		for(int i = 0; i < strings.length; i++) {
			String string = strings[i];
			if (string.matches("[\\\",]")) {
				
			}
			if (0 < i) ret.append(",");
			
			ret.append(strings[i]);
		}
		
		return ret.toString();
	}
	
	public static class Entry {
		public int     id;
		public String  dataset_code;
		public String  database_code;
		public String  name;
		public String  description;
		public String  refreshed_at;
		public String  newest_available_date;
		public String  oldest_available_date;
		public String  column_names[];
		public String  frequency; // annual
		public String  type;      // Time Series
		public boolean premium;
		public int     database_id;
	}
	
	
	public List<Entry> datasets;
	public Quandl.Meta meta;
	
	private static void save(CSVPrinter csvPrinter, DatasetList datasets) throws IOException {
		for(Entry entry: datasets.datasets) {
			csvPrinter.printRecord(
					entry.id,
					entry.dataset_code,
					entry.database_code,
					entry.name,
					entry.description,
					entry.refreshed_at,
					entry.newest_available_date,
					entry.oldest_available_date,
					csvFormat.format((Object[])entry.column_names),
					entry.frequency,
					entry.type,
					entry.premium,
					entry.database_id
					);
		}
	}
	
	public static void save(CSVPrinter csvPrinter, String database_code) throws IOException {
		Gson gson = new Gson();

		int totalPages;
		int totalCount;

		{
			String url = getURL(database_code, 1);
//			logger.info("url = {}", url);
			String json = HttpUtil.downloadAsString(url);
//			logger.info("json = {}", json);
			DatasetList datasets = gson.fromJson(json, DatasetList.class);
			totalPages = datasets.meta.total_pages;
			totalCount = datasets.meta.total_count;
//			logger.info("totalPages = {}", totalPages);
			logger.info("totalCount = {}", totalCount);
			logger.info("{} / {}", 1, totalPages);
			save(csvPrinter, datasets);
		}
		
		for(int i = 2; i <= totalPages; i++) {
			String url = getURL(database_code, i);
//			logger.info("url = {}", url);
			String json = HttpUtil.downloadAsString(url);
//			logger.info("json = {}", json);
			DatasetList datasets = gson.fromJson(json, DatasetList.class);
			logger.info("{} / {}", i, totalPages);
			save(csvPrinter, datasets);
		}
		
	}

	public static void update() {
		logger.info("START");
		List<DatabaseList.Entry> databaseList = DatabaseList.load();
		
		// Create parent directory if it doesn't exist.
		{
			File dir = new File(PATH_DIR);
			if (!dir.exists()) dir.mkdirs();
		}
		
		for(DatabaseList.Entry database: databaseList) {
			String database_code = database.database_code;
			String path = getPath(database_code);
			File   file = new File(path);
			if (file.exists()) continue;
			
			logger.info("database_code {}", database_code);

			try (CSVPrinter csvPrint = new CSVPrinter(new BufferedWriter(new FileWriter(path), BUFFER_SIZE), csvFormat)) {
				save(csvPrint, database_code);
			} catch (IOException e) {
				logger.error("IOException {}", e.toString());
				throw new SecuritiesException("IOException");
			}
		}
		logger.info("STOP");
	}
	
	public static void main(String[] args) {
		update();
	}
}
