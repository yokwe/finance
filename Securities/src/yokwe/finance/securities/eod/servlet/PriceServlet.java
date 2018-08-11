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

public class PriceServlet extends HttpServlet {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PriceServlet.class);
	
	private static final long serialVersionUID = 1L;
	
	private String path_eod;
	private String path_price;
	@Override
	public void init(ServletConfig config) {
		logger.info("init csv");
		
		ServletContext servletContext = config.getServletContext();
		
		path_eod = servletContext.getInitParameter("path.eod");
		logger.info("path_eod {}", path_eod);
		
		path_price = String.format("%s/price", path_eod);
		logger.info("path_price {}", path_price);
	}
	
	@Override
	public void destroy() {
		logger.info("destroy csv");
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) {
		logger.info("doGet START");
		
		String symbolString = req.getParameter("symbol");
		logger.debug("symbolString {}", symbolString);
		if (symbolString == null) {
			symbolString = "";
		}
		String[] symbols = symbolString.split(",");
		logger.debug("symbols {}", Arrays.asList(symbols));
		
		Map<String, List<Price>> map = new TreeMap<>();
		for(String symbol: symbols) {
			logger.debug("symbol {}", symbol);
			String filePath = UpdatePrice.getCSVPath(path_price, symbol);
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
			map.put(symbol, priceList);
		}
		logger.debug("map {}", map.size());
		
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        
        try (Writer writer = res.getWriter()) {
    		JsonGenerator gen = Json.createGenerator(writer);
    		
     		gen.writeStartObject();
    		for(Map.Entry<String, List<Price>> entry: map.entrySet()) {
    			String symbol = entry.getKey();
    			List<Price> dataList = entry.getValue();
    			
    			logger.debug("entry {} {}", symbol, dataList.size());
    			
    			gen.writeStartArray(symbol);
    			
    			for(Price data: dataList) {
    				gen.
    				writeStartObject().
    					write("date",   data.date).
//    					write("symbol", data.symbol).
//    					write("open",   data.open).
//    					write("high",   data.high).
//    					write("low",    data.low).
    					write("close",  data.close).
//    					write("volume", data.volume).
    				writeEnd();

    			}
    			gen.writeEnd();
    		}
    		gen.writeEnd();
    		gen.flush();
       } catch (IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new SecuritiesException();
		}

		logger.info("doGet STOP");
	}
}
