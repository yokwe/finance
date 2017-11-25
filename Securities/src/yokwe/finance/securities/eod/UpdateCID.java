package yokwe.finance.securities.eod;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.FileUtil;
import yokwe.finance.securities.util.HttpUtil;

public class UpdateCID {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateCID.class);

// *First time no cookie*
//GET https://finance.google.com/finance?q=NYSE:IBM HTTP/1.1
//Host: finance.google.com
//User-Agent: Mozilla
//Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
//Accept-Language: en
//Accept-Encoding: gzip, deflate, br
//DNT: 1
//Connection: keep-alive
//Upgrade-Insecure-Requests: 1
	
//HTTP/1.1 200 OK
//Content-Type: text/html; charset=utf-8
//Content-Encoding: gzip
//P3P: CP="This is not a P3P policy! See g.co/p3phelp for more info."
//Date: Sat, 25 Nov 2017 08:53:20 GMT
//Expires: Sat, 25 Nov 2017 08:53:20 GMT
//Cache-Control: private, max-age=0
//X-Content-Type-Options: nosniff
//X-Frame-Options: SAMEORIGIN
//X-XSS-Protection: 1; mode=block
//Server: GSE
//Set-Cookie: NID=118=kNgg26Qv-9yZN4GVTaOiyCldnNcJf4dZI3yDKFGkZDoEM01nWDM-LjaOkBpxgKnYZ5v_G2BVqcis0EvMRT-g35oAN2MVOqI8Og4Id1ioQdpkh_SMeK8gkWB-rg5_w78k;Domain=.google.com;Path=/;Expires=Sun, 27-May-2018 08:53:20 GMT;HttpOnly
//Alt-Svc: hq=":443"; ma=2592000; quic=51303431; quic=51303339; quic=51303338; quic=51303337; quic=51303335,quic=":443"; ma=2592000; v="41,39,38,37,35"
//Transfer-Encoding: chunked
	
	
// *Second time have cookie*
//GET https://finance.google.com/finance?q=NYSE:IBM HTTP/1.1
//Host: finance.google.com
//User-Agent: Mozilla
//Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
//Accept-Language: en
//Accept-Encoding: gzip, deflate, br
//Cookie: NID=118=kNgg26Qv-9yZN4GVTaOiyCldnNcJf4dZI3yDKFGkZDoEM01nWDM-LjaOkBpxgKnYZ5v_G2BVqcis0EvMRT-g35oAN2MVOqI8Og4Id1ioQdpkh_SMeK8gkWB-rg5_w78k; S=quotestreamer=Akapc7PiKPenEimepM8Bk9MBjeqBlDjS; 1P_JAR=2017-11-25-8
//DNT: 1
//Connection: keep-alive
//Upgrade-Insecure-Requests: 1
//Cache-Control: max-age=0

//HTTP/1.1 200 OK
//Content-Type: text/html; charset=utf-8
//Content-Encoding: gzip
//Date: Sat, 25 Nov 2017 08:53:32 GMT
//Expires: Sat, 25 Nov 2017 08:53:32 GMT
//Cache-Control: private, max-age=0
//X-Content-Type-Options: nosniff
//X-Frame-Options: SAMEORIGIN
//X-XSS-Protection: 1; mode=block
//Server: GSE
//Alt-Svc: hq=":443"; ma=2592000; quic=51303431; quic=51303339; quic=51303338; quic=51303337; quic=51303335,quic=":443"; ma=2592000; v="41,39,38,37,35"
//Transfer-Encoding: chunked




	
	public static final String PATH_CID = "tmp/eod/cid.csv";
	public static final String PATH_DIR = "tmp/eod/cid";
	
	private static final long MIN_SLEEP_INTERVAL = 1_000; // 1000 milliseconds = 1 sec
	
	private static String getURL(String exchange, String symbolGoogle) {
		// https://finance.google.com/finance?q=NYSE%3AESV
		return String.format("https://finance.google.com/finance?q=%s:%s", exchange, symbolGoogle);
	}
	
	private static class Match {
		final Matcher matcher;
		
		Match(String pattern) {
			matcher = Pattern.compile(pattern, Pattern.DOTALL).matcher("");
		}
		String getValue(String string) {
			boolean found = matcher.reset(string).find();
			if (!found) {
				logger.error("No match {}", matcher);
				throw new SecuritiesException("No match");
			}
			String ret = matcher.group(1);
			return ret;
		}
	}
	
	private static final Match NAME     = new Match("<meta itemprop=\"name\".+?content=\"(.+?)\" />");
	private static final Match SYMBOL   = new Match("<meta itemprop=\"tickerSymbol\".+?content=\"(.+?)\" />");
	private static final Match EXCHANGE = new Match("<meta itemprop=\"exchange\".+?content=\"(.+?)\" />");
	private static final Match URL_CID  = new Match("<meta itemprop=\"url\".+?content=\".+?cid=(.+?)\" />");
	
	public static String getContents(String exchange, String symbolGoogle) {
		String url = getURL(exchange, symbolGoogle);
		String ret = HttpUtil.downloadAsString(url);
		return ret;
	}
	
	public static CID getCID(String contents) {
		CID ret = null;
		
		if (contents != null && contents.contains("itemprop")) {
			String mySymbol   = SYMBOL.getValue(contents);
			String myExchange = EXCHANGE.getValue(contents);
			String myName     = NAME.getValue(contents);
			String myCID      = URL_CID.getValue(contents);
			
			// Replace
			if (myExchange.equals("NYSEAMERICAN")) myExchange = "NYSEMKT";
			
			ret = new CID(mySymbol, myExchange, myName, myCID);
		}
		
		return ret;
	}
	
	private static String getKey(CID cid) {
		return String.format("%s:%s", cid.exchange, cid.symbol);
	}
	private static String getKey(Stock stock) {
		return String.format("%s:%s", stock.exchange, stock.symbolGoogle);
	}
	
	public static List<CID> getCIDList() {
		// key is getKey(cid)
		Map<String, CID> map = new TreeMap<>();
		
		int countFile = 0;
		int countDup  = 0;
		File dir = new File(PATH_DIR);
		for(File file: dir.listFiles()) {
			countFile++;
			String contents = FileUtil.read(file);
			CID cid = getCID(contents);
			
			String key = getKey(cid);
			if (map.containsKey(key)) {
				countDup++;
				CID oldCID = map.get(key);
				
				if (cid.isEqual(oldCID)) {
					logger.warn("DUPLICATE SAME {}", file.getPath());
				} else {
					logger.warn("DUPLICATE DIFF");
					logger.warn("  OLD {}", oldCID);
					logger.warn("  NEW {}", cid);
				}
			} else {
				map.put(key, cid);
			}
		}
		
		List<CID> ret = new ArrayList<>();
		for(String key: map.keySet()) {
			ret.add(map.get(key));
		}			

		// Sort before returns
		Collections.sort(ret);

		logger.info("countFile {}", String.format("%5d", countFile));
		logger.info("countDup  {}", String.format("%5d", countDup));
		logger.info("countCID  {}", String.format("%5d", ret.size()));

		return ret;
	}

	public static void main(String[] args) {
		logger.info("START");
		
		// Build existing cidList
		Map<String, CID> cidMap = new TreeMap<>();
		for(CID cid: getCIDList()) {
			String key = getKey(cid);
			cidMap.put(key, cid);
		}

		List<Stock>	stockList = Stock.load();
		
		int symbolSize   = stockList.size();
		int showInterval = (symbolSize < 100) ? 1 : 100;
		int count = 0;
		int lastOutputCount = -1;
		
		showInterval = 1;
		
		long lastSleepTime = System.currentTimeMillis();
		
		for(Stock stock: stockList) {
			int outputCount = count / showInterval;
			boolean showOutput;
			if (outputCount != lastOutputCount) {
				showOutput = true;
				lastOutputCount = outputCount;
			} else {
				showOutput = false;
			}
			count++;
			
			String key = getKey(stock);
			
			if (cidMap.containsKey(key)) {
				if (showOutput) logger.info("{}  skip   {}", String.format("%4d / %4d",  count, symbolSize), stock.symbol);				
			} else {
				try {
					long expectedStartTime = lastSleepTime + MIN_SLEEP_INTERVAL;
					long currentTime = System.currentTimeMillis();
					if (currentTime < expectedStartTime) {
						Thread.sleep(Math.min(MIN_SLEEP_INTERVAL, expectedStartTime - currentTime));
						currentTime = System.currentTimeMillis();
					}
					lastSleepTime = currentTime;
				} catch (InterruptedException e) {
					logger.error("InterruptedException {}", e.toString());
					throw new SecuritiesException("InterruptedException");
				}
				
				String contents = getContents(stock.exchange, stock.symbolGoogle);
				CID cid = getCID(contents);
				
				if (cid != null) {
					String timeStamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());

					File cidFile = new File(String.format("%s/%s-%s", PATH_DIR, timeStamp, cid.symbol));
					// create new cid file
					FileUtil.write(cidFile, contents);
					
					/*if (showOutput)*/ logger.info("{}  new    {}", String.format("%4d / %4d",  count, symbolSize), stock.symbol);
				} else {
					// no data in google
					if (showOutput) logger.info("{}  none   {}", String.format("%4d / %4d",  count, symbolSize), stock.symbol);
					if (contents.contains("produced no matches.")) {
						// XXAA
						logger.debug("produced no matches.", key);
					} else if (!contents.contains("Error 404 (Not Found)")) {
						// Error 404 (Not Found)
						logger.error("Error 404 (Not Found)", key);
						throw new SecuritiesException("Error 404 (Not Found)");
					} else if (!contents.contains("52 week")) {
						// NASDAQ:AHPAU
						logger.debug("No 52 week", contents);
						logger.debug("getContents {}", contents);
					} else {
						logger.error("getContents {}", contents);
						
						logger.error("Unexpected {}", key);
						throw new SecuritiesException("Unexpected");
					}
				}
			}
		}
		
		logger.info("SAVE");
		CID.save(getCIDList());
		
		logger.info("STOP");
	}
}
