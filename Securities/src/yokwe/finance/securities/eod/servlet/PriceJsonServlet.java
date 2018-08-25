package yokwe.finance.securities.eod.servlet;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.eod.Price;
import yokwe.finance.securities.eod.UpdatePrice;
import yokwe.finance.securities.util.JSONUtil;
import yokwe.finance.securities.util.JSONUtil.ClassInfo;

public class PriceJsonServlet extends HttpServlet {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PriceJsonServlet.class);
	
	private static final long serialVersionUID = 1L;
	
	private String path_base;

	@Override
	public void init(ServletConfig config) {
		logger.info("init");
		
		ServletContext servletContext = config.getServletContext();
		
		path_base = servletContext.getInitParameter("path.base");
		logger.info("path_base {}", path_base);
	}
	
	@Override
	public void destroy() {
		logger.info("destroy");
	}
	
	private static ClassInfo classInfo = JSONUtil.getClassInfo(Price.class);
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) {
		logger.info("doGet START");
		
		// symbol
		String symbolString = req.getParameter("symbol");
		logger.debug("symbolString {}", symbolString);
		if (symbolString == null) {
			symbolString = "";
		}
		String[] symbols = symbolString.split(",");
		logger.debug("symbols {}", Arrays.asList(symbols));
		
		// filter
		String filterString = req.getParameter("filter");
		logger.debug("filterString {}", filterString);
		String[] filters;
		if (filterString == null) {
			filters = classInfo.fields;
		} else {
			filters = filterString.split(",");
		}
		logger.debug("filters {}", Arrays.asList(filters));
		
		// last
		String lastString = req.getParameter("last");
		logger.debug("lastString {}", lastString);
		if (lastString == null) {
			lastString = "0";
		}
		int last = Integer.valueOf(lastString);
		logger.debug("last {}", last);
		
		
		// Build data
		Map<String, List<Price>> dataMap = new TreeMap<>();
		for(String symbol: symbols) {
			// Skip empty symbol
			if (symbol.length() == 0) continue;
			
			logger.debug("symbol {}", symbol);
			String filePath = UpdatePrice.getCSVPath(path_base, symbol);
			logger.debug("filePath {}", filePath);
			File file = new File(filePath);
			if (!file.exists()) {
				logger.warn("file doesn't exist {}", filePath);
				continue;
			}
			
			List<Price> priceList = UpdatePrice.load(file);
			if (priceList == null) {
				logger.warn("priceList == null {}", filePath);
				continue;
			}
			if (priceList.isEmpty()) {
				logger.warn("priceList.isEmpty() {}", filePath);
				continue;
			}
			
			if (0 < last) {
				priceList = JSONUtil.getLastElement(priceList, last);
			}
			
			dataMap.put(symbol, priceList);
		}
		logger.debug("dataMap {}", dataMap.size());
		
		// Prepare for output response
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        
        try (Writer writer = res.getWriter()) {
    		JsonGenerator gen = Json.createGenerator(writer);
    		
     		gen.writeStartObject();
    		for(Map.Entry<String, List<Price>> entry: dataMap.entrySet()) {
    			String symbol = entry.getKey();
    			List<Price> dataList = entry.getValue();
    			
    			logger.debug("entry {} {}", symbol, dataList.size());
    			
    			gen.writeStartObject(symbol);
    			
    			// Then output field in filters
    			for(String filter: filters) {
    				// Skip symbol
    				if (filter.equals("symbol")) continue;
    				
    				JSONUtil.buildArray(gen, filter, dataList);
    			}
    			
    			gen.writeEnd();
    		}
    		gen.writeEnd();
    		gen.flush();
       } catch (IOException e) {
			logger.error("IOException {}", e.getMessage());
			throw new SecuritiesException("IOException");
		}

		logger.info("doGet STOP");
	}
}
