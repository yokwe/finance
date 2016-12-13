package yokwe.finance.securities.quandl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.CSVUtil;
import yokwe.finance.securities.util.HttpUtil;

public class Metadata {
	static final Logger logger = LoggerFactory.getLogger(Metadata.class);
	
	public static class DatabaseList {
		public static class Entry {
			public int     id;
			public String  name;
			public String  database_code;
			public String  description;
			public int     dataset_count;
			public long    downloads; // need to be long
			public boolean premium;
			public String  image;
			public boolean favorite;
			public String  url_name;
			
			@Override
			public String toString() {
				return String.format("{%d %s}", id, database_code);
			}
			
			@Override
			public boolean equals(Object o) {
				if (o instanceof Entry) {
					final Entry that = (Entry)o;
//					if (this.id != that.id) logger.info("id {} {}", this.id, that.id);
//					if (!this.name.equals(that.name)) logger.info("name {} {}", this.name, that.name);
//					if (!this.database_code.equals(that.database_code)) logger.info("database_code {} {}", this.database_code, that.database_code);
//					if (!this.description.equals(that.description)) logger.info("description {} {}", this.description, that.description);
//					if (this.dataset_count != that.dataset_count) logger.info("dataset_count {} {}", this.dataset_count, that.dataset_count);
//					if (this.downloads != that.downloads) logger.info("downloads {} {}", this.downloads, that.downloads);
//					if (this.premium != that.premium) logger.info("premium {} {}", this.premium, that.premium);
//					if (!this.image.equals(that.image)) logger.info("image {} {}", this.image, that.image);
//					if (this.favorite != that.favorite) logger.info("favorite {} {}", this.favorite, that.favorite);
//					if (!this.url_name.equals(that.url_name)) logger.info("url_name {} {}", this.url_name, that.url_name);
					
					return this.id == that.id &&
							this.name.equals(that.name) &&
							this.database_code.equals(that.database_code) &&
							this.description.equals(that.description) &&
							this.dataset_count == that.dataset_count &&
							this.downloads == that.downloads &&
							this.premium == that.premium &&
							this.image.equals(that.image) &&
							this.favorite == that.favorite &&
							this.url_name.equals(that.url_name);
				} else {
					return false;
				}
			}
		}
		public static class Meta {
			public String query;
			public int    per_page;
			public int    current_page;
			public int    prev_page;
			public int    total_pages;
			public int    total_count;
			public int    next_page;
			public int    current_first_item;
			public int    current_last_item;
			
			@Override
			public String toString() {
				return String.format("{%d-%d-%d  %d-%d}", total_count, current_first_item, current_last_item, total_pages, current_page);
			}
		}
		
		public List<Entry> databases;
		public Meta        meta;
		
		@Override
		public String toString() {
			return String.format("{{%s} meta {%s}}", databases.toString(), meta.toString());
		}
		
		public static final String NAME   = "databases";
		public static final String FORMAT = "json";
		public static final String PATH   = "tmp/fetch/quandl/metadata/databases.csv";
		
		public static String getURL(int page) {
			return Quandl.getURL(NAME, FORMAT, page);
		}
		
		public static List<Entry> getAll() {
			Gson gson = new Gson();
			
			List<Entry> entries = new ArrayList<>();
			int totalPages;
			int totalCount;
			
			{
				String url = getURL(1);
//				logger.info("url = {}", url);
				String json = HttpUtil.downloadAsString(url);
//				logger.info("json = {}", json);
				DatabaseList database = gson.fromJson(json, DatabaseList.class);
				totalPages = database.meta.total_pages;
				totalCount = database.meta.total_count;
				logger.info("totalPages = {}", totalPages);
				logger.info("totalCount = {}", totalCount);
				
				entries.addAll(database.databases);
				logger.info("{} entries {}", 1, entries.size());
			}
			for(int i = 2; i <= totalPages; i++) {
				String url = getURL(i);
//				logger.info("url = {}", url);
				String json = HttpUtil.downloadAsString(url);
//				logger.info("json = {}", json);
				DatabaseList databases = gson.fromJson(json, DatabaseList.class);
				entries.addAll(databases.databases);
				logger.info("{} entries {}", i, entries.size());
			}
			
			Map<Integer, Entry> map = new TreeMap<>();
			for(Entry entry: entries) {
				Integer key = entry.id;
				if (map.containsKey(key)) {
//					logger.warn("duplicate  {}", entry.database_code);
					Entry that = map.get(key);
					if (!that.equals(entry)) {
						logger.info("Same id but different contents {}", key);
					}
				} else {
					map.put(entry.id, entry);
				}
			}
			logger.info("map {}", map.size());
			
			return new ArrayList<>(map.values());
		}
		
		public static void saveAll() {
			List<Entry> entryList = getAll();
			CSVUtil.saveWithHeader(entryList, PATH);
		}
		
		public static List<Entry> loadAll() {
			List<Entry> ret = CSVUtil.loadWithHeader(PATH, Entry.class);
			logger.info("size {}", ret.size());
			return ret;
		}
	}
	
	
	public static class DatasetList {
		public static class Entry {
			public String key;
			public String description;
		}
		
		public static String getURL(String database_code) {
			return Quandl.getURL(database_code);
		}

		public static void save(String database_code) {
			String url = getURL(database_code);
//			logger.info("url = {}", url);
			byte[] zipData  = HttpUtil.downloadAsByteArray(url);
//			logger.info("zpiData {}", zipData.length);
			
			try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
				for(;;) {
					ZipEntry zipEntry = zis.getNextEntry();
					if (zipEntry == null) break;
//					logger.info("zipEntry {} {}", zipEntry.getName(), zipEntry.getSize());
					
					// save content to file
					String outPath = String.format("tmp/fetch/quandl/metadata/%s", zipEntry.getName());
					logger.info("outPath {}", outPath);
					File outFile = new File(outPath);
					try (FileOutputStream fos = new FileOutputStream(outFile)) {
						byte[] buf = new byte[64 * 1024];
						for(;;) {
							int len = zis.read(buf, 0, buf.length);
							if (len == -1) break;
							fos.write(buf, 0, len);
						}
					}
					
					zis.closeEntry();
				}
			} catch (IOException e) {
				logger.error("IOException {}", e.toString());
				throw new SecuritiesException("IOException");
			}
		}
		
		public static void saveAll() {
			List<DatabaseList.Entry> databaseList = DatabaseList.getAll();
			int i = 1;
			for(DatabaseList.Entry database: databaseList) {
				logger.info("database {} / {}  {}", i++, databaseList.size(), database.database_code);
				DatasetList.save(database.database_code);
			}
		}
	}
	
	public static class Dataset {
		public static class Entry {
			public String key;
			public String description;
		}
		
		public static String getURL(String database_code) {
			return Quandl.getURL(database_code);
		}

	}
	
	
	public static void main(String[] args) {
		DatabaseList.saveAll();
		DatabaseList.loadAll();
	}
}
