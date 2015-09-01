package yokwe.finance.etf;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Scrape<E extends Enum<E>> {
	private static final Logger logger = LoggerFactory.getLogger(Scrape.class);
	public static final String NO_VALUE = "*NOVALUE*";
	
	protected static class Element<EE extends Enum<EE>> {
		protected final   EE      key;
		protected final   int     groupCount;
		protected final   String  expect;
		protected final   Matcher matcher;
		protected         boolean haveValue;
		
		protected Element(EE key, int groupCount, String pattern, String expect) {
			this.key        = key;
			this.groupCount = groupCount;
			this.expect     = expect;
			
			matcher         = Pattern.compile(pattern).matcher("");
			haveValue       = false;
		}
		
		protected void reset(String fileName, String contents) {
			haveValue = expect == null || contents.contains(expect);
			if (haveValue) {
				matcher.reset(contents);
				if (!matcher.find()) {
					logger.error("NAME {} {}", fileName, key);
					logger.error("pat {}", matcher.toString());
					throw new RuntimeException("NAME");
				}
				if (matcher.groupCount() != groupCount) {
					logger.error("GROUP_COUNT {} {}  groupCount = {}", fileName, key, matcher.groupCount());
					throw new RuntimeException("GROUP_COUNT");
				}
			}
		}
		protected String getValue(int group) {
			return haveValue ? matcher.group(group) : NO_VALUE;
		}
	}
	
	protected  TreeMap<Enum<E>, Element<E>> map = new TreeMap<>();
	
	protected void add(E e, int groupCount, String pattern) {
		add(e, groupCount, pattern, null);
	}
	protected void add(E e, int groupCount, String pattern, String expect) {
		if (map.containsKey(e)) {
			logger.error("DUPLICATE {}", e);
			throw new RuntimeException("DUPLICATE");
		}
		Element<E> element = new Element<>(e, groupCount, pattern, expect);
		map.put(e, element);
	}

	// invoke add in init
	abstract protected void init();
	
	public Scrape() {
		init();
	}
	public void reset(File file) {
		String fileName = file.getName();
		String contents = Util.getContents(file);

		for(Element<E> element: map.values()) {
			element.reset(fileName, contents);
		}
	}
	public String getValue(E e) {
		return getValue(e, 1);
	}
	public String getValue(E e, int group) {
		String ret = map.get(e).getValue(group);
		
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
			ret = String.format("%.0f", value * 100);
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
				throw new RuntimeException("UNIT " + unit);
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
				throw new RuntimeException("UNIT " + unit);
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
		

		
		if (ret.contains(";")) {
			logger.error("SEMI {} {}", e, ret);
			throw new RuntimeException("SEMI");
		}
		
		return ret;
	}

	private static final DateTimeFormatter parseInceptionDate  = DateTimeFormatter.ofPattern("MMM d, yyyy");
	private static final DateTimeFormatter formatInceptionDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");


}
