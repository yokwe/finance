package yokwe.finance.stock.monex;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.stock.util.CSVUtil;
import yokwe.finance.stock.util.HttpUtil;

public class UpdateFXTax {
	private static final Logger logger = LoggerFactory.getLogger(UpdateFXTax.class);

	public static final String SOURCE_URL       = "https://mst.monex.co.jp/mst/servlet/ITS/ucu/UsEvaluationRateGST";
	public static final String SOURCE_ENCODING  = "SHIFT_JIS";
	
	public static final String PATH_MONEX_TAX_FX = "tmp/monex/monex-fx-tax.csv";
	
//    <tr>
//      <td class="al-c table-sub-th">2018/01/04</td>
//      <td class="al-r">113.75</td>
//      <td class="al-r">111.75</td>
//    </tr>


	private static final String  PATTERN_STRING = "<tr>\\s+<td class=\"al-c table-sub-th\">([0-9/]+)</td>\\s+<td class=\"al-r\">([0-9\\.]+)</td>\\s+<td class=\"al-r\">([0-9\\.]+)</td>\\s+</tr>";
	private static final Pattern PATTERN = Pattern.compile(PATTERN_STRING, (Pattern.MULTILINE | Pattern.DOTALL));
	
	public static void save(List<FXTax> dataList) {
		CSVUtil.saveWithHeader(dataList, PATH_MONEX_TAX_FX);
	}
	public static List<FXTax> load() {
		return CSVUtil.loadWithHeader(PATH_MONEX_TAX_FX, FXTax.class);
	}

	public static void main(String[] args) {
		logger.info("START");
		
		String contents = HttpUtil.downloadAsString(SOURCE_URL, SOURCE_ENCODING);

		Matcher matcher = PATTERN.matcher(contents);
		
		List<FXTax> monexStockFXList = new ArrayList<>();
		
		for(;;) {
			if (!matcher.find()) break;
			
			String date = matcher.group(1);
			String tts = matcher.group(2);
			String ttb = matcher.group(3);
			
			FXTax monexStockFX = new FXTax(date.replaceAll("/", "-"), Double.valueOf(tts), Double.valueOf(ttb));
			monexStockFXList.add(monexStockFX);
			
			logger.info("{}", monexStockFX);
		}
		logger.info("URL    = {}", SOURCE_URL);
		logger.info("OUTPUT = {}", PATH_MONEX_TAX_FX);
		
		save(monexStockFXList);
		logger.info("DATA   = {}", monexStockFXList.size());

		logger.info("STOP");		
	}
}
