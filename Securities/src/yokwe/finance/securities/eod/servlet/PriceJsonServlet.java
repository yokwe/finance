package yokwe.finance.securities.eod.servlet;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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

public class PriceJsonServlet extends HttpServlet {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PriceJsonServlet.class);
	
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
	
	private static <E> List<E> getLastElement(List<E> list, int count) {
		int listSize = list.size();
		return list.subList(Math.max(listSize - count, 0), listSize);
	}
	
	public static class FieldInfo {
		public enum Type {
			BOOLEAN, DOUBLE, FLOAT, INTEGER, LONG, STRING
		}
		
		private static Map<String, Type> typeMap = new TreeMap<>();
		static {
			typeMap.put(boolean.class.getName(), Type.BOOLEAN);
			typeMap.put(double.class.getName(),  Type.DOUBLE);
			typeMap.put(float.class.getName(),   Type.FLOAT);
			typeMap.put(int.class.getName(),     Type.INTEGER);
			typeMap.put(long.class.getName(),    Type.LONG);
			typeMap.put(String.class.getName(),  Type.STRING);
		}

		public final Field  field;
		public final String name;
		public final Type   type;
		
		public FieldInfo(Field field) {
			this.field = field;
			this.name  = field.getName();
			
			String typeName = field.getType().getName();
			if (typeMap.containsKey(typeName)) {
				type = typeMap.get(typeName);
			} else {
				logger.error("Unexpected {}", typeName);
				throw new SecuritiesException("Unexpected");
			}
		}
		
		@Override
		public String toString() {
			return String.format("{%-8s %s}", type, name);
		}
	}
	
	//                 class       field
	private static Map<String, Map<String, FieldInfo>> fieldInfoMap = new TreeMap<>();
	private static Map<String, FieldInfo> getFieldInfoMap(Class<?> clazz) {
		String className = clazz.getName();
		if (!fieldInfoMap.containsKey(className)) {
			Map<String, FieldInfo> map = new TreeMap<>();
			for(Field field: clazz.getDeclaredFields()) {
				int modifier = field.getModifiers();
				// Skip static field
				if (Modifier.isStatic(modifier)) continue;
				
				FieldInfo fieldInfo = new FieldInfo(field);
				map.put(fieldInfo.name, fieldInfo);
			}
			fieldInfoMap.put(className, map);
		}
		return fieldInfoMap.get(className);
	}

	private static <E> void buildArray(JsonGenerator gen, String fieldName, List<E> dataList) {		
		gen.writeStartArray(fieldName);
		
		if (!dataList.isEmpty()) {
			Object o = dataList.get(0);
			Map<String, FieldInfo> fieldInfoMap = getFieldInfoMap(o.getClass());
			
			if (!fieldInfoMap.containsKey(fieldName)) {
				logger.error("Unknown field {} {}", o.getClass().getName(), fieldName);
				throw new SecuritiesException("Unknown field");
			}
			
			FieldInfo fieldInfo = fieldInfoMap.get(fieldName);
			
			try {
				for(E data: dataList) {
					Object fieldValue = fieldInfo.field.get(data);
					if (fieldValue == null) {
						gen.writeNull();
					} else {
						switch(fieldInfo.type) {
						// BOOLEAN, DOUBLE, FLOAT, INTEGER, LONG, STRING
						case BOOLEAN:
							gen.write((boolean)fieldValue);
							break;
						case DOUBLE:
							gen.write((double)fieldValue);
							break;
						case FLOAT:
							gen.write((float)fieldValue);
							break;
						case INTEGER:
							gen.write((int)fieldValue);
							break;
						case LONG:
							gen.write((long)fieldValue);
							break;
						case STRING:
							gen.write((String)fieldValue);
							break;
						default:
							logger.error("Unexpected {}", fieldInfo);
							throw new SecuritiesException("Unexpected");
						}
					}
				}
			} catch (IllegalArgumentException e) {
				logger.error("IllegalArgumentException {}", e.getMessage());
				throw new SecuritiesException("IllegalArgumentException");
			} catch (IllegalAccessException e) {
				logger.error("IllegalAccessException {}", e.getMessage());
				throw new SecuritiesException("IllegalAccessException");
			}
		}
		gen.writeEnd();
	}

	
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
		if (filterString == null) {
			filterString = "close";
		}
		String[] filters = filterString.split(",");
		logger.debug("filters {}", Arrays.asList(filters));
		
		// last
		String lastString = req.getParameter("last");
		logger.debug("lastString {}", lastString);
		if (lastString == null) {
			lastString = "0";
		}
		int last = Integer.valueOf(lastString);
		logger.debug("last {}", last);
		
		Map<String, List<Price>> map = new TreeMap<>();
		for(String symbol: symbols) {
			// Skip empty symbol
			if (symbol.length() == 0) continue;
			
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
			
			if (0 < last) {
				priceList = getLastElement(priceList, last);
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
    			
    			gen.writeStartObject(symbol);
    			
    			// First output date
    			buildArray(gen, "date", dataList);
    			
    			// Then output specified in filters
    			for(String filter: filters) {
        			buildArray(gen, filter, dataList);
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
