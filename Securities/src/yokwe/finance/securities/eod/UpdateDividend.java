package yokwe.finance.securities.eod;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.database.NasdaqTable;
import yokwe.finance.securities.util.CSVUtil;
import yokwe.finance.securities.util.FileUtil;
import yokwe.finance.securities.util.HttpUtil;
import yokwe.finance.securities.util.NasdaqUtil;

public class UpdateDividend {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateDividend.class);
	private static final String PATH_DIR = "tmp/eod/dividend";
	
	private static final int HOUR_CLOSE_MARKET = 16; // market close at 1600
	private static final int DURTION_YEAR      =  1; // we need one year data
	
	private static final LocalDate DATE_LAST;
	private static final LocalDate DATE_FIRST;
	static {
		LocalDateTime today = LocalDateTime.now(ZoneId.of("America/New_York"));
		if (today.getHour() < HOUR_CLOSE_MARKET) today = today.minusDays(1); // Move to yesterday if it is before market close
		
		// Adjust for weekends
		DayOfWeek dayOfWeek = today.getDayOfWeek();
		if (dayOfWeek == DayOfWeek.SUNDAY)   today = today.minusDays(-2); // Move to previous Friday
		if (dayOfWeek == DayOfWeek.SATURDAY) today = today.minusDays(-1); // Move to previous Friday
		
		DATE_LAST  = today.toLocalDate();
		DATE_FIRST = DATE_LAST.plusYears(-DURTION_YEAR);
		logger.info("DATE {} - {}", DATE_FIRST, DATE_LAST);
	}
	
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	
	public interface UpdateFile {
		public void updateFile(String exch, String symbol, LocalDate dateFirst, LocalDate dateLast);
	}
	
	public static class UpdateFileYahoo implements UpdateFile {
		public void updateFile(String exch, String symbol, LocalDate dateFirst, LocalDate dateLast) {

			File file = getFile(symbol);
			
			NasdaqTable nasdaq = NasdaqUtil.get(symbol.replace(".PR.", "-"));

			// first
			int a = dateFirst.getMonthValue(); // mm
			int b = dateFirst.getDayOfMonth(); // dd
			int c = dateFirst.getYear();       // yyyy
			// last
			int d = dateLast.getMonthValue(); // mm
			int e = dateLast.getDayOfMonth(); // dd
			int f = dateLast.getYear();       // yyyy
			String url = String.format("http://real-chart.finance.yahoo.com/table.csv?s=%s&a=%02d&b=%02d&c=%04d&d=%02d&e=%02d&f=%04d&g=v&ignore=.csv", nasdaq.yahoo, a - 1, b, c, d - 1, e, f);
			String content = HttpUtil.downloadAsString(url);
			if (content == null) {
				// cannot get content
				file.delete();
				return;
			}
			
			String[] lines = content.split("\n");
//			logger.info("lines {}", lines.length);
			if (lines.length <= 1) {
				// only header
				file.delete();
				return;
			}
			
			// Sanity check
			String YAHOO_DIVIDEND_HEADER = "Date,Dividends";
			String header = lines[0];
			if (!header.equals(YAHOO_DIVIDEND_HEADER)) {
				logger.error("Unexpected header  {}", header);
				throw new SecuritiesException("Unexpected header");
			}

			List<Dividend> dividendList = new ArrayList<>();
			
			for(String line: lines) {
				if (line.startsWith(YAHOO_DIVIDEND_HEADER)) continue;
				
				String[] values = line.split(",");
				if (values.length != 2) {
					logger.error("Unexpected line  {}", line);
					throw new SecuritiesException("Unexpected header");
				}
				
				String date     = values[0];
				double dividend = round(Double.valueOf(values[1]), 4);
				
				dividendList.add(new Dividend(date, symbol, dividend));
			}
			CSVUtil.saveWithHeader(dividendList, file.getAbsolutePath());
		}
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
		String HEADER = "date,symbol,dividend";
		String header = lines[0];
		if (!header.equals(HEADER)) {
			logger.error("Unexpected header  {}", header);
			throw new SecuritiesException("Unexpected header");
		}
		// second line should be last data
		String line = lines[1];
		String[] values = line.split(",");
		if (values.length != 3) {
			logger.error("Unexpected line  {}", line);
			throw new SecuritiesException("Unexpected header");
		}
		String date = values[0];
		
		return !date.equals(dateLast.toString());
	}
	
	
	// This methods update end of day csv in tmp/eod directory.
	public static void main(String[] args) {
		logger.info("START");
		
//		UpdateFile updateFile = new UpdateFileGoogle();
		UpdateFile updateFile = new UpdateFileYahoo();
		
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
//			Collection<NasdaqTable> nasdaqCollection = new ArrayList<>();
//			nasdaqCollection.add(NasdaqUtil.get("IBM"));
//			nasdaqCollection.add(NasdaqUtil.get("NYT"));
//			nasdaqCollection.add(NasdaqUtil.get("PEP"));
			
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
						updateFile.updateFile(exch, symbol, DATE_FIRST, DATE_LAST);
					} else {
						if (showOutput) logger.info("{}  skip   {}", String.format("%4d / %4d",  count, total), symbol);
					}
				} else {
					if (showOutput) logger.info("{}  new    {}", String.format("%4d / %4d",  count, total), symbol);
					updateFile.updateFile(exch, symbol, DATE_FIRST, DATE_LAST);
				}
			}
		}
		logger.info("STOP");
	}}
