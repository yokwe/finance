package yokwe.finance.securities.quandl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import yokwe.finance.securities.util.HttpUtil;

public class DatabaseMetadata {
	static final Logger logger = LoggerFactory.getLogger(DatabaseMetadata.class);
	
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
	
	public static final String NAME = "databases";
	public static final String FORMAT = "json";
	
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
//			logger.info("url = {}", url);
			String json = HttpUtil.download(url);
			//logger.info("json = {}", json);
			DatabaseMetadata databases = gson.fromJson(json, DatabaseMetadata.class);
			totalPages = databases.meta.total_pages;
			totalCount = databases.meta.total_count;
			logger.info("totalPages = {}", totalPages);
			logger.info("totalCount = {}", totalCount);
			
			entries.addAll(databases.databases);
//			logger.info("{} entries {}", 1, entries.size());
		}
		for(int i = 2; i <= totalPages; i++) {
			String url = getURL(i);
//			logger.info("url = {}", url);
			String json = HttpUtil.download(url);
//			logger.info("json = {}", json);
			DatabaseMetadata databases = gson.fromJson(json, DatabaseMetadata.class);
			entries.addAll(databases.databases);
//			logger.info("{} entries {}", i, entries.size());
		}
		
		Map<String, Entry> map = new TreeMap<>();
		for(Entry entry: entries) {
			String key = entry.database_code;
			if (map.containsKey(key)) {
//				logger.warn("duplicate  {}", entry.database_code);
				Entry that = map.get(key);
				if (!that.equals(entry)) {
					logger.info("XXXX {}", key);
				}
				
			} else {
				map.put(entry.database_code, entry);
			}
		}
		logger.info("map {}", map.size());
		
		return new ArrayList<>(map.values());
	}

	
	public static void main(String[] args) {
		getAll();
	}
}
