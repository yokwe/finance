package yokwe.finance.securities.eod;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.FileUtil;

public class UpdatePreferred {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdatePreferred.class);
	
	public static final String PATH_DIR  = "tmp/fetch/quantum";
	public static final String PATH_FILE = "tmp/eod/preferred.csv";
	
	static class Match {
		final Matcher matcher;
		Match(String pattern, int flag) {
			matcher = Pattern.compile(pattern, flag).matcher("");
		}
		Match(String pattern) {
			this(pattern, 0);
		}
		
		String getValue(String content) {
			boolean found = matcher.reset(content).find();
			if (!found) {
				logger.error("No match {}", matcher);
				throw new SecuritiesException("No match");
			}
			String ret = matcher.group(1);
			
			ret = ret.replaceAll("&nbsp;", " ");
			ret = ret.replaceAll("<.+?>", " ");
			ret = ret.replaceAll("\\p{javaWhitespace}+", " ");
			ret = ret.trim();
			
//			// mm/dd/yyyy
//			if (ret.matches("^([0-9]{1,2})/([0-9]{1,2})/([0-9]{4})$")) {
//				String mdy[] = ret.split("\\/");
//				if (mdy[0].length() == 1) mdy[0] = "0" + mdy[0];
//				if (mdy[1].length() == 1) mdy[1] = "0" + mdy[1];
//				ret = String.format("%s-%s-%s", mdy[2], mdy[0], mdy[1]);
//			}
//			
//			// mm/dd/yyyy?
//			if (ret.matches("^([0-9]{1,2})/([0-9]{1,2})/([0-9]{2,4})\\?$")) {
//				ret = ret.replace("?", "");
//				String mdy[] = ret.split("\\/");
//				if (mdy[0].length() == 1) mdy[0] = "0" + mdy[0];
//				if (mdy[1].length() == 1) mdy[1] = "0" + mdy[1];
//				if (mdy[2].length() == 2) mdy[2] = "20" + mdy[2];
//				ret = String.format("%s?%s?%s", mdy[2], mdy[0], mdy[1]);
//			}
//			
//			if (ret.matches("^\\$[0-9,\\.]+$")) {
//				// $1.40625
//				if (ret.matches("^\\$[0-5](\\.[0-9]+)?$")) {
//					ret = String.format("%.5f", Double.valueOf(ret.substring(1)));
//				} else {
//					ret = String.format("%.2f", Double.valueOf(ret.replace(",", "").substring(1)));
//				}
//			}
//
//			if (ret.equals("n.a."))         ret = "*NA*";
//			if (ret.equals("Reset Rate"))   ret = "RESET";
//			if (ret.equals("Reset rate"))   ret = "RESET";
//			if (ret.equals("Floating"))     ret = "FLOAT";
//			if (ret.equals("Fixed/Adj"))    ret = "FIXED_ADJ";
//			if (ret.equals("Adj Rate"))     ret = "ADJ";
//			if (ret.equals("Var Rate"))     ret = "VAR";
//			if (ret.equals("None"))         ret = "NONE";
//			if (ret.equals("Tender Offer")) ret = "TENDER";
//			if (ret.equals("anytime"))      ret = "ANYTIME";
//			if (ret.equals("any time"))     ret = "ANYTIME";
//			if (ret.equals("Any time"))     ret = "ANYTIME";
//			if (ret.equals("Any Time"))     ret = "ANYTIME";
//			if (ret.equals("Partial Call")) ret = "PARTIAL";
//			if (ret.equals("Called for"))   ret = "CALLED";
//			if (ret.equals("noncallable"))  ret = "NO_REDEEM";
//			if (ret.equals("Noncallable"))  ret = "NO_REDEEM";
//			if (ret.equals("Nonredeem"))    ret = "NO_REDEEM";
//			if (ret.equals("Unredeemable")) ret = "NO_REDEEM";

			return ret;
		}
	}
	
	private static final Match SYMBOL   = new Match(">Ticker Symbol: ([^ ]+) ");
	private static final Match TYPE     = new Match("Security Type:.+?>(.+?)<", Pattern.DOTALL);
	
	private static final Match PARENT   = new Match("<b>Goto Parent Company's Record \\((.+?)\\)</b>");
	private static final Match ADDRESS  = new Match("<b>Address:</b>(.+?)</font>", Pattern.DOTALL);

	private static final Match NAME     = new Match("<font size=\"\\+1\"><center><b>(.+?)<", Pattern.DOTALL);

	private static final Pattern TABLE_HEADER7 = Pattern.compile("<tr bgcolor=\"FFEFB5\">" +
			"\\p{javaWhitespace}+<th>(.+?)</th>" +
			"\\p{javaWhitespace}+<th>(.+?)</th>" +
			"\\p{javaWhitespace}+<th>(.+?)</th>" +
			"\\p{javaWhitespace}+<th>(.+?)</th>" +
			"\\p{javaWhitespace}+<th>(.+?)</th>" +
			"\\p{javaWhitespace}+<th>(.+?)</th>" +
			"\\p{javaWhitespace}+<th>(.+?)</th>" +
			"\\p{javaWhitespace}+</tr>", Pattern.DOTALL);
	private static final Pattern TABLE_DATA7 = Pattern.compile("<tr bgcolor=\"FFEFB5\">.+?<tr>" +
			"\\p{javaWhitespace}+<td .+?>(.+?)" + // 1
			"\\p{javaWhitespace}+<td .+?>(.+?)" + // 2
			"\\p{javaWhitespace}+<td .+?>(.+?)" + // 3
			"\\p{javaWhitespace}+<td .+?>(.+?)" + // 4
			"\\p{javaWhitespace}+<td .+?>(.+?)" + // 5
			"\\p{javaWhitespace}+<td .+?>(.+?)" + // 6
			"\\p{javaWhitespace}+<td .+?>(.+?)" + // 7
			"\\p{javaWhitespace}+</tr>", Pattern.DOTALL);

	private static final Pattern TABLE_HEADER8 = Pattern.compile("<tr bgcolor=\"FFEFB5\">" +
			"\\p{javaWhitespace}+<th>(.+?)</th>" +
			"\\p{javaWhitespace}+<th>(.+?)</th>" +
			"\\p{javaWhitespace}+<th>(.+?)</th>" +
			"\\p{javaWhitespace}+<th>(.+?)</th>" +
			"\\p{javaWhitespace}+<th>(.+?)</th>" +
			"\\p{javaWhitespace}+<th>(.+?)</th>" +
			"\\p{javaWhitespace}+<th>(.+?)</th>" +
			"\\p{javaWhitespace}+<th>(.+?)</th>" +
			"\\p{javaWhitespace}+</tr>", Pattern.DOTALL);
	private static final Pattern TABLE_DATA8 = Pattern.compile("<tr bgcolor=\"FFEFB5\">.+?<tr>" +
			"\\p{javaWhitespace}+<td .+?>(.+?)" + // 1
			"\\p{javaWhitespace}+<td .+?>(.+?)" + // 2
			"\\p{javaWhitespace}+<td .+?>(.+?)" + // 3
			"\\p{javaWhitespace}+<td .+?>(.+?)" + // 4
			"\\p{javaWhitespace}+<td .+?>(.+?)" + // 5
			"\\p{javaWhitespace}+<td .+?>(.+?)" + // 6
			"\\p{javaWhitespace}+<td .+?>(.+?)" + // 7
			"\\p{javaWhitespace}+<td .+?>(.+?)" + // 8
			"\\p{javaWhitespace}+</tr>", Pattern.DOTALL);


	private static final Pattern USA_ZIPCODE = Pattern.compile("[A-Za-z]{2} [0-9]{5}(-[0-9]{4})?$");

	private static String[][] addressContryArray = {
			{"BERMUDA",     "Bermuda"},
			
			{"BRAZIL",      "Brasil"},
			
			{"CANADA",      "Canada"},
			
			{"CAYMAN",      "Cayman Islands"},

			{"GERMANY",     "Germany"},
			
			{"GREECE",      "Greece"},
			
			{"HONG KONG",   "Hong Kong"},
			
			{"IRELAND",     "Ireland"},
			
			{"MONACO",      "Monaco"},
			
			{"NETHERLANDS", "Netherlands"},
			
			{"PUERTO RICO", "Puerto Rico"},

			{"SPAIN",       "Edificio Pereda"},

			{"UK",          "United Kingdom"},
			{"UK",          "England"},
			
			{"USA",         "Philadelphia"},
			{"USA",         "New York"},
	};
	
	private static String toCountry(String address) {
		if (USA_ZIPCODE.matcher(address).find()) return "USA";
		
		for(String[] strings: addressContryArray) {
			String country = strings[0];
			String string  = strings[1];
			
			if (address.contains(string)) return country;
		}
		
		logger.error("address = {}", address);
		throw new SecuritiesException("Unexpected");
	}
	
	private static String normalize(String html) {
		String tokenList[] = html.split("<br>", -1);
		StringBuffer ret = new StringBuffer();
		
		for(int i = 0; i < tokenList.length; i++) {
			String token = tokenList[i];
			token = token.replaceAll("<.+?>", "");
			token = token.replaceAll("&nbsp;", " ");
			token = token.replaceAll("\\p{javaWhitespace}+", " ");
			token = token.trim();
			
			// mm/dd/yyyy
			if (token.matches("^([0-9]{1,2})/([0-9]{1,2})/([0-9]{4})$")) {
				String mdy[] = token.split("\\/");
				if (mdy[0].length() == 1) mdy[0] = "0" + mdy[0];
				if (mdy[1].length() == 1) mdy[1] = "0" + mdy[1];
				token = String.format("%s-%s-%s", mdy[2], mdy[0], mdy[1]);
			}

			// mm/dd/yy
			if (token.matches("^([0-9]{1,2})/([0-9]{1,2})/([0-9]{2})$")) {
				String mdy[] = token.split("\\/");
				if (mdy[0].length() == 1) mdy[0] = "0" + mdy[0];
				if (mdy[1].length() == 1) mdy[1] = "0" + mdy[1];
				token = String.format("20%s-%s-%s", mdy[2], mdy[0], mdy[1]);
			}
			
			// $[0-5].[0-9]+
			if (token.matches("^\\$[0-9]{1,2}(\\.[0-9]+)?$")) {
				token = String.format("%.5f", Double.valueOf(token.substring(1)));
			}
			// $.[0-9]+
			if (token.matches("^\\$(\\.[0-9]+)?$")) {
				token = String.format("%.2f", Double.valueOf("0" + token.substring(1)));
			}
			
			// $[0-9,]{2,5}.00
			if (token.matches("^\\$([0-9,]{2,5}\\.[0-9]{2})?$")) {
				token = String.format("%.2f", Double.valueOf(token.substring(1).replace(",", "")));
			}
			
			// 9.99%
			if (token.matches("^[0-9]+\\.[0-9]+%$")) {
				token = String.format("%.5f", Double.valueOf(token.substring(0, token.length() - 1)) * 0.01);
			}


			if (0 < token.length()) {
				ret.append("=").append(token);
			}
		}
		
		return ret.substring(1);
	}
	
	private static void update() {
		File dir = new File(PATH_DIR);
		if (!dir.isDirectory()) {
			logger.error("Not directory  path = {}", PATH_DIR);
			throw new SecuritiesException("not directory");
		}
		
		File[] fileArray = dir.listFiles();
		Arrays.sort(fileArray, (a, b) -> a.getName().compareTo(b.getName()));
		
		List<Preferred> preferredList = new ArrayList<>();
		
		int count = 0;
		for(File file: fileArray) {
			count++;
			
			if (file.length() == 0) {
				logger.warn("Skip empty file {}", file.getName());
				continue;
			}
			
			String content = FileUtil.read(file);
			if (content.contains("Not Found!")) {
				continue;
			}
			
			if (!content.contains("Distribution Dates")) {
				continue;
			}

			String symbol    = SYMBOL.getValue(content);

			String type      = content.contains("Security Type:") ? TYPE.getValue(content) : "Common";
			
			String parent    = content.contains("Goto Parent Company") ? PARENT.getValue(content) : "*NA*";
			String address   = content.contains("<b>Address:</b>") ? ADDRESS.getValue(content) : "*NA*";
			
			String country = toCountry(address);

			logger.debug("{}", String.format("%4d %-8s %-6s %-12s %s", count, symbol, parent, country, type));

			String name      = NAME.getValue(content);
			
			final String cpnRate;
			final String annAmt;
			final String liqPref;
			final String callPrice;
			final String remark;
			final String callDate;
			final String maturDate;

			if (TABLE_HEADER8.matcher(content).find()) {
				{
					Matcher matcher = TABLE_HEADER8.matcher(content);
					if (!matcher.find()) {
						logger.error("Unexpected");
						throw new SecuritiesException("Unexpected");
					}
					String t1 = normalize(matcher.group(1));
					String t2 = normalize(matcher.group(2));
					String t3 = normalize(matcher.group(3));
					String t4 = normalize(matcher.group(4));
					String t5 = normalize(matcher.group(5));
					String t6 = normalize(matcher.group(6));
					String t7 = normalize(matcher.group(7));
					String t8 = normalize(matcher.group(8));
					
					if (!t1.equals("Stock=Exchange")) {
						logger.error("Unexpected t1 = {}!", t1);
						throw new SecuritiesException("Unexpected");
					}
					if (!t2.equals("Cpn Rate=Ann Amt")) {
						logger.error("Unexpected t2 = {}!", t2);
						throw new SecuritiesException("Unexpected");
					}
					if (!t3.equals("LiqPref=CallPrice")) {
						logger.error("Unexpected t3 = {}!", t3);
						throw new SecuritiesException("Unexpected");
					}
					if (!t4.equals("Call Date=Matur Date")) {
						logger.error("Unexpected t4 = {}!", t4);
						throw new SecuritiesException("Unexpected");
					}
					if (!t5.equals("Moodys/S&P=Dated")) {
						logger.error("Unexpected t5 = {}!", t5);
						throw new SecuritiesException("Unexpected");
					}
					if (!t6.equals("Conversion=Shares@Price") && !t6.equals("Conv Shrs=Conv Price")) {
						logger.error("Unexpected t6 = {}!", t6);
						throw new SecuritiesException("Unexpected");
					}
					if (!t7.equals("Distribution Dates")) {
						logger.error("Unexpected t7 = {}!", t7);
						throw new SecuritiesException("Unexpected");
					}
					if (!t8.equals("15%=Tax Rate")) {
						logger.error("Unexpected t8 = {}!", t8);
						throw new SecuritiesException("Unexpected");
					}
				}
				{
					Matcher matcher = TABLE_DATA8.matcher(content);
					if (!matcher.find()) {
						logger.error("Unexpected");
						throw new SecuritiesException("Unexpected");
					}

//					String[] t1 = normalize(matcher.group(1)).split("=");
					String[] t2 = normalize(matcher.group(2)).split("=");
					String[] t3 = normalize(matcher.group(3)).split("=");
					String[] t4 = normalize(matcher.group(4)).split("=");
//					String[] t5 = normalize(matcher.group(5)).split("=");
//					String[] t6 = normalize(matcher.group(6)).split("=");
//					String[] t7 = normalize(matcher.group(7)).split("=");
//					String[] t8 = normalize(matcher.group(8)).split("=");
					
					if (t2.length == 2) {
						cpnRate = t2[0];
						annAmt  = t2[1];
					} else {
						logger.error("Unexpected");
						throw new SecuritiesException("Unexpected");
					}
					if (t3.length == 2) {
						liqPref   = t3[0];
						callPrice = t3[1];
					} else {
						logger.error("Unexpected");
						throw new SecuritiesException("Unexpected");
					}
					if (t4.length == 2) {
						remark    = "";
						callDate  = t4[0];
						maturDate = t4[1];
					} else if (t4.length == 3) {
						if (t4[0].equals("Called for")) {
							remark    = "CALLED";
							callDate  = t4[1];
							maturDate = t4[2];
						} else if (t4[0].equals("Partial Call")) {
							remark    = "PARTIAL CALL";
							callDate  = t4[1];
							maturDate = t4[2];
						} else {
							logger.error("Unexpected t4[0] = {}", t4[0]);
							throw new SecuritiesException("Unexpected");
						}
					} else {
						logger.error("Unexpected");
						throw new SecuritiesException("Unexpected");
					}
				}
			} else {
				{
					Matcher matcher = TABLE_HEADER7.matcher(content);
					if (!matcher.find()) {
						logger.error("Unexpected");
						throw new SecuritiesException("Unexpected");
					}

					String t1 = normalize(matcher.group(1));
					String t2 = normalize(matcher.group(2));
					String t3 = normalize(matcher.group(3));
					String t4 = normalize(matcher.group(4));
					String t5 = normalize(matcher.group(5));
					String t6 = normalize(matcher.group(6));
					String t7 = normalize(matcher.group(7));
					
					if (!t1.equals("Stock=Exchange")) {
						logger.error("Unexpected t1 = {}!", t1);
						throw new SecuritiesException("Unexpected");
					}
					if (!t2.equals("Cpn Rate=Ann Amt")) {
						logger.error("Unexpected t2 = {}!", t2);
						throw new SecuritiesException("Unexpected");
					}
					if (!t3.equals("LiqPref=CallPrice")) {
						logger.error("Unexpected t3 = {}!", t3);
						throw new SecuritiesException("Unexpected");
					}
					if (!t4.equals("Call Date=Matur Date")) {
						logger.error("Unexpected t4 = {}!", t4);
						throw new SecuritiesException("Unexpected");
					}
					if (!t5.equals("Moodys/S&P=Dated")) {
						logger.error("Unexpected t5 = {}!", t5);
						throw new SecuritiesException("Unexpected");
					}
					if (!t6.equals("Distribution Dates")) {
						logger.error("Unexpected t6 = {}!", t6);
						throw new SecuritiesException("Unexpected");
					}
					if (!t7.equals("15%=Tax Rate")) {
						logger.error("Unexpected t7 = {}!", t7);
						throw new SecuritiesException("Unexpected");
					}
				}
				{
					Matcher matcher = TABLE_DATA7.matcher(content);
					if (!matcher.find()) {
						logger.error("Unexpected");
						throw new SecuritiesException("Unexpected");
					}

//					String[] t1 = normalize(matcher.group(1)).split("=");
					String[] t2 = normalize(matcher.group(2)).split("=");
					String[] t3 = normalize(matcher.group(3)).split("=");
					String[] t4 = normalize(matcher.group(4)).split("=");
//					String[] t5 = normalize(matcher.group(5)).split("=");
//					String[] t6 = normalize(matcher.group(6)).split("=");
//					String[] t7 = normalize(matcher.group(7)).split("=");
					
					if (t2.length == 2) {
						cpnRate = t2[0];
						annAmt  = t2[1];
					} else {
						logger.error("Unexpected");
						throw new SecuritiesException("Unexpected");
					}
					if (t3.length == 2) {
						liqPref   = t3[0];
						callPrice = t3[1];
					} else {
						logger.error("Unexpected");
						throw new SecuritiesException("Unexpected");
					}
					if (t4.length == 2) {
						callDate  = t4[0];
						maturDate = t4[1];
						remark = "";
					} else if (t4.length == 3) {
						if (t4[0].equals("Called for")) {
							remark    = "CALLED";
							callDate  = t4[1];
							maturDate = t4[2];
						} else if (t4[0].equals("Partial Call")) {
							remark    = "PARTIAL CALL";
							callDate  = t4[1];
							maturDate = t4[2];
						} else {
							logger.error("Unexpected t4[0] = {}", t4[0]);
							throw new SecuritiesException("Unexpected");
						}
					} else {
						logger.error("Unexpected");
						throw new SecuritiesException("Unexpected");
					}
				}
			}

			preferredList.add(
				new Preferred(symbol, type, parent, country, name, cpnRate, annAmt, liqPref, remark, callPrice, callDate, maturDate));
		}
		
		Preferred.save(preferredList);
	}
	
	public static void main(String[] args) {
		logger.info("START");
		update();
		logger.info("STOP");
	}
}
