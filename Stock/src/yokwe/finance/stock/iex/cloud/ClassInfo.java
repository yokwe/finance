package yokwe.finance.stock.iex.cloud;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.stock.iex.cloud.IEXCloud.IgnoreField;
import yokwe.finance.stock.iex.cloud.IEXCloud.JSONName;


public class ClassInfo {
	static final Logger logger = LoggerFactory.getLogger(ClassInfo.class);

	private static Map<String, ClassInfo> map = new TreeMap<>();
	
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
	
	public static ClassInfo get(Base o) {
		return get(o.getClass());
	}
	public static ClassInfo get(Class<? extends Base> clazz) {
		String key = clazz.getName();
		
		if (map.containsKey(key)) return map.get(key);
		
		ClassInfo value = new ClassInfo(clazz);
		map.put(key, value);
		return value;
	}

	public final String      clazzName;
	public final String      method;
	public final String      path;
	public final FieldInfo[] fieldInfos;
	public final int         fieldSize;
	public final String      filter;
	
	private static Field getField(Class<?> clazz, String fieldName) {
		try {
			return clazz.getDeclaredField(fieldName);
		} catch (NoSuchFieldException e) {
			return null;
		}
	}
	
	ClassInfo(Class<? extends Base> clazz) {
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
			
			String method;
			{
				Field field = getField(clazz, "METHOD");
				if (field != null) {
					if (!Modifier.isStatic(field.getModifiers())) {
						logger.error("METHOD field is not static {}", clazz.getName());
						throw new UnexpectedError("METHOD field is not static");
					}
					String fieldTypeName = field.getType().getName();
					if (!fieldTypeName.equals("java.lang.String")) {
						logger.error("Unexpected fieldTypeName {}", fieldTypeName);
						throw new UnexpectedError("Unexpected fieldTypeName");
					}
					Object value = field.get(null);
					if (value instanceof String) {
						method = (String)value;
					} else {
						logger.error("Unexpected value {}", value.getClass().getName());
						throw new UnexpectedError("Unexpected value");
					}
				} else {
					method = null;
				}
			}

			String path;
			{
				Field field = getField(clazz, "PATH");
				if (field != null) {
					if (!Modifier.isStatic(field.getModifiers())) {
						logger.error("PATH field is not static {}", clazz.getName());
						throw new UnexpectedError("PATH field is not static");
					}
					String fieldTypeName = field.getType().getName();
					if (!fieldTypeName.equals("java.lang.String")) {
						logger.error("Unexpected fieldTypeName {}", fieldTypeName);
						throw new UnexpectedError("Unexpected fieldTypeName");
					}
					Object value = field.get(null);
					if (value instanceof String) {
						path = (String)value;
					} else {
						logger.error("Unexpected value {}", value.getClass().getName());
						throw new UnexpectedError("Unexpected value");
					}
				} else {
					path = null;
				}
			}
			
			String filter;
			{
				List<String>filterList = new ArrayList<>();
				for(ClassInfo.FieldInfo fiedlInfo: fieldInfos) {
					if (fiedlInfo.ignoreField) continue;
					filterList.add(fiedlInfo.jsonName);
				}
				filter = String.join(",", filterList.toArray(new String[0]));
			}

			this.clazzName  = clazz.getName();
			this.method     = method;
			this.path       = path;
			this.fieldInfos = fieldInfos;
			this.fieldSize  = fieldSize;
			this.filter     = filter;
		} catch (SecurityException e) {
			logger.error("SecurityException {}", e.toString());
			throw new UnexpectedError("SecurityException");
		} catch (IllegalArgumentException e) {
			logger.error("IllegalArgumentException {}", e.toString());
			throw new UnexpectedError("IllegalArgumentException");
		} catch (IllegalAccessException e) {
			logger.error("IllegalAccessException {}", e.toString());
			throw new UnexpectedError("IllegalAccessException");
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s  %s  %s  %s", clazzName, method, path, Arrays.asList(this.fieldInfos));
	}
}
