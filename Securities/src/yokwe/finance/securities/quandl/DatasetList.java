package yokwe.finance.securities.quandl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.CSVUtil;
import yokwe.finance.securities.util.HttpUtil;

public class DatasetList {
	private static final Logger logger = LoggerFactory.getLogger(DatasetList.class);

	public static class Entry {
		public String key;
		public String description;
	}
	
	public static final String PATH_DIR = Quandl.getPath("metadata/datasets/codes");
	
	public static String getURL(String database_code) {
		String path = String.format("databases/%s/codes", database_code);
		return Quandl.getURL(path);
	}
	
	public static String getPath(String database_code) {
		return String.format("%s/%s.csv", PATH_DIR, database_code);
	}

	public static void save(String database_code) {
		String url = getURL(database_code);
//		logger.info("url = {}", url);
		byte[] zipData  = HttpUtil.downloadAsByteArray(url);
		if (zipData == null) {
//			logger.warn("Failed to load {}", database_code);
			return;
		}
//		logger.info("zpiData {}", zipData.length);
		
		File dir = new File(PATH_DIR);
		dir.mkdirs();
		
		try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
			for(;;) {
				ZipEntry zipEntry = zis.getNextEntry();
				if (zipEntry == null) break;
//				logger.info("zipEntry {} {}", zipEntry.getName(), zipEntry.getSize());
				
				// save content to file
				String outPath = getPath(database_code);
//				logger.info("outPath {}", outPath);
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
	
	public static List<Entry> load(String database_code) {
		String path = getPath(database_code);
		List<Entry> entries = CSVUtil.load(path, Entry.class);
		return entries;
	}
	
	public static List<Entry> loadAll() {
		List<DatabaseList.Entry> databaseList = DatabaseList.getAll();
		logger.info("databaseList {}", databaseList.size());
		
		List<Entry> ret = new ArrayList<>();
		int i = 1;
		for(DatabaseList.Entry database: databaseList) {
			logger.info("database {} / {}  {}", i++, databaseList.size(), database.database_code);
			List<Entry> entries = load(database.database_code);
			ret.addAll(entries);
		}
		
		logger.info("ret {}", ret.size());
		return ret;
	}
	
	public static void update() {
		logger.info("START");
		List<DatabaseList.Entry> databaseList = DatabaseList.load();
		
		{
			int size = databaseList.size();
			int i = 0;
			for(DatabaseList.Entry entry: databaseList) {
				i++;
				String database_code = entry.database_code;
				String path = getPath(database_code);
				File file = new File(path);
				if (file.exists()) {
					logger.info("{}  check {}", String.format("%4d / %4d", i, size), database_code);
					// Sanity check
					// Try load csv file and make sure it can readable
					load(database_code);
				} else {
					logger.info("{}  save  {}", String.format("%4d / %4d", i, size), database_code);
					save(database_code);
					// save can failed. So don't do sanity check here.
				}
			}
		}
		
		// find removed entry from databaseList
		{
			Set<String> set = new TreeSet<>();
			for(DatabaseList.Entry entry: databaseList) {
				set.add(entry.database_code);
			}
			
			File[] files = new File(PATH_DIR).listFiles();
			for(File file: files) {
				String fileName = file.getName();
				if (fileName.endsWith(".csv")) {
					String database_code = fileName.substring(0, fileName.length() - 4);
					if (!set.contains(database_code)) {
						logger.info("This file is not in databaseList. remove {}", fileName);
						file.delete();
					}
				}
			}
		}
		logger.info("STOP");
	}
	
	public static void main(String[] args) {
		update();
	}
}
