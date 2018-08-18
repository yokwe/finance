package yokwe.finance.securities.eod.servlet;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.eod.UpdatePrice;
import yokwe.finance.securities.util.FileUtil;

public class PriceCSVServlet extends HttpServlet {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PriceCSVServlet.class);
	
	private static final long serialVersionUID = 1L;
	
	private String path_eod;
	private String path_price;
	@Override
	public void init(ServletConfig config) {
		logger.info("init");
		
		ServletContext servletContext = config.getServletContext();
		
		path_eod = servletContext.getInitParameter("path.eod");
		logger.info("path_eod {}", path_eod);
		
		path_price = String.format("%s/price", path_eod);
		logger.info("path_price {}", path_price);
	}
	
	@Override
	public void destroy() {
		logger.info("destroy");
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) {
		logger.info("doGet START");
		
		String symbol = req.getParameter("symbol");
		logger.debug("symbol {}", symbol);
		
		String filePath = UpdatePrice.getCSVPath(path_price, symbol);
		logger.debug("filePath {}", filePath);
		File file = new File(filePath);
		if (!file.exists()) {
			logger.warn("file doesn't exist {}", filePath);
		}
		
		String contents = FileUtil.read(file);
		
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType("text/csv");
        res.setCharacterEncoding("UTF-8");
        
        try (Writer writer = res.getWriter()) {
        	writer.write(contents);
        	writer.flush();
       } catch (IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new SecuritiesException();
		}

		logger.info("doGet STOP");
	}
}
