package yokwe.finance.securities.eod;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.FileUtil;
import yokwe.finance.securities.util.Pause;

public class UpdatePrice {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdatePrice.class);
	
	public static final String PATH_DIR = "tmp/eod/price";
	
	public static final String PATH_UNKNOWN_DIR = "tmp/eod/price-unknown";
	
	private static Map<String, UpdateProvider> updateProviderMap = new TreeMap<>();
	static {
		updateProviderMap.put(UpdateProvider.GOOGLE, new UpdateProviderGoogle());
		updateProviderMap.put(UpdateProvider.IEX,    new UpdateProviderIEX());
		updateProviderMap.put(UpdateProvider.YAHOO,  new UpdateProviderYahoo());
	}
	public static UpdateProvider getProvider(String provider) {
		if (updateProviderMap.containsKey(provider)) {
			return updateProviderMap.get(provider);
		} else {
			logger.error("Unknonw provider = {}", provider);
			throw new SecuritiesException("Unknonw provider");
		}
	}

	private static boolean needUpdate(File file) {
		// Don't update file after gracePeriod
//		if (UpdateProvider.GRACE_PERIOD < file.lastModified()) {
//			logger.info("Recently updated {}", file.getName());
//			return false;
//		}
			
		String content = FileUtil.read(file);
		String[] lines = content.split("\n");
		
		if (content.length() == 0) return true;
		
		// Sanity check
		if (lines.length <= 1) {
			logger.error("Unexpected content {}", content);
			throw new SecuritiesException("Unexpected content");
		}
		
		// first line should be header
		String HEADER = "date,symbol,open,high,low,close,volume";
		String header = lines[0];
		if (!header.equals(HEADER)) {
			logger.error("Unexpected header  {}", header);
			throw new SecuritiesException("Unexpected header");
		}
		
		// Does it contains data of DATE_LAST?
		String  dateLastString = UpdateProvider.DATE_LAST.toString();
		boolean needUpdate     = true;
		for(int i = 1; i < lines.length; i++) {
			String line = lines[i];
			String[] values = line.split(",");
			if (values.length != 7) {
				logger.error("Unexpected line  {}", line);
				throw new SecuritiesException("Unexpected header");
			}
			String date = values[0];
			if (date.equals(dateLastString)) {
				needUpdate = false;
				break;
			}
		}
		
		return needUpdate;
	}
	
	public static void updateFile(UpdateProvider updateProvider, Map<String, Stock> stockMap) {
		Set<String> symbolSet = new TreeSet<>(stockMap.keySet());
		logger.info("symbolSet {}", symbolSet.size());
		
		int total = stockMap.size();
		int countUpdate = 0;
		int countOld    = 0;
		int countSkip   = 0;
		int countNew    = 0;
		int countNone   = 0;
		
		int retryCount  = 0;
		boolean needSleep = false;

		for(;;) {
			retryCount++;
			if (UpdateProvider.MAX_RETRY < retryCount) break;
			logger.info("retry  {}", String.format("%4d", retryCount));
			
			if (needSleep) {
				try {
					logger.info("sleep");
					Thread.sleep(1 * 60 * 1000); // 1 minute
				} catch (InterruptedException e1) {
					logger.info("InterruptedException");
				}
			}
			
			int count = 0;
			int lastOutputCount = -1;
			Set<String> nextSymbolSet = new TreeSet<>();
			int lastCountOld = countOld;
			countOld = 0;

			int symbolSetSize = symbolSet.size();
			int showInterval = (symbolSetSize < 100) ? 1 : 100;
			
			Pause pause = updateProvider.getPause();
			logger.info("{}", pause);
			pause.reset();
						
			for(String symbol: symbolSet) {
				int outputCount = count / showInterval;
				boolean showOutput;
				if (outputCount != lastOutputCount) {
					showOutput = true;
					lastOutputCount = outputCount;
				} else {
					showOutput = false;
				}

				count++;
				
				File file = updateProvider.getFile(symbol);
				if (file.exists()) {
					if (needUpdate(file)) {
						pause.sleep();
						if (updateProvider.updateFile(symbol, false)) {
							if (showOutput) logger.info("{}  update {}", String.format("%4d / %4d",  count, symbolSetSize), symbol);
							countUpdate++;
						} else {
							if (showOutput) logger.info("{}  old    {}", String.format("%4d / %4d",  count, symbolSetSize), symbol);
							countOld++;
							nextSymbolSet.add(symbol);
						}
					} else {
						if (showOutput) logger.info("{}  skip   {}", String.format("%4d / %4d",  count, symbolSetSize), symbol);
						countSkip++;
					}
				} else {
					pause.sleep();
					if (updateProvider.updateFile(symbol, true)) {
						/*if (showOutput)*/ logger.info("{}  new    {}", String.format("%4d / %4d",  count, symbolSetSize), symbol);
						countNew++;
					} else {
//						/*if (showOutput)*/ logger.info("{}  none   {}", String.format("%4d / %4d",  count, symbolSetSize), symbol);
						countNone++;
					}
				}
			}
			logger.info("old    {}", String.format("%4d", countOld));
			if (countOld == 0) break; // Exit loop because there is no old file. 
			if (countOld != lastCountOld) {
				retryCount = 0; // reset retry count
			}
			needSleep = true;
			symbolSet = nextSymbolSet;
		}
		logger.info("===========");
		logger.info("update {}", String.format("%4d", countUpdate));
		logger.info("old    {}", String.format("%4d", countOld));
		logger.info("skip   {}", String.format("%4d", countSkip));
		logger.info("new    {}", String.format("%4d", countNew));
		logger.info("none   {}", String.format("%4d", countNone));
		logger.info("total  {}", String.format("%4d", countUpdate + countOld + countSkip + countNew + countNone));
		logger.info("total  {}", String.format("%4d", total));
	}
	
	// This methods update end of day csv in tmp/eod directory.
	public static void main(String[] args) {
		logger.info("START");
		
		logger.info("DATE_FIRST {}", UpdateProvider.DATE_FIRST);
		logger.info("DATE_LAST  {}", UpdateProvider.DATE_LAST);

		String providerName = args[0];
		UpdateProvider updateProvider = getProvider(providerName);
		logger.info("UpdateProvider {}", updateProvider.getName());
		
		int countUnknown    = 0;
//		int countNoDateLast = 0;
		
		{
			File dir = updateProvider.getFile("DUMMY").getParentFile();
			if (!dir.exists()) {
				logger.info("Create directory {}", dir.getPath());
				dir.mkdirs();
			} else {
				if (!dir.isDirectory()) {
					logger.info("Not directory {}", dir.getAbsolutePath());
					throw new SecuritiesException("Not directory");
				}
			}
			
			{
				File unknownDir = new File(PATH_UNKNOWN_DIR);
				if (!unknownDir.exists()) {
					logger.info("Create directory {}", unknownDir.getPath());
					unknownDir.mkdirs();
				} else {
					if (!unknownDir.isDirectory()) {
						logger.info("Not directory {}", unknownDir.getAbsolutePath());
						throw new SecuritiesException("Not directory");
					}
				}
			}
			
			
			// Remove unknown file
			{
				String destFileSuffix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
				List<File> fileList = Arrays.asList(dir.listFiles((d, name) -> (name.endsWith(".csv"))));
				fileList.sort((a, b) -> a.getName().compareTo(b.getName()));
				for(File file: fileList) {
					String name = file.getName();
					String symbol = name.replace(".csv", "");
					if (StockUtil.contains(symbol)) continue;
					
					countUnknown++;
					
					try {
						File destFile = new File(PATH_UNKNOWN_DIR, String.format("%s-%s", name, destFileSuffix));
						logger.info("{}  move unknown file {} to {}", String.format("%4d",  countUnknown), file.getPath(), destFile.getPath());
											
						// Copy file to new location
						Files.copy(file.toPath(), destFile.toPath());
						// Delete file after successful copy
						file.delete();
					} catch (IOException e) {
						logger.error("IOException {}", e.toString());
						throw new SecuritiesException("IOException");
					}
				}

			}
			
//			// Remove file that contains no DATE_LAST
//			{
//				String dateLast = UpdateProvider.DATE_LAST.toString();
//				List<File> fileList = Arrays.asList(dir.listFiles((d, name) -> (name.endsWith(".csv"))));
//				fileList.sort((a, b) -> a.getName().compareTo(b.getName()));
//				
//				for(File file: fileList) {
//					String name = file.getName();
//
//					boolean hasDateLast = false;
//					List<Price> priceList = Price.load(file);
//					for(Price price: priceList) {
//						if (price.date.equals(dateLast)) {
//							hasDateLast = true;
//							break;
//						}
//					}
//					if (hasDateLast) continue;
//					
//					countNoDateLast++;
//					logger.info("{} delete file contains no {}  {}", String.format("%4d",  countNoDateLast), dateLast, name);
//					file.delete();
//				}
//			}
		}
		
		{
			Map<String, Stock> stockMap = new TreeMap<>();
			StockUtil.getAll().stream().forEach(e -> stockMap.put(e.symbol, e));
			updateFile(updateProvider, stockMap);
		}
		
		logger.info("===========");
		logger.info("unknown    {}", String.format("%4d", countUnknown));
//		logger.info("noDateLast {}", String.format("%4d", countNoDateLast));
		
		logger.info("STOP");
	}
}
