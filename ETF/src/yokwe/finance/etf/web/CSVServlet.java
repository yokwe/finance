package yokwe.finance.etf.web;

import java.io.IOException;
import java.io.Writer;

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
	}
	
	@Override
	public void init(ServletConfig config) {
		logger.info("init csv");
	}
	
	@Override
	public void destroy() {
		logger.info("destroy csv");
	}
	
	private static String CRLF = "\r\n";
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		logger.info("doGet START");
		
		logger.info("parameterMap = {}", req.getParameterMap());
		
		StringBuilder buf = new StringBuilder();
		{
			buf.append("date,New York,San Francisco,Austin").append(CRLF);
			buf.append("2011-10-01,63.4,62.7,72.2").append(CRLF);
			buf.append("2011-10-02,58.0,59.9,67.7").append(CRLF);
			buf.append("2011-10-03,53.3,59.1,69.4").append(CRLF);
			buf.append("2011-10-04,55.7,58.8,68.0").append(CRLF);
			buf.append("2011-10-05,64.2,58.7,72.4").append(CRLF);
		}
		
		resp.setContentType("text/csv; charset=UTF-8");
		// No need to set content length of response.
		// Catalina will use chunked encoding for unknown content length
		try {
			resp.getWriter().append(buf.toString());
		} catch (IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
		}
		logger.info("doGet STOP");
	}
}
