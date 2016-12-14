package yokwe.finance.securities.quandl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
	
	public static final String PATH_DIR = "tmp/fetch/quandl/metadata/datasets/codes";
	
	public static String getURL(String database_code) {
		return Quandl.getURL(database_code);
	}
	
	public static String getPath(String database_code) {
		return String.format("%s/%s.csv", PATH_DIR, database_code);
	}

	public static void save(String database_code) {
		String url = getURL(database_code);
//		logger.info("url = {}", url);
		byte[] zipData  = HttpUtil.downloadAsByteArray(url);
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
	
	public static List<Entry> loadAll() {
		File[] files = new File(PATH_DIR).listFiles((dir, name) -> name.endsWith(".csv"));
		logger.info("files {}", files.length);
		
		List<Entry> ret = new ArrayList<>();
		int i = 0;
		for(File file: files) {
			logger.info("{}  {}", ++i, file.getName());
			List<Entry> entries = CSVUtil.load(file.getPath(), Entry.class);
			ret.addAll(entries);
		}
		
		// data size is too large to load in memory
		
		logger.info("ret {}", ret.size());
		return ret;
	}
	
	public static void update() {
		logger.info("START");
		List<DatabaseList.Entry> databaseList = DatabaseList.load();
		int size = databaseList.size();
		int i = 0;
		for(DatabaseList.Entry entry: databaseList) {
			i++;
			String database_code = entry.database_code;
			String outPath = getPath(database_code);
			File file = new File(outPath);
			if (file.exists()) continue;
			
			logger.info("{} / {}  save {}", i, size, outPath);
			save(database_code);
		}
		logger.info("STOP");
	}
	
	public static void main(String[] args) {
		update();
	}
}
