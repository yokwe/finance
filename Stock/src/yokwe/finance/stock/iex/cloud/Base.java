package yokwe.finance.stock.iex.cloud;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
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

import yokwe.finance.stock.iex.IEXUnexpectedError;
import yokwe.finance.stock.util.HttpUtil;

public class Base {
	static final Logger logger = LoggerFactory.getLogger(Base.class);
	
	// Use New York time in LocalDateTime
	public static final ZoneId ZONE_ID = ZoneId.of("America/New_York");
	
	public static final LocalDateTime NULL_LOCAL_DATE_TIME = LocalDateTime.ofInstant(Instant.EPOCH, ZONE_ID);


	@Override
	public String toString() {
		try {
			ClassInfo classInfo = ClassInfo.get(this);

			List<String>  result = new ArrayList<>();
			StringBuilder line   = new StringBuilder();
			
			Object o = this;
			for(ClassInfo.FieldInfo fieldInfo: classInfo.fieldInfos) {
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
			throw new UnexpectedError("IllegalAccessException");
		}
	}

	protected Base() {
		//
	}
	protected Base(JsonObject jsonObject) {
		try {
			ClassInfo iexInfo = ClassInfo.get(this);
			for(ClassInfo.FieldInfo fieldInfo: iexInfo.fieldInfos) {
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
						throw new UnexpectedError("Unexptected type");
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
					case "long":
						fieldInfo.field.set(this, Long.valueOf((jsonString.getString().length() == 0) ? "0" : jsonString.getString()));
						break;
					case "int":
						fieldInfo.field.set(this, Integer.valueOf((jsonString.getString().length() == 0) ? "0" : jsonString.getString()));
						break;
					default:
						logger.error("Unexptected type {} {}", valueType.toString(), fieldInfo.toString());
						throw new UnexpectedError("Unexptected type");
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
						throw new UnexpectedError("Unexptected type");
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
						throw new UnexpectedError("Unexptected type");
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
						throw new UnexpectedError("Unexptected type");
					}
				}
					break;
				case OBJECT:
				{
					Class<?> fieldType = fieldInfo.field.getType();
					
					if (Base.class.isAssignableFrom(fieldType)) {
						JsonObject childJson = jsonObject.get(fieldInfo.jsonName).asJsonObject();
//						logger.info("childJson {}", childJson.toString());
						
						Base child = (Base)fieldType.getDeclaredConstructor(JsonObject.class).newInstance(childJson);
//						logger.info("child {}", child.toString());
						
						fieldInfo.field.set(this, child);
					} else {
						String fieldTypeName = fieldType.getName();
						switch(fieldTypeName) {
						case "java.util.Map":
						{
							java.lang.reflect.Type type = fieldInfo.field.getGenericType();
							if (type instanceof ParameterizedType) {
								ParameterizedType parameterizedType = (ParameterizedType)type;
								
								java.lang.reflect.Type[] types = parameterizedType.getActualTypeArguments();
								if (types.length != 2) {
									logger.error("Unexptected types.length {}", types.length);
									throw new UnexpectedError("Unexpected types.length");
								}

								String keyTypeName   = types[0].getTypeName();
								String valueTypeName = types[1].getTypeName();
								
//								logger.info("keyTypeName   {}", keyTypeName);
//								logger.info("valueTypeName {}", valueTypeName);
								
								if (!keyTypeName.equals("java.lang.String")) {
									logger.error("Unexptected keyTypeName {}", keyTypeName);
									throw new UnexpectedError("Unexptected keyTypeName");
								}
								
								switch(valueTypeName) {
								case "java.lang.Long":
								{
									Map<String, Long> child = new TreeMap<>();
									JsonObject childJson = jsonObject.get(fieldInfo.jsonName).asJsonObject();
									for(String childKey: childJson.keySet()) {
										JsonValue childValue = childJson.get(childKey);
										ValueType childValueType = childValue.getValueType();
										
										switch(childValueType) {
										case NUMBER:
										{
											JsonNumber jsonNumber = childJson.getJsonNumber(childKey);
											long value = jsonNumber.longValue();
											child.put(childKey, value);
										}
											break;
										case STRING:
										{
											JsonString jsonString = childJson.getJsonString(childKey);
											long value = Long.valueOf((jsonString.getString().length() == 0) ? "0" : jsonString.getString());
											child.put(childKey, value);
										}
											break;
										default:
											logger.error("Unexptected childValueType {}", childValueType);
											throw new UnexpectedError("Unexptected childValueType");
										}
									}
									fieldInfo.field.set(this, child);
								}
									break;
								case "java.lang.Integer":
								{
									Map<String, Integer> child = new TreeMap<>();
									JsonObject childJson = jsonObject.get(fieldInfo.jsonName).asJsonObject();
									for(String childKey: childJson.keySet()) {
										JsonValue childValue = childJson.get(childKey);
										ValueType childValueType = childValue.getValueType();
										
										switch(childValueType) {
										case NUMBER:
										{
											JsonNumber jsonNumber = childJson.getJsonNumber(childKey);
											int value = jsonNumber.intValue();
											child.put(childKey, value);
										}
											break;
										case STRING:
										{
											JsonString jsonString = childJson.getJsonString(childKey);
											int value = Integer.valueOf((jsonString.getString().length() == 0) ? "0" : jsonString.getString());
											child.put(childKey, value);
										}
											break;
										default:
											logger.error("Unexptected childValueType {}", childValueType);
											throw new UnexpectedError("Unexptected childValueType");
										}
									}
									fieldInfo.field.set(this, child);
								}
									break;
								default:
									logger.error("Unexptected keyTypeName {}", keyTypeName);
									throw new UnexpectedError("Unexptected keyTypeName");
								}
							} else {
								throw new UnexpectedError("Unexptected");
							}
							
						}
							break;
						default:
							logger.error("Unexptected type {} {}", valueType.toString(), fieldInfo.toString());
							logger.error("fieldTypeName {}", fieldTypeName);
							throw new UnexpectedError("Unexptected type");
						}
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
									throw new UnexpectedError("Unexpected json array element type");
								}
							}
							fieldInfo.field.set(this, value);
						}
							break;
						default:
							logger.error("Unexpected array component type {} {}", componentTypeName, fieldInfo.toString());
							throw new UnexpectedError("Unexpected array component type");
						}
					} else {
						logger.error("Unexptected field is not Array {}", fieldInfo.toString());
						throw new UnexpectedError("Unexptected field is not Array");
					}
				}
					break;
				default:
					logger.error("Unknown valueType {} {}", valueType.toString(), fieldInfo.toString());
					throw new UnexpectedError("Unknown valueType");
				}
			}
			
			// Assign default value, if field value is null)
			for(ClassInfo.FieldInfo fieldInfo: iexInfo.fieldInfos) {
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
						throw new UnexpectedError("Unexpected field type");
					}
				}
			}

		} catch (IllegalAccessException e) {
			logger.error("IllegalAccessException {}", e.toString());
			throw new UnexpectedError("IllegalAccessException");
		} catch (InstantiationException e) {
			logger.error("InstantiationException {}", e.toString());
			throw new UnexpectedError("InstantiationException");
		} catch (IllegalArgumentException e) {
			logger.error("IllegalArgumentException {}", e.toString());
			throw new UnexpectedError("IllegalArgumentException");
		} catch (InvocationTargetException e) {
			logger.error("InvocationTargetException {}", e.toString());
			logger.error("getCause() {}", e.getCause());
			StackTraceElement[] list = e.getStackTrace();
			for(int i = 0; i < list.length; i++) {
				logger.info("XXX {}  {}", i, list[i]);
			}
			
			throw new UnexpectedError("InvocationTargetException");
		} catch (NoSuchMethodException e) {
			logger.error("NoSuchMethodException {}", e.toString());
			throw new UnexpectedError("NoSuchMethodException");
		} catch (SecurityException e) {
			logger.error("SecurityException {}", e.toString());
			throw new UnexpectedError("SecurityException");
		}
	}

	protected static String[] encodeSymbol(String[] symbols) {
		try {
			String[] ret = new String[symbols.length];
			for(int i = 0; i < symbols.length; i++) {
				ret[i] = URLEncoder.encode(symbols[i], "UTF-8");
			}
			return ret;
		} catch (UnsupportedEncodingException e) {
			logger.error("UnsupportedEncodingException {}", e.toString());
			throw new UnexpectedError("UnsupportedEncodingException");
		}
	}

	// for Status
	protected static <E extends Base> E getObject(Context context, Class<E> clazz) {
		ClassInfo classInfo = ClassInfo.get(clazz);
		
		// 'https://cloud.iexapis.com/v1/status?token=sk_bb977734bffe47ef8dca20cd4cfad878'
		if (classInfo.method == null) {
			logger.error("method == null {}", classInfo);
			throw new UnexpectedError("method == null");
		}
		String url = context.getURL(classInfo.method);
		logger.info("url = {}", url);
		
		String jsonString = HttpUtil.downloadAsString(url);
		if (jsonString == null) {
			logger.error("jsonString == null");
			throw new UnexpectedError("jsonString == null");
		}
		logger.info("jsonString = {}", jsonString);
		
		try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
			// Assume result is only one object
			JsonObject arg = reader.readObject();
			E ret = clazz.getDeclaredConstructor(JsonObject.class).newInstance(arg);
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

}
