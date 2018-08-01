package yokwe.finance.securities.iex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.util.CSVUtil;

public class UpdateCompany {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateCompany.class);
	
	public static String getCSVPath() {
		return IEXBase.getCSVPath(Company.class);
	}

	public static void main (String[] args) {
		logger.info("START");
		
		List<String> symbolList = UpdateSymbols.getSymbolList();
		int symbolListSize = symbolList.size();
		logger.info("symbolList {}", symbolList.size());
		
		List<Company.CSV> companyList = new ArrayList<>();
		for(int i = 0; i < symbolListSize; i += IEXBase.MAX_PARAM) {
			int fromIndex = i;
			int toIndex = Math.min(fromIndex + IEXBase.MAX_PARAM, symbolListSize);
			List<String> getList = symbolList.subList(fromIndex, toIndex);
			logger.info("  {} {}", fromIndex, getList.toString());
			
			Map<String, Company> companyMap = Company.getStock(getList.toArray(new String[0]));
			companyMap.values().stream().forEach(o -> companyList.add(new Company.CSV(o)));
		}
		logger.info("companyList {}", companyList.size());
		
		String csvPath = getCSVPath();
		logger.info("csvPath {}", csvPath);
		
		CSVUtil.saveWithHeader(companyList, csvPath);

		logger.info("STOP");
	}

}
