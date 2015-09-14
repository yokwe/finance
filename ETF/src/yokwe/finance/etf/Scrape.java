package yokwe.finance.etf;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Scrape<E extends Enum<E>> {
	private static final Logger logger = LoggerFactory.getLogger(Scrape.class);
	private static final String NO_VALUE = "*NA*";
	
	public static boolean isValid(String value) {
		return value.compareTo(NO_VALUE) != 0;
	}
	
	protected static class Element<EE extends Enum<EE>> {
		protected final   EE      key;
		protected final   String  expect;
		protected final   Matcher matcher;
		
		protected Element(EE key, String pattern, String expect) {
			this.key        = key;
			this.expect     = expect;
			
			matcher         = Pattern.compile(pattern).matcher("");
		}
		
		protected String read(String fileName, String contents) {
			boolean haveValue = expect == null || contents.contains(expect);
			if (haveValue) {
				matcher.reset(contents);
				if (!matcher.find()) {
					logger.error("NAME {} {}", fileName, key);
					logger.error("pat {}", matcher.toString());
					throw new ETFException("NAME");
				}
				if (matcher.groupCount() != 1) {
					logger.error("GROUP_COUNT {} {}  groupCount = {}", fileName, key, matcher.groupCount());
					throw new ETFException("GROUP_COUNT");
				}
				return matcher.group(1);
			} else {
				return NO_VALUE;
			}
		}
	}
	
	protected  TreeMap<E, Element<E>> map = new TreeMap<>();
	
	protected void add(E e, String pattern) {
		add(e, pattern, null);
	}
	protected void add(E e, String pattern, String expect) {
		if (map.containsKey(e)) {
			logger.error("DUPLICATE {}", e);
			throw new ETFException("DUPLICATE");
		}
		Element<E> element = new Element<>(e, pattern, expect);
		map.put(e, element);
	}

	// invoke add in init
	abstract protected void init();
	
	public Scrape() {
		init();
	}
	public Map<E, String> readFile(File file) {
		Map<E, String> ret = new TreeMap<>();
		
		String fileName = file.getName();
		String contents = Util.getContents(file);
		
		int count = 0;
		for(E key: map.keySet()) {
			String value = map.get(key).read(fileName, contents);
			value = normalize(value);
			if (!value.equals(NO_VALUE)) count++;
			ret.put(key, value);
		}
		
		return (count == 0) ? null : ret;
	}
	
	public List<Map<E, String>> readDirectory(String path) {
		File root = new File(path);
		if (!root.isDirectory()) {
			logger.error("Not directory  path = {}", path);
			throw new ETFException("not directory");
		}
		
		File[] fileList = root.listFiles();
		Arrays.sort(fileList, (a, b) -> a.getName().compareTo(b.getName()));
		
		List<Map<E, String>> ret = new ArrayList<>();
		for(File file: fileList) {
			if (file.length() == 0) continue;
			
			try {
				Map<E, String> values = readFile(file);
				if (values == null) continue;
				
				ret.add(values);
			} catch (ETFException e) {
				logger.error(e.toString());
			}
		}
		
		logger.info("file = {}", fileList.length);
		logger.info("list = {}", ret.size());
		
		return ret;
	}
		
	private static final DateTimeFormatter parseInceptionDate  = DateTimeFormatter.ofPattern("MMM d, yyyy");
	private static final DateTimeFormatter formatInceptionDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	protected String normalize(String string) {
		String ret = string;
		
		ret = ret.replace("&amp;", "&");
		ret = ret.replace("&gt;",  ">");
		ret = ret.replace("&rsquo;", "'");
		
		ret = ret.replaceAll("<[^>]+>", " ");
		ret = ret.replaceAll("\\p{javaWhitespace}+", " ");
		ret = ret.trim();
		
		// mm/dd/yy
		if (ret.matches("^([0-9]{2})/([0-9]{2})/([0-9]{2})$")) {
			String mm = ret.substring(0, 2);
			String dd = ret.substring(3, 5);
			String yy = ret.substring(6, 8);
			ret = String.format("20%s-%s-%s", yy, mm, dd);
		}
		
		// 9.99%
		if (ret.matches("^[0-9]+\\.[0-9]+%$")) {
			String str = ret.substring(0, ret.length() - 1);
			Float value = Float.valueOf(str);
			ret = String.format("%.2f", value);
		}
		
		// $576.4 K
		// $28.29 M
		// $31.95 B
		if (ret.matches("^\\$([0-9]+\\.[0-9]+) [KBM]$")) {
			String suffix = "";
			char unit = ret.charAt(ret.length() - 1);
			switch (unit) {
			case 'K':
				suffix = "";
				break;
			case 'M':
				suffix = "000";
				break;
			case 'B':
				suffix = "000000";
				break;
			default:
				logger.error("ret {}!", ret);
				throw new ETFException("UNIT " + unit);
			}
			String str = ret.substring(1, ret.length() - 2);
			Float value = Float.valueOf(str);
			String fract = String.format("%.0f", value * 1000.0);
			ret = fract + suffix;
		}
		
		// 998.14K
		// 59.31M
		// 144.98B
		if (ret.matches("^([0-9]+\\.[0-9]+)[KBM]$")) {
			String suffix = "";
			char unit = ret.charAt(ret.length() - 1);
			switch (unit) {
			case 'K':
				suffix = "";
				break;
			case 'M':
				suffix = "000";
				break;
			case 'B':
				suffix = "000000";
				break;
			default:
				logger.error("ret {}!", ret);
				throw new ETFException("UNIT " + unit);
			}
			String str = ret.substring(0, ret.length() - 1);
			Float value = Float.valueOf(str);
			String fract = String.format("%.0f", value * 1000.0);
			ret = fract + suffix;
		}
		
		// Aug 19, 2015
		if (ret.matches("^([A-Za-z]{3}) ([0-9]{1,2}), ([0-9]{4})$")) {
			ret = formatInceptionDate.format(parseInceptionDate.parse(ret));
		}
		
		// --
		if (ret.compareTo("--") == 0) {
			ret = NO_VALUE;
		}
		
		// --
		if (ret.compareTo("NaN") == 0) {
			ret = NO_VALUE;
		}
		
		// N/A
		if (ret.compareTo("N/A") == 0) {
			ret = NO_VALUE;
		}
		
		// Sanity check
		if (ret.contains(";")) {
			logger.error("SEMI {}", ret);
			throw new ETFException("SEMI");
		}
		
		if (ret.contains("\"")) {
			logger.error("DOUBLE_QUOTE {}", ret);
			throw new ETFException("DOUBLE_QUOTE");
		}
		
		return ret;
	}
}
