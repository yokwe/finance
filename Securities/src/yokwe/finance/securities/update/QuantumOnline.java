package yokwe.finance.securities.update;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.util.FileUtil;
import yokwe.finance.securities.SecuritiesException;

// Take security data from QuantamOnline.com
//   http://www.quantumonline.com/search.cfm?tickersymbol=NRF-A&sopt=symbol

public class QuantumOnline {
	private static final Logger logger = LoggerFactory.getLogger(QuantumOnline.class);

	private static final int BUFFER_SIZE = 256 * 1024;
	
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
			ret = ret.replaceAll("<[^>]+>", " ");
			ret = ret.replaceAll("\\p{javaWhitespace}+", " ");
			ret = ret.trim();
			
			// mm/dd/yyyy
			if (ret.matches("^([0-9]{1,2})/([0-9]{1,2})/([0-9]{4})$")) {
				String mdy[] = ret.split("\\/");
				if (mdy[0].length() == 1) mdy[0] = "0" + mdy[0];
				if (mdy[1].length() == 1) mdy[1] = "0" + mdy[1];
				ret = String.format("%s-%s-%s", mdy[2], mdy[0], mdy[1]);
			}
			
			// mm/dd/yyyy?
			if (ret.matches("^([0-9]{1,2})/([0-9]{1,2})/([0-9]{2,4})\\?$")) {
				ret = ret.replace("?", "");
				String mdy[] = ret.split("\\/");
				if (mdy[0].length() == 1) mdy[0] = "0" + mdy[0];
				if (mdy[1].length() == 1) mdy[1] = "0" + mdy[1];
				if (mdy[2].length() == 2) mdy[2] = "20" + mdy[2];
				ret = String.format("%s?%s?%s", mdy[2], mdy[0], mdy[1]);
			}
			
			// 9.99%
			if (ret.matches("^[0-9]+\\.[0-9]+%$")) {
				ret = String.format("%.4f", Double.valueOf(ret.substring(0, ret.length() - 1)) * 0.01);
			}

			if (ret.matches("^\\$[0-9,\\.]+$")) {
				// $1.40625
				if (ret.matches("^\\$[0-5](\\.[0-9]+)?$")) {
					ret = String.format("%.5f", Double.valueOf(ret.substring(1)));
				} else {
					ret = String.format("%.2f", Double.valueOf(ret.replace(",", "").substring(1)));
				}
			}

			if (ret.equals("n.a."))         ret = "*NA*";
			if (ret.equals("Reset Rate"))   ret = "RESET";
			if (ret.equals("Reset rate"))   ret = "RESET";
			if (ret.equals("Floating"))     ret = "FLOAT";
			if (ret.equals("Fixed/Adj"))    ret = "FIXED_ADJ";
			if (ret.equals("Adj Rate"))     ret = "ADJ";
			if (ret.equals("Var Rate"))     ret = "VAR";
			if (ret.equals("None"))         ret = "NONE";
			if (ret.equals("Tender Offer")) ret = "TENDER";
			if (ret.equals("anytime"))      ret = "ANYTIME";
			if (ret.equals("any time"))     ret = "ANYTIME";
			if (ret.equals("Any time"))     ret = "ANYTIME";
			if (ret.equals("Any Time"))     ret = "ANYTIME";
			if (ret.equals("Partial Call")) ret = "PARTIAL";
			if (ret.equals("Called for"))   ret = "CALLED";
			if (ret.equals("noncallable"))  ret = "NO_REDEEM";
			if (ret.equals("Noncallable"))  ret = "NO_REDEEM";
			if (ret.equals("Nonredeem"))    ret = "NO_REDEEM";
			if (ret.equals("Unredeemable")) ret = "NO_REDEEM";

			return ret;
		}
	}

	private static final Match NAME     = new Match("<font size=\"\\+1\"><center><b>(.+?)<", Pattern.DOTALL);
	private static final Match SYMBOL   = new Match(">Ticker Symbol: ([^ ]+) ");
	private static final Match CUSIP    = new Match("CUSIP: (.+?)&");
	private static final Match EXCHANGE = new Match("Exchange: (.+?)<");
	private static final Match TYPE     = new Match("Security Type:.+?>(.+?)<", Pattern.DOTALL);
	private static final Match CPN_RATE = new Match("<th><font size=\"2\">Cpn Rate<br>Ann Amt</font></th>.+?<td align=\"center\"><font size=\"2\">(.+?)<", Pattern.DOTALL);
	private static final Match ANN_AMT  = new Match("<th><font size=\"2\">Cpn Rate<br>Ann Amt</font></th>.+?<td align=\"center\"><font size=\"2\">.+?<br>(.+?)<", Pattern.DOTALL);
	private static final Match LIQ_PREF = new Match("<th><font size=\"2\">Cpn Rate<br>Ann Amt</font></th>.+?<td align=\"center\"><font size=\"2\">.+?<font size=\"2\">(.+?)<", Pattern.DOTALL);
	private static final Match CALL_PRICE = new Match("<th><font size=\"2\">Cpn Rate<br>Ann Amt</font></th>.+?<td align=\"center\"><font size=\"2\">.+?<font size=\"2\">.+?<br>(.+?)<", Pattern.DOTALL);
	private static final Match CALL_DATE = new Match("<th><font size=\"2\">Cpn Rate<br>Ann Amt</font></th>.+?<td align=\"center\"><font size=\"2\">.+?.+?<td align=\"center\">.+?<font size=\"2\">(.+?)<br>", Pattern.DOTALL);
	private static final Match MATUR_DATE = new Match("<th><font size=\"2\">Cpn Rate<br>Ann Amt</font></th>.+?<td align=\"center\"><font size=\"2\">.+?.+?<td align=\"center\">.+?<font size=\"2\">.+?<br>(.+?)<", Pattern.DOTALL);
	

	public static void save(String dirPath, String csvPath) {
		File root = new File(dirPath);
		if (!root.isDirectory()) {
			logger.error("Not directory  path = {}", dirPath);
			throw new SecuritiesException("not directory");
		}
		
		File[] fileList = root.listFiles();
		Arrays.sort(fileList, (a, b) -> a.getName().compareTo(b.getName()));
		
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(csvPath), BUFFER_SIZE)) {
			// Outpu header
			bw.append("symbol,cpnRate,annAmt,liqPref,callPrice,callDate,maturDate,type,name\n");
			
			int count = 0;
			for(File file: fileList) {
				if (file.length() == 0) {
					logger.warn("Skip empty file {}", file.getName());
					continue;
				}
				
				String content = FileUtil.read(file);
				if (content.contains("Not Found!")) {
//					logger.warn("Skip not found {}", file.getName());
					continue;
				}
				
				
				
				// name symbol exchange 
				if (!content.contains("Security Type:")) {
//					logger.warn("Skip no Security Type {}", file.getName());
					continue;
				}
				if (!content.contains("Cpn Rate")) {
//					logger.warn("Skip no Security Type {}", file.getName());
					continue;
				}
				
				
//				logger.info("FILE {}", file.getName());
				
				String symbol    = SYMBOL.getValue(content);
				String cusip     = CUSIP.getValue(content);
				String exch      = EXCHANGE.getValue(content);
				String type      = TYPE.getValue(content);
				String name      = NAME.getValue(content);
				String cpnRate   = CPN_RATE.getValue(content);
				String annAmt    = ANN_AMT.getValue(content);
				String liqPref   = LIQ_PREF.getValue(content);
				String callPrice = CALL_PRICE.getValue(content);
				String callDate  = CALL_DATE.getValue(content);
				String maturDate = MATUR_DATE.getValue(content);
				
				// name
				name = name.replace("\"", "\"\"");
				if (name.contains(",")) {
					name = "\"" + name + "\"";
				}
				
				// cpnRate
				if (cpnRate.matches("[0-9\\.]+")) {
					cpnRate = String.format("%.5f", Double.valueOf(cpnRate));
				}
				// annAmt
				if (annAmt.matches("[0-9\\.]+")) {
					annAmt = String.format("%.5f", Double.valueOf(annAmt));
				}
				
				logger.info("{}", String.format("%4d %-8s %-9s %-4s %10s %10s %10s %10s %10s %10s %-40s %s", ++count, symbol, cusip, exch, cpnRate, annAmt, liqPref, callPrice, callDate, maturDate, type, name));
				
				// symbol, cpnRate, annAmt, liqPref, callPrice, callDate, maturDate, type, name
				bw.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s\n", symbol, cpnRate, annAmt, liqPref, callPrice, callDate, maturDate, type, name));
			}
		} catch (IOException e) {
			logger.error("IOException {}", e);
			throw new SecuritiesException("IOException");
		}
	}
	
	public static void main(String[] args) {
//		String dirPath = "tmp/fetch/quantum";
//		String csvPath = "tmp/quantum.csv";
		String dirPath = args[0];
		String csvPath = args[1];
		
		logger.info("START");
		logger.info("dirPath = {}", dirPath);
		logger.info("csvPath = {}", csvPath);
		save(dirPath, csvPath);
		logger.info("STOP");
	}
}
