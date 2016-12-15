package yokwe.finance.securities.quandl;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.FileUtil;
import yokwe.finance.securities.util.HttpUtil;

public class Dataset {
	private static final Logger logger = LoggerFactory.getLogger(Dataset.class);
	
	public static String getURL(String database_code, String dataset_code) {
		String path = String.format("datasets/%s/%s/metadata.json", database_code, dataset_code);
		return Quandl.getURL(path);
	}
	
	public static String getDirPath(String database_code) {
		return Quandl.getPath(String.format("metadata/datasets/%s", database_code));
	}
	public static String getPath(String database_code, String dataset_code) {
		return String.format("%s/%s.json", getDirPath(database_code), dataset_code);
	}

	public static class Entry {
		public int     database_id;
		public int     id;
		public String  database_code;
		public String  dataset_code;
		
		public String  name;
		public String  description;
		
		public String  refreshed_at;
		public String  newest_available_date;
		public String  odlest_available_date;
		
		public String  frequency;
		public String  type;
		public boolean premium;
		
		public String  column_names[];
		
		@Override
		public String toString() {
			return String.format("[%d %d %s %s  %s]", database_id, id, database_code, dataset_code, refreshed_at);
		}
	}
	
	public Entry dataset;

	
	
	public static Entry load(String database_code, String dataset_code) {
		Gson gson = new Gson();
		
		String path = getPath(database_code, dataset_code);
		String json = FileUtil.read(new File(path));
		Dataset dataset = gson.fromJson(json, Dataset.class);
		return dataset.dataset;
	}
	
	public static void save(Entry entry) {
		Gson gson = new Gson();
		
		String json = gson.toJson(entry);
		String path = getPath(entry.database_code, entry.dataset_code);
		FileUtil.write(new File(path), json);
	}
	
	public static void update(String database_code, String dataset_code) {
		Gson gson = new Gson();

		String url = getURL(database_code, dataset_code);
		String json = HttpUtil.downloadAsString(url);
//		logger.debug("url {}", url);
//		logger.debug("json {}", json);
		
		Dataset dataset = gson.fromJson(json, Dataset.class);
//		logger.debug("entry {}", dataset.dataset);
		save(dataset.dataset);
	}
	
	public static void update() {
		logger.info("START update");
		List<DatabaseList.Entry> databaseList = DatabaseList.load();
		int i = 0;
		int databaseListSize = databaseList.size();
		for(DatabaseList.Entry database: databaseList) {
			List<DatasetList.Entry> datasetList = DatasetList.load(database.database_code);
			int datasetListSize = datasetList.size();
			i++;
			int j = 0;
			for(DatasetList.Entry dataset: datasetList) {
				String[] keys = dataset.key.split("/");
				String database_code = keys[0];
				String dataset_code = keys[1];
				j++;
				
				// Sanity check
				if (!database_code.equals(database.database_code)) {
					logger.error("database {}", database.database_code);
					logger.error("key      {}", dataset.key);
					throw new SecuritiesException("Mismatch database_code");
				}
				
				// Skip if file exists.
				{
					String path = getPath(database_code, dataset_code);
					File file = new File(path);
					if (file.exists()) continue;
				}
				
				String message = String.format("%4d / %4d  %4d / %4d  %-16s %s", i, databaseListSize, j, datasetListSize, database_code, dataset_code);
				logger.info("{}", message);
				update(database_code, dataset_code);
			}
		}
		logger.info("STOP  update");
	}
	
	public static void main(String[] args) {
		update();
	}
}
