package yokwe.finance.securities.monex;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.FileUtil;

public class UpdateMonexUS {
	private static final Logger logger = LoggerFactory.getLogger(UpdateMonexUS.class);

	public static final String SOURCE_ENCODING  = "SHIFT_JIS";
	
	public static final String PATH_SOUTH      = "tmp/fetch/monex/UsMeigaraJsonGST";
	public static final String PATH_MONEX_US = "tmp/monex/monex-us.csv";

	private static final String  PATTERN_STRING = "\\((.+)\\)";
	private static final Pattern PATTERN = Pattern.compile(PATTERN_STRING, (Pattern.MULTILINE | Pattern.DOTALL));
	
	public static void main(String[] args) {
		logger.info("START");
		
		File file = new File(PATH_SOUTH);
		String contents = FileUtil.read(file, SOURCE_ENCODING);
		String content2 = contents.replaceAll(",\\s+]", "]"); // Remove comma of last array element
		Matcher matcher = PATTERN.matcher(content2);
		if (!matcher.find()) {
			logger.error("Unexpected");
			throw new SecuritiesException();
		}
		
		String match = matcher.group(1);
//		logger.info("match = {}", match.length());
//		logger.info("match = {} ... {}", match.substring(0, 20), match.substring(match.length() - 20, match.length()));
		try (JsonReader reader = Json.createReader(new StringReader(match))) {
			
			JsonObject jsonObject = reader.readObject();
			JsonArray data = jsonObject.getJsonArray("data");
//			logger.info("data = {}", data.size());
			
			List<MonexUS> monexUSList = new ArrayList<>(data.size());

			int count = 0;
			int countETF = 0;
			for(int i = 0; i < data.size(); i++) {
				JsonObject element = data.getJsonObject(i);
				if (element.getString("name").length() == 0) continue;
				
//				logger.info("element {}", element.toString());
				String ticker  = element.getString("Ticker");
				String name    = element.getString("name");
				String jname   = element.getString("jname");
				String keyword = element.getString("keyword");
				String etf     = element.getString("etf");
				String shijo   = element.getString("shijo");
				String update  = element.getString("update");
				
				// Stock
				String gyoshu  = element.getString("gyoshu");
				String jigyo   = element.getString("jigyo");
				
				// ETF
				String benchmark = element.getString("benchmark");
				String shisan    = element.getString("shisan");
				String chiiki    = element.getString("chiiki");
				String category  = element.getString("category");
				String keihi     = element.getString("keihi");
				String comp      = element.getString("comp");
				String pdf       = element.getString("pdf");
				
//				logger.info("{} - {}", ticker, name);
				
				count++;
				if (etf.equals("1")) countETF++;

				MonexUS usSecurity = new MonexUS(ticker, name, jname, keyword, etf, shijo, update,
						gyoshu, jigyo,
						benchmark, shisan, chiiki, category, keihi, comp, pdf);
				
				monexUSList.add(usSecurity);
			}
			logger.info("output = {}", PATH_MONEX_US);
			MonexUS.save(monexUSList);
			
			logger.info("ETF   = {}", String.format("%5d", countETF));
			logger.info("STOCK = {}", String.format("%5d", count - countETF));
			logger.info("TOTAL = {}", String.format("%5d", count));
		}

		logger.info("STOP");		
	}
}
