package yokwe.finance.securities.iex;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.LocalDateTime;
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
import yokwe.finance.securities.eod.Market;

public class JSONUtil {
	private static final Logger logger = LoggerFactory.getLogger(JSONUtil.class);
	
	public static LocalDateTime getMarketDateTime(long t) {
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(t), Market.ZONE_ID);
	}
	public static final LocalDateTime DEFAULT_DATETIME = LocalDateTime.ofInstant(Instant.ofEpochMilli(0), Market.ZONE_ID);
	
	private static class ClassInfo {
		final String   clazzName;
		final Field[]  fields;
		final String[] types;
		final String[] names;
		final int      size;
		
		ClassInfo(String clazzName, Field[] fields, String[] types, String[] names, int size) {
			this.clazzName = clazzName;
			this.fields    = fields;
			this.types     = types;
			this.names     = names;
			this.size      = size;
		}
		
		@Override
		public String toString() {
			List<String> typeList  = Arrays.asList(types);
			List<String> nameList  = Arrays.asList(names);
			
			return String.format("%s %s %s", clazzName, nameList, typeList);
		}
	}
	private static Map<String, ClassInfo> classInfoMap = new TreeMap<>();
	
	public static <E> E parse(JsonObject jsonObject, Class<E> clazz) {
		try {
			String clazzName = clazz.getName();

			ClassInfo classInfo;
			if (classInfoMap.containsKey(clazzName)) {
				classInfo = classInfoMap.get(clazzName);
			} else {
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
				
				Field[] fields = fieldList.toArray(new Field[0]);
				String[] types  = typeList.toArray(new String[0]);
				String[] names  = nameList.toArray(new String[0]);
				int size   = fieldList.size();
				
				classInfo = new ClassInfo(clazzName, fields, types, names, size);
				classInfoMap.put(clazzName, classInfo);
			}

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
			
			E ret = clazz.newInstance();
			for(int i = 0; i < classInfo.size; i++) {
				String name  = classInfo.names[i];
				String type  = classInfo.types[i];
				Field  field = classInfo.fields[i];
				
				ValueType valueType = jsonObject.get(name).getValueType();
				
				logger.error("parse {} {} {}", name, valueType.toString(), type);
				
				switch(valueType) {
				case NUMBER:
				{
					JsonNumber jsonNumber = jsonObject.getJsonNumber(name);
					
					switch(type) {
					case "double":
					{
						double value = jsonNumber.doubleValue();
						field.set(ret, value);
					}
					case "long":
					{
						long value = jsonNumber.longValue();
						field.set(ret, value);
					}
						break;
					case "java.time.LocalDateTime":
					{
						LocalDateTime value = LocalDateTime.ofInstant(Instant.ofEpochMilli(jsonNumber.longValue()), Market.ZONE_ID);
						field.set(ret, value);
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
						field.set(ret, value);
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
						field.set(ret, Double.NaN);
						break;
					case "long":
						field.set(ret, 0);
						break;
					case "java.time.LocalDateTime":
						field.set(ret, null);
						break;
					default:
						logger.error("Unexptected type {} {} {}", name, valueType.toString(), type);
						throw new SecuritiesException("Unexptected type");
					}
				}
					break;
				default:
					logger.error("Unexptected type {} {} {}", name, valueType.toString(), type);
					throw new SecuritiesException("Unexptected type");
				}
			}
			
			return ret;
		} catch (InstantiationException e) {
			logger.error("InstantiationException {}", e.toString());
			throw new SecuritiesException("InstantiationException");
		} catch (IllegalAccessException e) {
			logger.error("IllegalAccessException {}", e.toString());
			throw new SecuritiesException("IllegalAccessException");
		}
	}
	
	public static String toString(Object o) {
		try {
			Class<? extends Object> clazz = o.getClass();
			String clazzName = o.getClass().getName();
			
			logger.info("o {}", o.getClass().getName());

			ClassInfo classInfo;
			if (classInfoMap.containsKey(clazzName)) {
				classInfo = classInfoMap.get(clazzName);
			} else {
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
				
				Field[] fields = fieldList.toArray(new Field[0]);
				String[] types  = typeList.toArray(new String[0]);
				String[] names  = nameList.toArray(new String[0]);
				int size   = fieldList.size();
				
				classInfo = new ClassInfo(clazzName, fields, types, names, size);
				classInfoMap.put(clazzName, classInfo);
			}

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
}
