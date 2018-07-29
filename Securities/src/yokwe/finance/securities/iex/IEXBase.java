package yokwe.finance.securities.iex;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue.ValueType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class IEXBase {
	private static final Logger logger = LoggerFactory.getLogger(IEXBase.class);

	// Use New York time in LocalDateTime
	public static final ZoneId ZONE_ID = ZoneId.of("America/New_York");
	
	public static final String END_POINT = "https://api.iextrading.com/1.0";

	public static class ClassInfo {
		private static Map<String, ClassInfo> map = new TreeMap<>();
		
		public static ClassInfo get(Object o) {
			Class<? extends Object> clazz = o.getClass();
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
		
		ClassInfo(String clazzName, Field[] fields, String[] types, String[] names, int size) {
			this.clazzName = clazzName;
			this.fields    = fields;
			this.types     = types;
			this.names     = names;
			this.size      = size;
		}
		
		ClassInfo(Class<? extends Object> clazz) {
			List<Field>  fieldList = new ArrayList<>();
			List<String> typeList  = new ArrayList<>();
			List<String> nameList  = new ArrayList<>();

			for(Field field: clazz.getDeclaredFields()) {
				// Skip static field
				if (Modifier.isStatic(field.getModifiers())) continue;

				fieldList.add(field);
				typeList.add(field.getType().getName());
				nameList.add(field.getName());
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

			StringBuilder ret = new StringBuilder();
			for(int i = 0; i < classInfo.size; i++) {
				String name  = classInfo.names[i];
				String type  = classInfo.types[i];
				Field  field = classInfo.fields[i];
				
				switch(type) {
				case "double":
				{
					double value = field.getDouble(o);
					ret.append(String.format(", %s: %s", name, Double.toString(value)));
				}
					break;
				case "float":
				{
					float value = field.getFloat(o);
					ret.append(String.format(", %s: %s", name, Float.toString(value)));
				}
					break;
				case "long":
				{
					long value = field.getLong(o);
					ret.append(String.format(", %s: %s", name, Long.toString(value)));
				}
					break;
				case "int":
				{
					int value = field.getInt(o);
					ret.append(String.format(", %s: %s", name, Integer.toString(value)));
				}
					break;
				case "short":
				{
					short value = field.getShort(o);
					ret.append(String.format(", %s: %s", name, Short.toString(value)));
				}
					break;
				case "byte":
				{
					byte value = field.getByte(o);
					ret.append(String.format(", %s: %s", name, Byte.toString(value)));
				}
					break;
				case "char":
				{
					char value = field.getChar(o);
					ret.append(String.format(", %s: %s", name, Character.toString(value)));
				}
					break;
				default:
				{
					Object value = field.get(o);
					if (value == null) {
						ret.append(String.format(", %s: null", name));
					} else if (value instanceof IEXBase) {
						ret.append(String.format(", %s: %s", name, value.toString()));
					} else {
						ret.append(String.format(", %s: \"%s\"", name, value.toString()));
					}
				}
					break;
				}
			}
			
			return String.format("{%s}", ret.substring(2));
		} catch (IllegalAccessException e) {
			logger.error("IllegalAccessException {}", e.toString());
			throw new SecuritiesException("IllegalAccessException");
		}
	}

	public IEXBase() {
		//
	}
	public IEXBase(JsonObject jsonObject) {
		try {
			ClassInfo classInfo = ClassInfo.get(this);
			{
				// Sanity check
				{
					Set<String> fieldNameSet = new TreeSet<>(Arrays.asList(classInfo.names));
					Set<String> jsonNameSet  = new TreeSet<>(jsonObject.keySet());
					
					Set<String> unionSet = new TreeSet<>();
					unionSet.addAll(fieldNameSet);
					unionSet.addAll(jsonNameSet);
					
					int fieldNameSetSize = fieldNameSet.size();
					int jsonNameSetSize  = jsonNameSet.size();
					int unionSetSize     = unionSet.size();
					
					if (fieldNameSetSize != unionSetSize || jsonNameSetSize != unionSetSize) {
						logger.error("fieldNameSet = ({}){}", fieldNameSetSize, fieldNameSet);
						logger.error("jsonNameSet  = ({}){}", jsonNameSetSize, jsonNameSet);
						throw new SecuritiesException("Unknonw field in jsonObject");
					}
				}
			}
			
			for(int i = 0; i < classInfo.size; i++) {
				String name  = classInfo.names[i];
				String type  = classInfo.types[i];
				Field  field = classInfo.fields[i];
				
				ValueType valueType = jsonObject.get(name).getValueType();
				
//				logger.debug("parse {} {} {}", name, valueType.toString(), type);
				
				switch(valueType) {
				case NUMBER:
				{
					JsonNumber jsonNumber = jsonObject.getJsonNumber(name);
					
					switch(type) {
					case "double":
					{
						double value = jsonNumber.doubleValue();
						field.set(this, value);
					}
					case "long":
					{
						long value = jsonNumber.longValue();
						field.set(this, value);
					}
						break;
					case "java.time.LocalDateTime":
					{
						LocalDateTime value = LocalDateTime.ofInstant(Instant.ofEpochMilli(jsonNumber.longValue()), ZONE_ID);
						field.set(this, value);
					}
						break;
					default:
						logger.error("Unexptected type {} {} {}", name, valueType.toString(), type);
						throw new SecuritiesException("Unexptected type");
					}
				}
					break;
				case STRING:
				{
					JsonString jsonString = jsonObject.getJsonString(name);
					switch(type) {
					case "java.lang.String":
					{
						String value = jsonString.getString();
						field.set(this, value);
					}
						break;
					default:
						logger.error("Unexptected type {} {} {}", name, valueType.toString(), type);
						throw new SecuritiesException("Unexptected type");
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
						field.set(this, null);
						break;
					default:
						logger.error("Unexptected type {} {} {}", name, valueType.toString(), type);
						throw new SecuritiesException("Unexptected type");
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
						logger.error("fieldType {}", fieldType.getName());
						throw new SecuritiesException("Unexptected type");
					}
				}
					break;
				default:
					logger.error("Unexptected type {} {} {}", name, valueType.toString(), type);
					throw new SecuritiesException("Unexptected type");
				}
			}
		} catch (IllegalAccessException e) {
			logger.error("IllegalAccessException {}", e.toString());
			throw new SecuritiesException("IllegalAccessException");
		} catch (InstantiationException e) {
			logger.error("InstantiationException {}", e.toString());
			throw new SecuritiesException("InstantiationException");
		} catch (IllegalArgumentException e) {
			logger.error("IllegalArgumentException {}", e.toString());
			throw new SecuritiesException("IllegalArgumentException");
		} catch (InvocationTargetException e) {
			logger.error("InvocationTargetException {}", e.toString());
			throw new SecuritiesException("InvocationTargetException");
		} catch (NoSuchMethodException e) {
			logger.error("NoSuchMethodException {}", e.toString());
			throw new SecuritiesException("NoSuchMethodException");
		} catch (SecurityException e) {
			logger.error("SecurityException {}", e.toString());
			throw new SecuritiesException("SecurityException");
		}
	}
}
