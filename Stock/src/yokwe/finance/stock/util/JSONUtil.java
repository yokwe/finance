package yokwe.finance.stock.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.json.stream.JsonGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.stock.UnexpectedException;

public class JSONUtil {
	static final Logger logger = LoggerFactory.getLogger(JSONUtil.class);

	public enum Type {
		BOOLEAN, DOUBLE, FLOAT, INTEGER, LONG, STRING
	}
	
	public static class FieldInfo {
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
		
		private FieldInfo(Field field) {
			this.field = field;
			this.name  = field.getName();
			
			String typeName = field.getType().getName();
			if (typeMap.containsKey(typeName)) {
				type = typeMap.get(typeName);
			} else {
				logger.error("Unexpected {}", typeName);
				throw new UnexpectedException("Unexpected");
			}
		}
		
		@Override
		public String toString() {
			return String.format("{%-8s %s}", name, type);
		}
	}
	
	public static class ClassInfo {
		private static Map<String, ClassInfo> cache = new TreeMap<>();
		
		private static ClassInfo get(Class<?> clazz) {
			String className = clazz.getName();
			if (!cache.containsKey(className)) {
				cache.put(className, new ClassInfo(clazz));
			}
			return cache.get(className);
		}
		
		public final String                 className;
		//               field
		public final Map<String, FieldInfo> fieldMap;
		public final String[]               fields;
		
		private ClassInfo(Class<?> clazz) {
			className = clazz.getName();
			logger.info("ClassInfo {}", className);
			
			Map<String, FieldInfo> map = new TreeMap<>();
			for(Field field: clazz.getDeclaredFields()) {
				int modifier = field.getModifiers();
				// Skip static field
				if (Modifier.isStatic(modifier)) continue;
				
				FieldInfo fieldInfo = new FieldInfo(field);
				map.put(fieldInfo.name, fieldInfo);
			}
			fieldMap = Collections.unmodifiableMap(map);
			
			fields = fieldMap.keySet().toArray(new String[0]);
			
			for(FieldInfo fieldInfo: map.values()) {
				logger.info("  {}", fieldInfo.toString());
			}
		}
		
		public Type getType(String field) {
			if (fieldMap.containsKey(field)) {
				return fieldMap.get(field).type;
			}
			logger.error("Unexpected {} {}", className, field);
			throw new UnexpectedException("Unexpected");
		}
		public boolean containsField(String fieldName) {
			return fieldMap.containsKey(fieldName);
		}
		public FieldInfo get(String fieldName) {
			if (fieldMap.containsKey(fieldName)) {
				return fieldMap.get(fieldName);
			}
			logger.error("Unexpected {} {}", className, fieldName);
			throw new UnexpectedException("Unexpected");
		}
	}
	
	public static ClassInfo getClassInfo(Class<?> clazz) {
		return ClassInfo.get(clazz);
	}
	
	public static <E> void buildArray(JsonGenerator gen, String fieldName, List<E> dataList) {		
		gen.writeStartArray(fieldName);
		
		if (!dataList.isEmpty()) {
			ClassInfo classInfo;
			{
				Object o = dataList.get(0);
				classInfo = getClassInfo(o.getClass());
				
				if (!classInfo.containsField(fieldName)) {
					logger.error("Unknown field {} {}", o.getClass().getName(), fieldName);
					throw new UnexpectedException("Unknown field");
				}
			}
			
			FieldInfo fieldInfo = classInfo.get(fieldName);
			
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
							throw new UnexpectedException("Unexpected");
						}
					}
				}
			} catch (IllegalArgumentException e) {
				logger.error("IllegalArgumentException {}", e.getMessage());
				throw new UnexpectedException("IllegalArgumentException");
			} catch (IllegalAccessException e) {
				logger.error("IllegalAccessException {}", e.getMessage());
				throw new UnexpectedException("IllegalAccessException");
			}
		}
		gen.writeEnd();
	}

	public static <E> List<E> getLastElement(List<E> list, int count) {
		int listSize = list.size();
		return list.subList(Math.max(listSize - count, 0), listSize);
	}
}
