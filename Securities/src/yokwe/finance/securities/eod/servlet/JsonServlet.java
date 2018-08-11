package yokwe.finance.securities.eod.servlet;

import java.io.IOException;
import java.io.Writer;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class JsonServlet extends HttpServlet {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(JsonServlet.class);
	
	private static final long serialVersionUID = 1L;
	
	@Override
	public void init(ServletConfig config) {
		logger.info("init csv");
	}
	
	@Override
	public void destroy() {
		logger.info("destroy csv");
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) {
		logger.info("doGet START");
		
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        
        try (Writer writer = res.getWriter()) {
    		JsonGenerator gen = Json.createGenerator(writer);
    		
			gen
				.writeStartObject()
					.write("firstName", "John")
					.write("lastName", "Smith")
					.write("age", 25)
					.writeStartObject("address")
						.write("streetAddress", "21 2nd Street")
						.write("city", "New York")
						.write("state", "NY")
						.write("postalCode", "10021")
					.writeEnd()
					.writeStartArray("phoneNumber")
						.writeStartObject().write("type", "home")
							.write("number", "212 555-1234").writeEnd()
							.writeStartObject()
								.write("type", "fax")
								.write("number", "646 555-4567")
							.writeEnd()
						.writeEnd()
				.writeEnd();
    		gen.flush();
       	
        } catch (IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new SecuritiesException();
		}

		logger.info("doGet STOP");
	}
}
