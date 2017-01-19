package yokwe.finance.securities.eod;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.database.NasdaqTable;
import yokwe.finance.securities.util.CSVUtil;
import yokwe.finance.securities.util.FileUtil;
import yokwe.finance.securities.util.HttpUtil;
import yokwe.finance.securities.util.NasdaqUtil;

public class Update {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Update.class);
	
	private static final String PATH_DIR = "tmp/eod/price";
	
	private static final DateTimeFormatter DATE_FORMAT_URL    = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);
	private static final DateTimeFormatter DATE_FORMAT_PARSE  = DateTimeFormatter.ofPattern("d-MMM-yy");
	private static final DateTimeFormatter DATE_FORMAT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private static final LocalDate DATE_LAST;
	private static final LocalDate DATE_FIRST;
	static {
		LocalDateTime today = LocalDateTime.now(ZoneId.of("America/New_York"));
		if (today.getHour() < 16) today = today.minusDays(1); // Move to yesterday if it is before market close
		
		// Adjust for weekends
		DayOfWeek dayOfWeek = today.getDayOfWeek();
		if (dayOfWeek == DayOfWeek.SUNDAY)   today = today.minusDays(-2); // Move to previous Friday
		if (dayOfWeek == DayOfWeek.SATURDAY) today = today.minusDays(-1); // Move to previous Friday
		
		DATE_LAST  = today.toLocalDate();
		DATE_FIRST = DATE_LAST.plusYears(-1);
		logger.info("DATE {} - {}", DATE_FIRST, DATE_LAST);
	}
	
	private static File getFile(String symbol) {
		String path = String.format("%s/%s.csv", PATH_DIR, symbol);
		File file = new File(path);
		return file;
	}

	private static boolean needUpdate(File file, LocalDate dateFirst, LocalDate dateLast) {
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
		// second line should be last data
		String line = lines[1];
		String[] values = line.split(",");
		if (values.length != 7) {
			logger.error("Unexpected line  {}", line);
			throw new SecuritiesException("Unexpected header");
		}
		String date = values[0];
		
		return !date.equals(dateLast.toString());
	}
	
	private static void updateFile(String exch, String symbol, LocalDate dateFirst, LocalDate dateLast) {
		File file = getFile(symbol);
		
		String dateFrom = dateFirst.format(DATE_FORMAT_URL).replace(" ", "%20");
		String dateTo   = dateLast.format(DATE_FORMAT_URL).replace(" ", "%20");
		
		NasdaqTable nasdaq = NasdaqUtil.get(symbol.replace(".PR.", "-"));

		// Convert from '.PR.' to  '-' in symbol for google
		String url = String.format("https://www.google.com/finance/historical?q=%s:%s&startdate=%s&enddate=%s&output=csv", nasdaq.exchange, nasdaq.google, dateFrom, dateTo);

		String content = HttpUtil.downloadAsString(url);
		if (content == null) {
			// cannot get content
			file.delete();
			return;
		}
		
		String[] lines = content.split("\n");
//		logger.info("lines {}", lines.length);
		if (lines.length <= 1) {
			// only header
			file.delete();
			return;
		}
		
		// Sanity check
		String GOOGLE_PRICE_HEADER = "\uFEFFDate,Open,High,Low,Close,Volume";
		String header = lines[0];
		if (!header.equals(GOOGLE_PRICE_HEADER)) {
			logger.error("Unexpected header  {}", header);
			throw new SecuritiesException("Unexpected header");
		}

		List<Price> priceList = new ArrayList<>();
		
		for(String line: lines) {
			if (line.startsWith("\uFEFFDate,Open,High,Low,Close,Volume")) continue;
			
			String[] values = line.split(",");
			if (values.length != 6) {
				logger.error("Unexpected line  {}", line);
				throw new SecuritiesException("Unexpected header");
			}
			
			// Fix format of date  02-Jan-14 => 2014-01-02
			// Fix year of date    02-Jan-80 => 1980-01-02
			{
				LocalDate localDate = LocalDate.from(DATE_FORMAT_PARSE.parse(values[0]));
				if (DATE_LAST.getYear() < localDate.getYear()) localDate = localDate.minusYears(100);
				values[0] = DATE_FORMAT_FORMAT.format(localDate);
			}
			
			// Special when field (open, high and low) contains dash
			if (values[1].equals("-")) {
				values[1] = values[4];
			}
			if (values[2].equals("-")) {
				values[2] = values[4];
			}
			if (values[3].equals("-")) {
				values[3] = values[4];
			}
			
			// Special when field (volume) contains dash
			if (values[5].equals("-")) {
				values[5] = "0";
			}

			String date   = values[0];
			double open   = Double.valueOf(values[1]);
			double high   = Double.valueOf(values[2]);
			double low    = Double.valueOf(values[3]);
			double close  = Double.valueOf(values[4]);
			long   volume = Long.valueOf(values[5]);
			
			priceList.add(new Price(date, symbol, open, high, low, close, volume));
		}
		CSVUtil.saveWithHeader(priceList, file.getAbsolutePath());
	}
	
	// This methods update end of day csv in tmp/eod directory.
	public static void main(String[] args) {
		logger.info("START");
		
		{
			File dir = new File(PATH_DIR);
			if (!dir.exists()) {
				dir.mkdirs();
			} else {
				if (!dir.isDirectory()) {
					logger.info("Not directory {}", dir.getAbsolutePath());
					throw new SecuritiesException("Not directory");
				}
			}
			
			// Remove unknown file
			File[] fileList = dir.listFiles();
			for(File file: fileList) {
				String name = file.getName();
				if (name.endsWith(".csv")) {
					String symbol = name.replace(".csv", "");
					if (NasdaqUtil.contains(symbol)) continue;
				}
				
				logger.info("delete unknown file {}", name);
				file.delete();
			}
		}
		
		{
			Collection<NasdaqTable> nasdaqCollection = NasdaqUtil.getAll();
			
			int total = nasdaqCollection.size();
			int count = 0;
			
			int showInterval = 100;
			boolean showOutput;
			int lastOutputCount = -1;
			for(NasdaqTable nasdaq: nasdaqCollection) {
				String exch   = nasdaq.exchange;
				String symbol = nasdaq.symbol;

				int outputCount = count / showInterval;
				if (outputCount != lastOutputCount) {
					showOutput = true;
					lastOutputCount = outputCount;
				} else {
					showOutput = false;
				}

				count++;
				
				File file = getFile(symbol);
				if (file.exists()) {
					if (needUpdate(file, DATE_FIRST, DATE_LAST)) {
						if (showOutput) logger.info("{}  update {}", String.format("%4d / %4d",  count, total), symbol);
						updateFile(exch, symbol, DATE_FIRST, DATE_LAST);
					} else {
						if (showOutput) logger.info("{}  skip   {}", String.format("%4d / %4d",  count, total), symbol);
					}
				} else {
					if (showOutput) logger.info("{}  new    {}", String.format("%4d / %4d",  count, total), symbol);
					updateFile(exch, symbol, DATE_FIRST, DATE_LAST);
				}
			}
		}
		logger.info("STOP");
	}

}
