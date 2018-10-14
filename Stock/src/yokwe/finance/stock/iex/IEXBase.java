package yokwe.finance.stock.iex;

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

import yokwe.finance.stock.util.CSVUtil;
import yokwe.finance.stock.util.HttpUtil;

public class IEXBase {
	private static final Logger logger = LoggerFactory.getLogger(IEXBase.class);
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface JSONName {
		String value();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface IgnoreField {
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
	
	public static final LocalDateTime NULL_LOCAL_DATE_TIME = LocalDateTime.ofInstant(Instant.EPOCH, ZONE_ID);
	
	public static final String PATH_DATA_DIR = "tmp/iex";

	public static final String getCSVPath(Class<? extends IEXBase> clazz) {
		return String.format("%s/%s.csv", PATH_DATA_DIR, IEXInfo.get(clazz).type);
	}
	public static final String getCSVPath(Class<? extends IEXBase> clazz, String symbol) {
		return String.format("%s/%s/%s.csv", PATH_DATA_DIR, IEXInfo.get(clazz).type, symbol);
	}
	public static final String getDelistedCSVPath(Class<? extends IEXBase> clazz, String symbol, String suffix) {
		return String.format("%s/%s-delisted/%s.csv-%s", PATH_DATA_DIR, IEXInfo.get(clazz).type, symbol, suffix);
	}
	

	public static class IEXInfo {
		private static Map<String, IEXInfo> map = new TreeMap<>();
		
		public static class FieldInfo {
			public final Field   field;
			public final String  name;
			public final String  jsonName;
			public final String  type;
			public final boolean isArray;
			public final boolean ignoreField;
			
			FieldInfo(Field field) {
				this.field = field;
				
				this.name  = field.getName();

				// Use JSONName if exists.
				JSONName jsonName = field.getDeclaredAnnotation(JSONName.class);
				this.jsonName = (jsonName == null) ? field.getName() : jsonName.value();
				
				Class<?> type = field.getType();
				this.type     = type.getName();
				this.isArray  = type.isArray();
				
				this.ignoreField = field.getDeclaredAnnotation(IgnoreField.class) != null;
			}
			
			@Override
			public String toString() {
				return String.format("{%s %s %s %s}", name, type, isArray, ignoreField);
			}
		}
		
		public static IEXInfo get(IEXBase o) {
			return get(o.getClass());
		}
		public static IEXInfo get(Class<? extends IEXBase> clazz) {
			String key = clazz.getName();
			
			if (map.containsKey(key)) return map.get(key);
			
			IEXInfo value = new IEXInfo(clazz);
			map.put(key, value);
			return value;
		}

		public final String      clazzName;
		public final String      type;
		public final FieldInfo[] fieldInfos;
		public final int         fieldSize;
		public final String      filter;
		
		IEXInfo(Class<? extends IEXBase> clazz) {
			try {
				List<Field> fieldList = new ArrayList<>();
				for(Field field: clazz.getDeclaredFields()) {
					// Skip static field
					if (Modifier.isStatic(field.getModifiers())) continue;
					fieldList.add(field);
				}
				FieldInfo[] fieldInfos = new FieldInfo[fieldList.size()];
				for(int i = 0; i < fieldInfos.length; i++) {
					fieldInfos[i] = new FieldInfo(fieldList.get(i));
				}
				
				int fieldSize = 0;
				{
					for(int i = 0; i < fieldInfos.length; i++) {
						if (fieldInfos[i].ignoreField) continue;
						fieldSize++;
					}
				}
				
				String type;
				{
					Field field = clazz.getDeclaredField("TYPE");
					if (field == null) {
						logger.error("No TYPE field {}", clazz.getName());
						throw new IEXUnexpectedError("No TYPE field");
					}
					if (!Modifier.isStatic(field.getModifiers())) {
						logger.error("TYPE field is not static {}", clazz.getName());
						throw new IEXUnexpectedError("TYPE field is not static");
					}
					String fieldTypeName = field.getType().getName();
					if (!fieldTypeName.equals("java.lang.String")) {
						logger.error("Unexpected fieldTypeName {}", fieldTypeName);
						throw new IEXUnexpectedError("Unexpected fieldTypeName");
					}
					Object value = field.get(null);
					if (value instanceof String) {
						type = (String)value;
					} else {
						logger.error("Unexpected value {}", value.getClass().getName());
						throw new IEXUnexpectedError("Unexpected value");
					}
				}
				
				String filter;
				{
					List<String>filterList = new ArrayList<>();
					for(IEXInfo.FieldInfo fiedlInfo: fieldInfos) {
						if (fiedlInfo.ignoreField) continue;
						filterList.add(fiedlInfo.jsonName);
					}
					filter = String.join(",", filterList.toArray(new String[0]));
				}

				this.clazzName  = clazz.getName();
				this.type       = type;
				this.fieldInfos = fieldInfos;
				this.fieldSize  = fieldSize;
				this.filter     = filter;
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
		
		@Override
		public String toString() {
			return String.format("%s %s %s", clazzName, Arrays.asList(this.fieldInfos));
		}
	}

	@Override
	public String toString() {
		try {
			IEXInfo iexInfo = IEXInfo.get(this);

			List<String>  result = new ArrayList<>();
			StringBuilder line   = new StringBuilder();
			
			Object o = this;
			for(IEXInfo.FieldInfo fieldInfo: iexInfo.fieldInfos) {
				line.setLength(0);
				line.append(fieldInfo.name).append(": ");
				
				switch(fieldInfo.type) {
				case "double":
					line.append(Double.toString(fieldInfo.field.getDouble(o)));
					break;
				case "float":
					line.append(fieldInfo.field.getFloat(o));
					break;
				case "long":
					line.append(fieldInfo.field.getLong(o));
					break;
				case "int":
					line.append(fieldInfo.field.getInt(o));
					break;
				case "short":
					line.append(fieldInfo.field.getShort(o));
					break;
				case "byte":
					line.append(fieldInfo.field.getByte(o));
					break;
				case "char":
					line.append(String.format("'%c'", fieldInfo.field.getChar(o)));
					break;
				default:
				{
					Object value = fieldInfo.field.get(o);
					if (value == null) {
						line.append("null");
					} else if (value instanceof String) {
						// Quote special character in string \ => \\  " => \"
						String stringValue = value.toString().replace("\\", "\\\\").replace("\"", "\\\"");
						line.append("\"").append(stringValue).append("\"");
					} else if (fieldInfo.isArray) {
						List<String> arrayElement = new ArrayList<>();
						int length = Array.getLength(value);
						for(int i = 0; i < length; i++) {
							Object element = Array.get(value, i);
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
			IEXInfo iexInfo = IEXInfo.get(this);
			for(IEXInfo.FieldInfo fieldInfo: iexInfo.fieldInfos) {
				// Skip field if name is not exist in jsonObject
				if (!jsonObject.containsKey(fieldInfo.jsonName))continue;
				
				ValueType valueType = jsonObject.get(fieldInfo.jsonName).getValueType();
				
//				logger.debug("parse {} {} {}", name, valueType.toString(), type);
				
				switch(valueType) {
				case NUMBER:
				{
					JsonNumber jsonNumber = jsonObject.getJsonNumber(fieldInfo.jsonName);
					
					switch(fieldInfo.type) {
					case "double":
						fieldInfo.field.set(this, jsonNumber.doubleValue());
						break;
					case "long":
						fieldInfo.field.set(this, jsonNumber.longValue());
						break;
					case "java.math.BigDecimal":
						fieldInfo.field.set(this, jsonNumber.bigDecimalValue());
						break;
					case "java.lang.String":
						// To handle irregular case in Symbols, add this code. Value of iexId in Symbols can be number or String.
						fieldInfo.field.set(this, jsonNumber.toString());
						break;
					case "java.time.LocalDateTime":
						fieldInfo.field.set(this, LocalDateTime.ofInstant(Instant.ofEpochMilli(jsonNumber.longValue()), ZONE_ID));
						break;
					default:
						logger.error("Unexptected type {} {}", valueType.toString(), fieldInfo.toString());
						throw new IEXUnexpectedError("Unexptected type");
					}
				}
					break;
				case STRING:
				{
					JsonString jsonString = jsonObject.getJsonString(fieldInfo.jsonName);
					switch(fieldInfo.type) {
					case "java.lang.String":
						fieldInfo.field.set(this, jsonString.getString());
						break;
					case "double":
						fieldInfo.field.set(this, Double.valueOf((jsonString.getString().length() == 0) ? "0" : jsonString.getString()));
						break;
					default:
						logger.error("Unexptected type {} {}", valueType.toString(), fieldInfo.toString());
						throw new IEXUnexpectedError("Unexptected type");
					}
				}
					break;
				case TRUE:
				{
					switch(fieldInfo.type) {
					case "boolean":
						fieldInfo.field.set(this, true);
						break;
					default:
						logger.error("Unexptected type {} {}", valueType.toString(), fieldInfo.toString());
						throw new IEXUnexpectedError("Unexptected type");
					}
				}
					break;
				case FALSE:
				{
					switch(fieldInfo.type) {
					case "boolean":
						fieldInfo.field.set(this, false);
						break;
					default:
						logger.error("Unexptected type {} {}", valueType.toString(), fieldInfo.toString());
						throw new IEXUnexpectedError("Unexptected type");
					}
				}
					break;
				case NULL:
				{
					switch(fieldInfo.type) {
					case "double":
						fieldInfo.field.set(this, 0);
						break;
					case "long":
						fieldInfo.field.set(this, 0);
						break;
					case "java.time.LocalDateTime":
						fieldInfo.field.set(this, NULL_LOCAL_DATE_TIME);
						break;
					case "java.lang.String":
						fieldInfo.field.set(this, "");
						break;
					default:
						logger.error("Unexptected type {} {}", valueType.toString(), fieldInfo.toString());
						throw new IEXUnexpectedError("Unexptected type");
					}
				}
					break;
				case OBJECT:
				{
					Class<?> fieldType = fieldInfo.field.getType();
					
					if (IEXBase.class.isAssignableFrom(fieldType)) {
						JsonObject childJson = jsonObject.get(fieldInfo.jsonName).asJsonObject();
//						logger.info("childJson {}", childJson.toString());
						
						IEXBase child = (IEXBase)fieldType.getDeclaredConstructor(JsonObject.class).newInstance(childJson);
//						logger.info("child {}", child.toString());
						
						fieldInfo.field.set(this, child);
					} else {
						logger.error("Unexptected type {} {}", valueType.toString(), fieldInfo.toString());
						throw new IEXUnexpectedError("Unexptected type");
					}
				}
					break;
				case ARRAY:
				{					
					if (fieldInfo.isArray) {
						JsonArray childJson = jsonObject.get(fieldInfo.jsonName).asJsonArray();

						Class<?> componentType = fieldInfo.field.getType().getComponentType();
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
									logger.error("Unexpected json array element type {} {}", childJsonValue.getValueType().toString(), fieldInfo.toString());
									throw new IEXUnexpectedError("Unexpected json array element type");
								}
							}
							fieldInfo.field.set(this, value);
						}
							break;
						default:
							logger.error("Unexpected array component type {} {}", componentTypeName, fieldInfo.toString());
							throw new IEXUnexpectedError("Unexpected array component type");
						}
					} else {
						logger.error("Unexptected field is not Array {}", fieldInfo.toString());
						throw new IEXUnexpectedError("Unexptected field is not Array");
					}
				}
					break;
				default:
					logger.error("Unknown valueType {} {}", valueType.toString(), fieldInfo.toString());
					throw new IEXUnexpectedError("Unknown valueType");
				}
			}
			
			// Assign default value, if field value is null)
			for(IEXInfo.FieldInfo fieldInfo: iexInfo.fieldInfos) {
				Object o = fieldInfo.field.get(this);
				// If field is null, assign default value
				if (o == null) {
					switch(fieldInfo.type) {
					case "double":
						if (!fieldInfo.ignoreField) {
							logger.warn("Assign defautl value  {} {} {}", iexInfo.clazzName, fieldInfo.name, fieldInfo.type);
						}
						fieldInfo.field.setDouble(this, 0);
						break;
					case "java.lang.String":
						if (!fieldInfo.ignoreField) {
							logger.warn("Assign defautl value  {} {} {}", iexInfo.clazzName, fieldInfo.name, fieldInfo.type);
						}
						fieldInfo.field.set(this, "");
						break;
					case "java.time.LocalDateTime":
						if (!fieldInfo.ignoreField) {
							logger.warn("Assign defautl value  {} {} {}", iexInfo.clazzName, fieldInfo.name, fieldInfo.type);
						}
						fieldInfo.field.set(this, NULL_LOCAL_DATE_TIME);
						break;
					default:
						logger.error("Unexpected field type {} {}", iexInfo.clazzName, fieldInfo.toString());
						throw new IEXUnexpectedError("Unexpected field type");
					}
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
		
		IEXInfo iexInfo = IEXInfo.get(clazz);
		
		String url = String.format("%s/stock/market/batch?types=%s&symbols=%s&filter=%s", END_POINT, iexInfo.type, String.join(",", encodeSymbol(symbols)), iexInfo.filter);
		String jsonString = HttpUtil.downloadAsString(url);
		if (jsonString == null) {
			logger.error("jsonString == null");
			throw new IEXUnexpectedError("jsonString == null");
		}
		
		// {"IBM":{"ohlc":{"open":{"price":146.89,"time":1532698210193},"close":{"price":145.15,"time":1532721693191},"high":147.14,"low":144.66}},"BT":{"ohlc":{"open":{"price":15.44,"time":1532698504567},"close":{"price":15.48,"time":1532721721103},"high":15.75,"low":15.405}}}
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
				if (!elementKey.equals(iexInfo.type)) {
					logger.error("Unexpected elementKey {} {} {}", resultKey, iexInfo.type, elementKey);
					throw new IEXUnexpectedError("Unexpected elementKey");
				}
				switch(elementValueType) {
				case OBJECT:
				{
					JsonObject arg = elementValue.asJsonObject();
					// Sanity check
					if (iexInfo.fieldSize != arg.size()) {
						logger.warn("Unexpected resultChild {} {} {}", resultKey, iexInfo.fieldSize, arg.size());
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
		
		IEXInfo iexInfo = IEXInfo.get(clazz);
		
		String url = String.format("%s/stock/market/batch?types=%s&symbols=%s", END_POINT, iexInfo.type, String.join(",", encodeSymbol(symbols)));
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
				if (!elementKey.equals(iexInfo.type)) {
					logger.error("Unexpected elementKey {} {}", iexInfo.type, elementKey);
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
		
		IEXInfo iexInfo = IEXInfo.get(clazz);
		
		String url = String.format("%s/stock/market/batch?types=%s&symbols=%s&filter=%s&range=%s", END_POINT, iexInfo.type, String.join(",", encodeSymbol(symbols)), iexInfo.filter, range.value);
		String jsonString = HttpUtil.downloadAsString(url);
		if (jsonString == null) {
			logger.error("jsonString == null");
			throw new IEXUnexpectedError("jsonString == null");
		}
		
		// {"IBM":{"dividends":[{"exDate":"2017-05-08","paymentDate":"2017-06-10","recordDate":"2017-05-10","declaredDate":"2017-04-25","amount":1.5,"flag":"","type":"Dividend income","qualified":"Q","indicated":""}]}}
		try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
			Map<String, E[]> ret = new TreeMap<>();

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
				if (!elementKey.equals(iexInfo.type)) {
					logger.error("Unexpected elementKey {} {}", iexInfo.type, elementKey);
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
						childArray[i]  = clazz.getDeclaredConstructor(JsonObject.class).newInstance(arg);
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
	
	public static <E extends IEXBase> void updateCSV(Class<E> clazz) {
		List<String> symbolList = UpdateSymbols.getSymbolList();
		int symbolListSize = symbolList.size();
		logger.info("symbolList {}", symbolList.size());
		
		List<E> dataList = new ArrayList<>();
		for(int i = 0; i < symbolListSize; i += IEXBase.MAX_PARAM) {
			int fromIndex = i;
			int toIndex = Math.min(fromIndex + IEXBase.MAX_PARAM, symbolListSize);
			List<String> getList = symbolList.subList(fromIndex, toIndex);
			if (getList.isEmpty()) continue;
			if (getList.size() == 1) {
				logger.info("  {}", String.format("%4d  %3d %-7s", fromIndex, getList.size(), getList.get(0)));
			} else {
				logger.info("  {}", String.format("%4d  %3d %-7s - %-7s", fromIndex, getList.size(), getList.get(0), getList.get(getList.size() - 1)));
			}
			
			Map<String, E> dataMap = getStockObject(clazz, getList.toArray(new String[0]));
			dataMap.values().stream().forEach(o -> dataList.add(o));
		}
		logger.info("dataList {}", dataList.size());
		
		String csvPath = getCSVPath(clazz);
		logger.info("csvPath {}", csvPath);
		
		CSVUtil.saveWithHeader(dataList, csvPath);
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
