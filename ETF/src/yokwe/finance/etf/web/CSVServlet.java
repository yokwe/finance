package yokwe.finance.etf.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;

public class CSVServlet extends HttpServlet {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(CSVServlet.class);
	
	private static final long serialVersionUID = 1L;
	
	static {
		logger.info("load csv");
		System.out.println("load CSV");
	}
	
	@Override
	public void init(ServletConfig config) {
		logger.info("init csv");
	}
	
	@Override
	public void destroy() {
		logger.info("destroy csv");
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		logger.info("doGet START");
		
		resp.setCharacterEncoding("UTF-9");
		resp.setContentType("text/csv");
		resp.setStatus(HttpServletResponse.SC_OK);
		try {
			PrintWriter writer = resp.getWriter();
			
			writer.println("A,B,C");
			writer.println("1,2,3");
			writer.println("11,22,33");
			writer.println("111,222,333");
			writer.flush();
		} catch (IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
		}
		logger.info("doGet STOP");
	}
}
