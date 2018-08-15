package yokwe.finance.securities.monex;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.util.CSVUtil;
import yokwe.finance.securities.util.FileUtil;

public class UpdateMonexStockFX {
	private static final Logger logger = LoggerFactory.getLogger(UpdateMonexStockFX.class);

	public static final String SOURCE_ENCODING  = "SHIFT_JIS";
	
	public static final String PATH_SOUTH          = "tmp/fetch/monex/UsEvaluationRateGST";
	public static final String PATH_MONEX_STOCK_FX = "tmp/monex/monex-stock-fx.csv";
	
//    <tr>
//      <td class="al-c table-sub-th">2018/01/04</td>
//      <td class="al-r">113.75</td>
//      <td class="al-r">111.75</td>
//    </tr>


	private static final String  PATTERN_STRING = "<tr>\\s+<td class=\"al-c table-sub-th\">([0-9/]+)</td>\\s+<td class=\"al-r\">([0-9\\.]+)</td>\\s+<td class=\"al-r\">([0-9\\.]+)</td>\\s+</tr>";
	private static final Pattern PATTERN = Pattern.compile(PATTERN_STRING, (Pattern.MULTILINE | Pattern.DOTALL));
	
	public static void save(List<MonexStockFX> dataList) {
		CSVUtil.saveWithHeader(dataList, PATH_MONEX_STOCK_FX);
	}
	public static List<MonexStockFX> load() {
		return CSVUtil.loadWithHeader(PATH_MONEX_STOCK_FX, MonexStockFX.class);
	}

	public static void main(String[] args) {
		logger.info("START");
		
		File file = new File(PATH_SOUTH);
		String contents = FileUtil.read(file, SOURCE_ENCODING);
		Matcher matcher = PATTERN.matcher(contents);
		
		List<MonexStockFX> monexStockFXList = new ArrayList<>();
		
		for(;;) {
			if (!matcher.find()) break;
			
			String date = matcher.group(1);
			String tts = matcher.group(2);
			String ttb = matcher.group(3);
			
			MonexStockFX monexStockFX = new MonexStockFX(date.replaceAll("/", "-"), Double.valueOf(tts), Double.valueOf(ttb));
			monexStockFXList.add(monexStockFX);
			
			logger.info("{}", monexStockFX);
		}
		logger.info("output {}", PATH_MONEX_STOCK_FX);
		
		save(monexStockFXList);
		logger.info("monexStockFXList {}", monexStockFXList.size());

		logger.info("STOP");		
	}
}
