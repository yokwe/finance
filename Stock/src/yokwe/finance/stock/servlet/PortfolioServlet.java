package yokwe.finance.stock.servlet;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;

import yokwe.finance.stock.UnexpectedException;
import yokwe.finance.stock.data.StockHistory;
import yokwe.finance.stock.data.StockHistoryUtil;
import yokwe.finance.stock.data.Portfolio;
import yokwe.finance.stock.util.JSONUtil;
import yokwe.finance.stock.util.JSONUtil.ClassInfo;

public class PortfolioServlet extends HttpServlet {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PortfolioServlet.class);
	
	private static final long serialVersionUID = 1L;
	
	private static String PORTFOLIO_FIRSTRADE = "firstrade";
	private static String PORTFOLIO_MONEX     = "monex";
	
	
	private String                 pathBase;
	private Map<String, Portfolio> portfolioMap;
	
	@Override
	public void init(ServletConfig config) {
		logger.info("init");
		
		ServletContext servletContext = config.getServletContext();
		
		pathBase = servletContext.getInitParameter("path.base");
		logger.info("pathBase {}", pathBase);
		
		portfolioMap = new TreeMap<>();
		
		// Firstrade or Monex
		portfolioMap.put(PORTFOLIO_FIRSTRADE, new Portfolio(pathBase, StockHistoryUtil.PATH_STOCK_HISTORY_FIRSTRADE));
		portfolioMap.put(PORTFOLIO_MONEX,     new Portfolio(pathBase, StockHistoryUtil.PATH_STOCK_HISTORY_MONEX));
		
		for(Map.Entry<String, Portfolio> entry: portfolioMap.entrySet()) {
			logger.info("{} {}", entry.getKey(), entry.getValue().getEntryMap().size());
		}
	}
	
	@Override
	public void destroy() {
		logger.info("destroy");
	}
	
	private static ClassInfo classInfo = JSONUtil.getClassInfo(StockHistory.class);
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) {
		logger.info("doGet START");
		
		// name
		String name = req.getParameter("name");
		logger.debug("name {}", name);
		if (name == null) {
			logger.error("name is null");
			throw new UnexpectedException("portfolio is null");
		}
		if (!portfolioMap.containsKey(name)) {
			logger.error("Unknown name", name);
			throw new UnexpectedException("Unknown name");
		}
		Portfolio portfolio = portfolioMap.get(name);
		
		// active
		boolean active = req.getParameter("active") != null;
		
		// active
		boolean last = req.getParameter("last") != null;
		
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
    		for(Portfolio.Entry entry: portfolio.getEntryMap().values()) {
    			if (active) {
    				if (!entry.active) continue;
    			}
    			
    			if (symbolSet != null) {
    				if (!symbolSet.contains(entry.symbol)) continue;
    			}
    			
    			List<StockHistory> dataList = new ArrayList<>();
    			if (last) {
    				dataList.add(entry.lastStockHistory);
    			} else {
    				dataList.addAll(entry.lastStockHistoryList);
    			}
    			gen.writeStartObject(entry.symbol);
    			
    			// Then output field in filters
    			for(String filter: filters) {
    				JSONUtil.buildArray(gen, filter, dataList);
    			}
    			
    			gen.writeEnd();
    		}
    		gen.writeEnd();
    		gen.flush();
       } catch (IOException e) {
			logger.error("IOException {}", e.getMessage());
			throw new UnexpectedException("IOException");
		}

		logger.info("doGet STOP");
	}
}
