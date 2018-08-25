package yokwe.finance.securities.eod.servlet;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.eod.StockHistory;
import yokwe.finance.securities.eod.StockInfo;
import yokwe.finance.securities.util.JSONUtil;
import yokwe.finance.securities.util.JSONUtil.ClassInfo;

public class StockServlet extends HttpServlet {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(StockServlet.class);
	
	private static final long serialVersionUID = 1L;
	
	private String pathBase;
	private StockInfo stockInfo;
	
	@Override
	public void init(ServletConfig config) {
		logger.info("init");
		
		ServletContext servletContext = config.getServletContext();
		
		pathBase = servletContext.getInitParameter("path.base");
		logger.info("path_base {}", pathBase);
		
		stockInfo = new StockInfo(pathBase);
	}
	
	@Override
	public void destroy() {
		logger.info("destroy");
	}
	
	private static ClassInfo classInfo = JSONUtil.getClassInfo(StockHistory.class);
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) {
		logger.info("doGet START");
		
		// active
		boolean onlyActive = req.getParameter("onlyActive") != null;
		
		// active
		boolean onlyLast = req.getParameter("onlyLast") != null;
		
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
		
		// symbol
		String symbolString = req.getParameter("symbol");
		logger.debug("symbolString {}", symbolString);
		Set<String> symbolSet;
		if (symbolString == null) {
			symbolSet = null;
		} else {
			symbolSet = new TreeSet<>();
			for(String symbol: symbolString.split(",")) {
				if (symbol.length() == 0) continue;
				symbolSet.add(symbol);
			}
		}
		logger.debug("symbolSet {}", symbolSet);
		
		// Prepare for output response
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        
        try (Writer writer = res.getWriter()) {
    		JsonGenerator gen = Json.createGenerator(writer);
    		
     		gen.writeStartObject();
    		for(StockInfo.Entry entry: stockInfo.getEntryMap().values()) {
    			if (onlyActive) {
    				if (!entry.active) continue;
    			}
    			
    			if (symbolSet != null) {
    				if (!symbolSet.contains(entry.symbol)) continue;
    			}
    			
    			List<StockHistory> dataList = new ArrayList<>();
    			if (onlyLast) {
    				dataList.add(entry.lastStockHistory);
    			} else {
    				dataList.addAll(entry.lastStockHistoryList);
    			}
    			gen.writeStartObject(entry.symbol);
    			
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
