package yokwe.finance.securities.quandl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import yokwe.finance.securities.util.CSVUtil;
import yokwe.finance.securities.util.HttpUtil;

public class DatabaseList {
	private static final Logger logger = LoggerFactory.getLogger(DatabaseList.class);
	
	public static final String PATH  = Quandl.getPath("metadata/databases.csv");
	
	public static String getURL(int page) {
		String path  = "databases.json";
		String query = String.format("per_page=%d&page=%d", Quandl.PER_PAGE, page);
		
		return Quandl.getURL(path, query);
	}

	public static class Entry {
		public int     id;
		public String  name;
		public String  database_code;
		public String  description;
		public int     datasets_count;
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
//				if (this.id != that.id) logger.info("id {} {}", this.id, that.id);
//				if (!this.name.equals(that.name)) logger.info("name {} {}", this.name, that.name);
//				if (!this.database_code.equals(that.database_code)) logger.info("database_code {} {}", this.database_code, that.database_code);
//				if (!this.description.equals(that.description)) logger.info("description {} {}", this.description, that.description);
//				if (this.dataset_count != that.dataset_count) logger.info("dataset_count {} {}", this.dataset_count, that.dataset_count);
//				if (this.downloads != that.downloads) logger.info("downloads {} {}", this.downloads, that.downloads);
//				if (this.premium != that.premium) logger.info("premium {} {}", this.premium, that.premium);
//				if (!this.image.equals(that.image)) logger.info("image {} {}", this.image, that.image);
//				if (this.favorite != that.favorite) logger.info("favorite {} {}", this.favorite, that.favorite);
//				if (!this.url_name.equals(that.url_name)) logger.info("url_name {} {}", this.url_name, that.url_name);
				
				return this.id == that.id &&
						this.name.equals(that.name) &&
						this.database_code.equals(that.database_code) &&
						this.description.equals(that.description) &&
						this.datasets_count == that.datasets_count &&
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
	
	public List<Entry> databases;
	public Quandl.Meta meta;
	
	@Override
	public String toString() {
		return String.format("{{%s} meta {%s}}", databases.toString(), meta.toString());
	}
	
	public static List<Entry> getAll() {
		Gson gson = new Gson();
		
		List<Entry> entries = new ArrayList<>();
		int totalPages;
		int totalCount;
		
		{
			String url = getURL(1);
//			logger.info("url = {}", url);
			String json = HttpUtil.downloadAsString(url);
//			logger.info("json = {}", json);
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
//			logger.info("url = {}", url);
			String json = HttpUtil.downloadAsString(url);
//			logger.info("json = {}", json);
			DatabaseList databases = gson.fromJson(json, DatabaseList.class);
			entries.addAll(databases.databases);
			logger.info("{} entries {}", i, entries.size());
		}
		
		Map<Integer, Entry> map = new TreeMap<>();
		for(Entry entry: entries) {
			Integer key = entry.id;
			if (map.containsKey(key)) {
//				logger.warn("duplicate  {}", entry.database_code);
				Entry that = map.get(key);
				if (!that.equals(entry)) {
					logger.info("Same id but different contents {}", key);
				}
			} else {
				map.put(entry.id, entry);
			}
		}
		logger.info("all {}", map.size());
		
		// sort ret with id
		List<Entry> ret = new ArrayList<>(map.values());
		ret.sort((o1, o2) -> o1.id - o2.id);
		return ret;
	}
	
	public static void save(List<Entry> data) {
		CSVUtil.save(data, PATH);
	}
	
	public static List<Entry> load() {
		List<Entry> data = CSVUtil.load(PATH, Entry.class);
		return data;
	}
	
	public static void update() {
		logger.info("START update");
		Map<Integer, Entry> map = new TreeMap<>();
		for(Entry entry: load()) {
			entry.downloads = 0;
			map.put(entry.id, entry);
		}
		logger.info("entry {}", map.size());
		
		for(Entry entry: getAll()) {
			entry.downloads = 0;
			if (map.containsKey(entry.id)) {
				Entry old = map.get(entry.id);
				if (!entry.equals(old)) {
					logger.warn("same id but different values  {} {}", entry.id, entry.database_code);
				}
			} else {
				logger.info("add entry {} {}", entry.id, entry.database_code);
				map.put(entry.id, entry);
			}
		}
		logger.info("entry {}", map.size());
		
		// sort ret with id
		List<Entry> ret = new ArrayList<>(map.values());
		ret.sort((o1, o2) -> o1.id - o2.id);
		save(ret);
		logger.info("STOP  update");
	}
	
	public static void test() {
		logger.info("START");
		List<Entry> data1 = getAll();
		logger.info("data1.size {}", data1.size());
		List<Entry> data2 = load();
		logger.info("data2.size {}", data2.size());
		
		logger.info("verify data1 and data2");
		if (data1.size() != data2.size()) {
			logger.error("data1.size != data2.size  {} != {}", data1.size(), data2.size());
		} else {
			int size = data1.size();
			for(int i = 0; i < size; i++) {
				Entry entry1 = data1.get(i);
				Entry entry2 = data2.get(i);
				if (!entry1.equals(entry2)) {
					logger.error("entry1 != entry2  {} != {}", entry1, entry2);
				}
			}
		}
		logger.info("STOP");
	}
	
	public static void main(String[] args) {
		File file = new File(PATH);
		if (file.exists()) {
			update();
		} else {
			save(getAll());
		}
	}
}
