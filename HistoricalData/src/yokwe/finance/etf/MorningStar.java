package yokwe.finance.etf;

import java.io.IOException;
import java.net.MalformedURLException;

import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

//
// this is test of scraping morningstar.com page with htmlunit
//
public class MorningStar {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(MorningStar.class);
	
	public static void main(String[] args) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		logger.info("START");
		WebClient webClient = new WebClient(BrowserVersion.FIREFOX_38);
		webClient.getOptions().setCssEnabled(false);
		webClient.getOptions().setJavaScriptEnabled(true);
		webClient.getOptions().setAppletEnabled(false);
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setUseInsecureSSL(true);
		
		HtmlPage htlmPage = webClient.getPage("http://etfs.morningstar.com/quote?t=QQQ&platform=RET");
//		HtmlPage htlmPage = webClient.getPage("http://www.asahi.com/");
//		HtmlPage htlmPage = webClient.getPage("http://www.msn.com/en-us/money/etfdetails/SPY");
		
		logger.info("Page {}", htlmPage.asXml());
		
		webClient.close();
		logger.info("STOP");
	}
}
