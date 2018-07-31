package yokwe.finance.securities.iex;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.util.HttpUtil;

public class IEXBase {
	private static final Logger logger = LoggerFactory.getLogger(IEXBase.class);
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface JSONName {
		String value();
	}
	
	public static enum Range {
		Y1("1y"), Y2("2y"), Y5("5y"), YTD("ytd"),
		M6("6m"), M3("3m"), M1("1m"),
		LAST("1y&chartLast=1"); // Use 1y in LAST for annually dividend
		
		public final String value;
		Range(String value) {
			this.value = value;
		}
	}

	// Use New York time in LocalDateTime
	public static final ZoneId ZONE_ID = ZoneId.of("America/New_York");
	
	public static final String END_POINT = "https://api.iextrading.com/1.0";
	
	public static final int MAX_PARAM = 100;
	
	public static final LocalDateTime DEFAULT_LOCAL_DATE_TIME = LocalDateTime.ofInstant(Instant.EPOCH, ZONE_ID);

	private static class ClassInfo {
		private static Map<String, ClassInfo> map = new TreeMap<>();
		
		public static ClassInfo get(Object o) {
			return get(o.getClass());
		}
		public static ClassInfo get(Class<? extends Object> clazz) {
			String key = clazz.getName();
			
			if (!map.containsKey(key)) {
				ClassInfo classInfo = new ClassInfo(clazz);
				map.put(key, classInfo);
			}
			return map.get(key);
		}

		public final String   clazzName;
		public final Field[]  fields;
		public final String[] types;
		public final String[] names;
		public final int      size;
		
		ClassInfo(Class<? extends Object> clazz) {
			List<Field>  fieldList = new ArrayList<>();
			List<String> typeList  = new ArrayList<>();
			List<String> nameList  = new ArrayList<>();

			for(Field field: clazz.getDeclaredFields()) {
				// Skip static field
				if (Modifier.isStatic(field.getModifiers())) continue;

				fieldList.add(field);
				typeList.add(field.getType().getName());
				
				// Use JSONName if exists.
				JSONName jsonName = field.getDeclaredAnnotation(JSONName.class);
				nameList.add((jsonName == null) ? field.getName() : jsonName.value());
			}
			
			this.clazzName = clazz.getName();
			this.fields    = fieldList.toArray(new Field[0]);
			this.types     = typeList.toArray(new String[0]);
			this.names     = nameList.toArray(new String[0]);
			this.size      = fieldList.size();
		}
		
		@Override
		public String toString() {
			List<String> typeList  = Arrays.asList(types);
			List<String> nameList  = Arrays.asList(names);
			
			return String.format("%s %s %s", clazzName, nameList, typeList);
		}
	}

	@Override
	public String toString() {
		Object o = this;
		try {
			ClassInfo classInfo = ClassInfo.get(o);

			List<String> result = new ArrayList<>();
			StringBuilder line = new StringBuilder();
			
			for(int i = 0; i < classInfo.size; i++) {
				String name  = classInfo.names[i];
				String type  = classInfo.types[i];
				Field  field = classInfo.fields[i];
				
				line.setLength(0);
				line.append(name).append(": ");
				
				switch(type) {
				case "double":
					line.append(Double.toString(field.getDouble(o)));
					break;
				case "float":
					line.append(field.getFloat(o));
					break;
				case "long":
					line.append(field.getLong(o));
					break;
				case "int":
					line.append(field.getInt(o));
					break;
				case "short":
					line.append(field.getShort(o));
					break;
				case "byte":
					line.append(field.getByte(o));
					break;
				case "char":
					line.append(String.format("'%c'", field.getChar(o)));
					break;
				default:
				{
					Object value = field.get(o);
					if (value == null) {
						line.append("null");
					} else if (value instanceof String) {
						// Quote special character in string \ => \\  " => \"
						String stringValue = value.toString().replace("\\", "\\\\").replace("\"", "\\\"");
						line.append("\"").append(stringValue).append("\"");
					} else if (field.getType().isArray()) {
						List<String> arrayElement = new ArrayList<>();
						int length = Array.getLength(value);
						for(int j = 0; j < length; j++) {
							Object element = Array.get(value, j);
							if (element instanceof String) {
								// Quote special character in string \ => \\  " => \"
								String stringValue = element.toString().replace("\\", "\\\\").replace("\"", "\\\"");
								arrayElement.add(String.format("\"%s\"", stringValue));
							} else {
								arrayElement.add(String.format("%s", element.toString()));
							}
						}						
						line.append("[").append(String.join(", ", arrayElement)).append("]");
					} else {
						line.append(value.toString());
					}
				}
					break;
				}
				result.add(line.toString());
			}
			
			return String.format("{%s}", String.join(", ", result));
		} catch (IllegalAccessException e) {
			logger.error("IllegalAccessException {}", e.toString());
			throw new IEXUnexpectedError("IllegalAccessException");
		}
	}

	protected IEXBase() {
		//
	}
	protected IEXBase(JsonObject jsonObject) {
		try {
			ClassInfo classInfo = ClassInfo.get(this);
			for(int i = 0; i < classInfo.size; i++) {
				String name  = classInfo.names[i];
				String type  = classInfo.types[i];
				Field  field = classInfo.fields[i];
				
				// Skip field if name is not exist in jsonObject
				if (!jsonObject.containsKey(name))continue;
				
				ValueType valueType = jsonObject.get(name).getValueType();
				
//				logger.debug("parse {} {} {}", name, valueType.toString(), type);
				
				switch(valueType) {
				case NUMBER:
				{
					JsonNumber jsonNumber = jsonObject.getJsonNumber(name);
					
					switch(type) {
					case "double":
						field.set(this, jsonNumber.doubleValue());
						break;
					case "long":
						field.set(this, jsonNumber.longValue());
						break;
					case "java.math.BigDecimal":
						field.set(this, jsonNumber.bigDecimalValue());
						break;
					case "java.lang.String":
						// To handle irregular case in Symbols, add this code. Value of iexId in Symbols can be number or String.
						field.set(this, jsonNumber.toString());
						break;
					case "java.time.LocalDateTime":
						field.set(this, LocalDateTime.ofInstant(Instant.ofEpochMilli(jsonNumber.longValue()), ZONE_ID));
						break;
					default:
						logger.error("Unexptected type {} {} {}", name, valueType.toString(), type);
						throw new IEXUnexpectedError("Unexptected type");
					}
				}
					break;
				case STRING:
				{
					JsonString jsonString = jsonObject.getJsonString(name);
					switch(type) {
					case "java.lang.String":
						field.set(this, jsonString.getString());
						break;
					case "double":
						field.set(this, Double.valueOf((jsonString.getString().length() == 0) ? "0" : jsonString.getString()));
						break;
					default:
						logger.error("Unexptected type {} {} {}", name, valueType.toString(), type);
						throw new IEXUnexpectedError("Unexptected type");
					}
				}
					break;
				case TRUE:
				{
					switch(type) {
					case "boolean":
						field.set(this, true);
						break;
					default:
						logger.error("Unexptected type {} {} {}", name, valueType.toString(), type);
						throw new IEXUnexpectedError("Unexptected type");
					}
				}
					break;
				case FALSE:
				{
					switch(type) {
					case "boolean":
						field.set(this, false);
						break;
					default:
						logger.error("Unexptected type {} {} {}", name, valueType.toString(), type);
						throw new IEXUnexpectedError("Unexptected type");
					}
				}
					break;
				case NULL:
				{
					switch(type) {
					case "double":
						field.set(this, Double.NaN);
						break;
					case "long":
						field.set(this, 0);
						break;
					case "java.time.LocalDateTime":
						field.set(this, DEFAULT_LOCAL_DATE_TIME);
						break;
					case "java.lang.String":
						field.set(this, "");
						break;
					default:
						logger.error("Unexptected type {} {} {}", name, valueType.toString(), type);
						throw new IEXUnexpectedError("Unexptected type");
					}
				}
					break;
				case OBJECT:
				{
					Class<?> fieldType = field.getType();
					
					if (IEXBase.class.isAssignableFrom(fieldType)) {
						JsonObject childJson = jsonObject.get(name).asJsonObject();
//						logger.info("childJson {}", childJson.toString());
						
						IEXBase child = (IEXBase)fieldType.getDeclaredConstructor(JsonObject.class).newInstance(childJson);
//						logger.info("child {}", child.toString());
						
						field.set(this, child);
					} else {
						logger.error("Unexptected type {} {} {}", name, valueType.toString(), type);
						throw new IEXUnexpectedError("Unexptected type");
					}
				}
					break;
				case ARRAY:
				{					
					Class<?> fieldType = field.getType();
					if (fieldType.isArray()) {
						JsonArray childJson = jsonObject.get(name).asJsonArray();

						Class<?> componentType = fieldType.getComponentType();
						String componentTypeName = componentType.getName();
						switch(componentTypeName) {
						case "java.lang.String":
						{
							int childJsonArraySize = childJson.size();
							String[] value = new String[childJson.size()];
							
							for(int j = 0; j < childJsonArraySize; j++) {
								JsonValue childJsonValue = childJson.get(j);
								switch(childJsonValue.getValueType()) {
								case STRING:
									value[j] = childJson.getString(j);
									break;
								default:
									logger.error("Unexpected json array element type {} {}", name, childJsonValue.getValueType().toString());
									throw new IEXUnexpectedError("Unexpected json array element type");
								}
							}
							field.set(this, value);
						}
							break;
						default:
							logger.error("Unexpected array component type {} {}", name, componentTypeName);
							throw new IEXUnexpectedError("Unexpected array component type");
						}
					} else {
						logger.error("Unexptected field is not Array {} {}", name, type);
						throw new IEXUnexpectedError("Unexptected field is not Array");
					}
				}
					break;
				default:
					logger.error("Unknown valueType {} {}", name, valueType.toString());
					throw new IEXUnexpectedError("Unknown valueType");
				}
			}
		} catch (IllegalAccessException e) {
			logger.error("IllegalAccessException {}", e.toString());
			throw new IEXUnexpectedError("IllegalAccessException");
		} catch (InstantiationException e) {
			logger.error("InstantiationException {}", e.toString());
			throw new IEXUnexpectedError("InstantiationException");
		} catch (IllegalArgumentException e) {
			logger.error("IllegalArgumentException {}", e.toString());
			throw new IEXUnexpectedError("IllegalArgumentException");
		} catch (InvocationTargetException e) {
			logger.error("InvocationTargetException {}", e.toString());
			throw new IEXUnexpectedError("InvocationTargetException");
		} catch (NoSuchMethodException e) {
			logger.error("NoSuchMethodException {}", e.toString());
			throw new IEXUnexpectedError("NoSuchMethodException");
		} catch (SecurityException e) {
			logger.error("SecurityException {}", e.toString());
			throw new IEXUnexpectedError("SecurityException");
		}
	}

	private static Map<String, String> typeMap = new TreeMap<>();
	private static String getType(Class<? extends IEXBase> clazz) {
		try {
			String clazzName = clazz.getName();
			if (typeMap.containsKey(clazzName)) {
				return typeMap.get(clazzName);
			}

			Field field = clazz.getDeclaredField("TYPE");
			String fieldTypeName = field.getType().getName();
			if (!fieldTypeName.equals("java.lang.String")) {
				logger.error("Unexpected fieldTypeName {}", fieldTypeName);
				throw new IEXUnexpectedError("Unexpected fieldTypeName");
			}
			Object value = field.get(null);
			if (value instanceof String) {
				typeMap.put(clazzName, (String)value);
				return (String)value;
			} else {
				logger.error("Unexpected value {}", value.getClass().getName());
				throw new IEXUnexpectedError("Unexpected value");
			}
		} catch (NoSuchFieldException e) {
			logger.error("NoSuchFieldException {}", e.toString());
			throw new IEXUnexpectedError("NoSuchFieldException");
		} catch (SecurityException e) {
			logger.error("SecurityException {}", e.toString());
			throw new IEXUnexpectedError("SecurityException");
		} catch (IllegalArgumentException e) {
			logger.error("IllegalArgumentException {}", e.toString());
			throw new IEXUnexpectedError("IllegalArgumentException");
		} catch (IllegalAccessException e) {
			logger.error("IllegalAccessException {}", e.toString());
			throw new IEXUnexpectedError("IllegalAccessException");
		}
	}
	
	private static String[] encodeSymbol(String[] symbols) {
		try {
			String[] ret = new String[symbols.length];
			for(int i = 0; i < symbols.length; i++) {
				ret[i] = URLEncoder.encode(symbols[i], "UTF-8");
			}
			return ret;
		} catch (UnsupportedEncodingException e) {
			logger.error("UnsupportedEncodingException {}", e.toString());
			throw new IEXUnexpectedError("UnsupportedEncodingException");
		}
	}
	// For Company, DelayedQuote, OHLC, Quote and Stats
	protected static <E extends IEXBase> Map<String, E> getStockObject(Class<E> clazz, String ... symbols) {
		// Sanity check
		if (symbols.length == 0) {
			logger.error("symbols.length == 0");
			throw new IEXUnexpectedError("symbols.length == 0");
		}
		
		String type = getType(clazz);
		String url = String.format("%s/stock/market/batch?types=%s&symbols=%s", END_POINT, type, String.join(",", encodeSymbol(symbols)));
		String jsonString = HttpUtil.downloadAsString(url);
		if (jsonString == null) {
			logger.error("jsonString == null");
			throw new IEXUnexpectedError("jsonString == null");
		}
		
		// {"IBM":{"ohlc":{"open":{"price":146.89,"time":1532698210193},"close":{"price":145.15,"time":1532721693191},"high":147.14,"low":144.66}},"BT":{"ohlc":{"open":{"price":15.44,"time":1532698504567},"close":{"price":15.48,"time":1532721721103},"high":15.75,"low":15.405}}}
		try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
			Map<String, E> ret = new TreeMap<>();

			ClassInfo classInfo = ClassInfo.get(clazz);

			// Assume result is only one object
			JsonObject result = reader.readObject();
			for(String resultKey: result.keySet()) {
				JsonValue resultChild = result.get(resultKey);
				ValueType resultChildValueType = resultChild.getValueType();
				// Sanity check
				if (resultChildValueType != ValueType.OBJECT) {
					logger.error("Unexpected resultChildValueType {}", resultChildValueType.toString());
					throw new IEXUnexpectedError("Unexpected resultChildValueType");
				}
				JsonObject element = resultChild.asJsonObject();
				String[] elementKeys = element.keySet().toArray(new String[0]);
				// Sanity check
				if (elementKeys.length != 1) {
					logger.error("elementKeys.length {}", element.keySet().toString());
					throw new IEXUnexpectedError("elementKeys.length");
				}
				String elementKey = elementKeys[0];
				JsonValue elementValue = element.get(elementKey);
				ValueType elementValueType = elementValue.getValueType();
				
				// Sanity check
				if (!elementKey.equals(type)) {
					logger.error("Unexpected elementKey {} {} {}", resultKey, type, elementKey);
					throw new IEXUnexpectedError("Unexpected elementKey");
				}
				switch(elementValueType) {
				case OBJECT:
				{
					JsonObject arg = elementValue.asJsonObject();
					// Sanity check
					if (classInfo.size != arg.size()) {
						logger.warn("Unexpected resultChild {} {} {}", resultKey, classInfo.size, arg.size());
						continue;
					}

					E child = (E)clazz.getDeclaredConstructor(JsonObject.class).newInstance(arg);
					ret.put(resultKey, child);
				}
					break;
				default:
					logger.error("Unexpected elementValueType {}", elementValueType);
					throw new IEXUnexpectedError("Unexpected elementValueType");
				}
			}
			return ret;
		} catch (IllegalAccessException e) {
			logger.error("IllegalAccessException {}", e.toString());
			throw new IEXUnexpectedError("IllegalAccessException");
		} catch (InstantiationException e) {
			logger.error("InstantiationException {}", e.toString());
			throw new IEXUnexpectedError("InstantiationException");
		} catch (IllegalArgumentException e) {
			logger.error("IllegalArgumentException {}", e.toString());
			throw new IEXUnexpectedError("IllegalArgumentException");
		} catch (InvocationTargetException e) {
			logger.error("InvocationTargetException {}", e.toString());
			throw new IEXUnexpectedError("InvocationTargetException");
		} catch (NoSuchMethodException e) {
			logger.error("NoSuchMethodException {}", e.toString());
			throw new IEXUnexpectedError("NoSuchMethodException");
		} catch (SecurityException e) {
			logger.error("SecurityException {}", e.toString());
			throw new IEXUnexpectedError("SecurityException");
		}
	}
	
	// For Price
	protected static <E extends IEXBase> Map<String, E> getStockNumber(Class<E> clazz, String ... symbols) {
		// Sanity check
		if (symbols.length == 0) {
			logger.error("symbols.length == 0");
			throw new IEXUnexpectedError("symbols.length == 0");
		}
		String type = getType(clazz);
		String url = String.format("%s/stock/market/batch?types=%s&symbols=%s", END_POINT, type, String.join(",", encodeSymbol(symbols)));
		String jsonString = HttpUtil.downloadAsString(url);
		if (jsonString == null) {
			logger.error("jsonString == null");
			throw new IEXUnexpectedError("jsonString == null");
		}
		
		// {"IBM":{"price":145.15},"BT":{"price":15.48}}
		try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
			Map<String, E> ret = new TreeMap<>();

			// Assume result is only one object
			JsonObject result = reader.readObject();
			for(String resultKey: result.keySet()) {
				JsonValue resultChild = result.get(resultKey);
				ValueType resultChildValueType = resultChild.getValueType();
				// Sanity check
				if (resultChildValueType != ValueType.OBJECT) {
					logger.error("Unexpected resultChildValueType {}", resultChildValueType.toString());
					throw new IEXUnexpectedError("Unexpected resultChildValueType");
				}
				JsonObject element = resultChild.asJsonObject();
				String[] elementKeys = element.keySet().toArray(new String[0]);
				// Sanity check
				if (elementKeys.length != 1) {
					logger.error("elementKeys.length {}", element.keySet().toString());
					throw new IEXUnexpectedError("elementKeys.length");
				}
				String elementKey = elementKeys[0];
				JsonValue elementValue = element.get(elementKey);
				ValueType elementValueType = elementValue.getValueType();
				
				// Sanity check
				if (!elementKey.equals(type)) {
					logger.error("Unexpected elementKey {} {}", type, elementKey);
					throw new IEXUnexpectedError("Unexpected elementKey");
				}
				switch(elementValueType) {
				case NUMBER:
				{
					E child = (E)clazz.getDeclaredConstructor(String.class).newInstance(elementValue.toString());
					ret.put(resultKey, child);
				}
					break;
				default:
					logger.error("Unexpected elementValueType {}", elementValueType);
					throw new IEXUnexpectedError("Unexpected elementValueType");
				}
			}
			return ret;
		} catch (IllegalAccessException e) {
			logger.error("IllegalAccessException {}", e.toString());
			throw new IEXUnexpectedError("IllegalAccessException");
		} catch (InstantiationException e) {
			logger.error("InstantiationException {}", e.toString());
			throw new IEXUnexpectedError("InstantiationException");
		} catch (IllegalArgumentException e) {
			logger.error("IllegalArgumentException {}", e.toString());
			throw new IEXUnexpectedError("IllegalArgumentException");
		} catch (InvocationTargetException e) {
			logger.error("InvocationTargetException {}", e.toString());
			throw new IEXUnexpectedError("InvocationTargetException");
		} catch (NoSuchMethodException e) {
			logger.error("NoSuchMethodException {}", e.toString());
			throw new IEXUnexpectedError("NoSuchMethodException");
		} catch (SecurityException e) {
			logger.error("SecurityException {}", e.toString());
			throw new IEXUnexpectedError("SecurityException");
		}
	}
	
	// For Chart and Dividends
	protected static <E extends IEXBase> Map<String, E[]> getStockArray(Class<E> clazz, Range range, String ... symbols) {
		// Sanity check
		if (symbols.length == 0) {
			logger.error("symbols.length == 0");
			throw new IEXUnexpectedError("symbols.length == 0");
		}
		String type = getType(clazz);
		// https://api.iextrading.com/1.0/stock/market/batch?symbols=ibm,bt&types=dividends&range=1y&chartLast=1
		String url = String.format("%s/stock/market/batch?types=%s&symbols=%s&range=%s", END_POINT, type, String.join(",", encodeSymbol(symbols)), range.value);
		String jsonString = HttpUtil.downloadAsString(url);
		if (jsonString == null) {
			logger.error("jsonString == null");
			throw new IEXUnexpectedError("jsonString == null");
		}
		
		// {"IBM":{"dividends":[{"exDate":"2017-05-08","paymentDate":"2017-06-10","recordDate":"2017-05-10","declaredDate":"2017-04-25","amount":1.5,"flag":"","type":"Dividend income","qualified":"Q","indicated":""}]}}
		try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
			Map<String, E[]> ret = new TreeMap<>();

			ClassInfo classInfo = ClassInfo.get(clazz);

			// Assume result is only one object
			JsonObject result = reader.readObject();
			for(String resultKey: result.keySet()) {
				JsonValue resultChild = result.get(resultKey);
				ValueType resultChildValueType = resultChild.getValueType();
				// Sanity check
				if (resultChildValueType != ValueType.OBJECT) {
					logger.error("Unexpected resultChildValueType {}", resultChildValueType.toString());
					throw new IEXUnexpectedError("Unexpected resultChildValueType");
				}
				JsonObject element = resultChild.asJsonObject();
				String[] elementKeys = element.keySet().toArray(new String[0]);
				// Sanity check
				if (elementKeys.length != 1) {
					logger.error("elementKeys.length {}", element.keySet().toString());
					throw new IEXUnexpectedError("elementKeys.length");
				}
				String elementKey = elementKeys[0];
				JsonValue elementValue = element.get(elementKey);
				ValueType elementValueType = elementValue.getValueType();
				
				// Sanity check
				if (!elementKey.equals(type)) {
					logger.error("Unexpected elementKey {} {}", type, elementKey);
					throw new IEXUnexpectedError("Unexpected elementKey");
				}
				switch(elementValueType) {
				case ARRAY:
				{
					JsonArray jsonArray = elementValue.asJsonArray();
					int jsonArraySize = jsonArray.size();
					
					Object child = Array.newInstance(clazz, jsonArraySize);
					@SuppressWarnings("unchecked")
					E[] childArray = (E[])child;
					
					for(int i = 0; i < jsonArraySize; i++) {
						JsonObject arg = jsonArray.getJsonObject(i);
						E e = clazz.getDeclaredConstructor(JsonObject.class).newInstance(arg);
						
						// Assign default value, if field value is null)
						for(int j = 0; j < classInfo.size; j++) {
							Object o = classInfo.fields[j].get(e);
							// If field is null, assign default value
							if (o == null) {
								switch(classInfo.types[j]) {
								case "double":
									logger.warn("Assign defautl value  {} {} {}", classInfo.clazzName, classInfo.names[j], classInfo.types[j]);
									classInfo.fields[j].setDouble(o, 0);
									break;
								default:
									logger.error("Unexpected field type {} {} {}", classInfo.clazzName, classInfo.names[i], classInfo.types[i]);
									throw new IEXUnexpectedError("Unexpected field type");
								}
							}
						}

						childArray[i] = e;
					}
					ret.put(resultKey, childArray);
				}
					break;
				default:
					logger.error("Unexpected elementValueType {}", elementValueType);
					throw new IEXUnexpectedError("Unexpected elementValueType");
				}
			}
			return ret;
		} catch (IllegalAccessException e) {
			logger.error("IllegalAccessException {}", e.toString());
			throw new IEXUnexpectedError("IllegalAccessException");
		} catch (InstantiationException e) {
			logger.error("InstantiationException {}", e.toString());
			throw new IEXUnexpectedError("InstantiationException");
		} catch (IllegalArgumentException e) {
			logger.error("IllegalArgumentException {}", e.toString());
			throw new IEXUnexpectedError("IllegalArgumentException");
		} catch (InvocationTargetException e) {
			logger.error("InvocationTargetException {}", e.toString());
			throw new IEXUnexpectedError("InvocationTargetException");
		} catch (NoSuchMethodException e) {
			logger.error("NoSuchMethodException {}", e.toString());
			throw new IEXUnexpectedError("NoSuchMethodException");
		} catch (SecurityException e) {
			logger.error("SecurityException {}", e.toString());
			throw new IEXUnexpectedError("SecurityException");
		}
	}
	
	public static void main(String[] args) {
		logger.info("START");
		
		{
			Map<String, Chart[]> map = Chart.getStock(Range.M1, "ibm", "bt");
			logger.info("chart {}", map.size());
			for(Map.Entry<String, Chart[]> entry: map.entrySet()) {
				logger.info("  {} {} {}", entry.getKey(), entry.getValue().length, Arrays.asList(entry.getValue()).toString());
			}
		}
	
		{
			Map<String, Company> map = Company.getStock("ibm", "bt");
			logger.info("company {}", map.size());
			for(Map.Entry<String, Company> entry: map.entrySet()) {
				logger.info("  {} {}", entry.getKey(), entry.getValue().toString());
			}
		}

		{
			Map<String, DelayedQuote> map = DelayedQuote.getStock("ibm", "bt");
			logger.info("delayedQuote {}", map.size());
			for(Map.Entry<String, DelayedQuote> entry: map.entrySet()) {
				logger.info("  {} {}", entry.getKey(), entry.getValue().toString());
			}
		}

		{
			Map<String, Dividends[]> map = Dividends.getStock(Range.Y1, "ibm", "bt");
			logger.info("dividends {}", map.size());
			for(Map.Entry<String, Dividends[]> entry: map.entrySet()) {
				logger.info("  {} {} {}", entry.getKey(), entry.getValue().length, Arrays.asList(entry.getValue()).toString());
			}
		}

		{
			Map<String, OHLC> map = OHLC.getStock("ibm", "bt");
			logger.info("ohlc {}", map.size());
			for(Map.Entry<String, OHLC> entry: map.entrySet()) {
				logger.info("  {} {}", entry.getKey(), entry.getValue().toString());
			}
		}

		{
			Map<String, Price> map = Price.getStock("ibm", "bt");
			logger.info("price {}", map.size());
			for(Map.Entry<String, Price> entry: map.entrySet()) {
				logger.info("  {} {}", entry.getKey(), entry.getValue().toString());
			}
		}
		
		{
			Map<String, Quote> map = Quote.getStock("ibm", "bt");
			logger.info("quote {}", map.size());
			for(Map.Entry<String, Quote> entry: map.entrySet()) {
				logger.info("  {} {}", entry.getKey(), entry.getValue().toString());
			}
		}
		
		{
			Map<String, Stats> map = Stats.getStock("ibm");
			logger.info("stats {}", map.size());
			for(Map.Entry<String, Stats> entry: map.entrySet()) {
				logger.info("  {} {}", entry.getKey(), entry.getValue().toString());
			}
		}
		
		{
			Symbols[] symbols = Symbols.getRefData();
			logger.info("symbols {}", symbols.length);
//			logger.info("symbols {}", Arrays.asList(symbols).toString());
		}
		
		logger.info("STOP");
	}
}
