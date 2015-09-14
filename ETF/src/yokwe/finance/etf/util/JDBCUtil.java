package yokwe.finance.etf.util;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.etf.ETFException;
import yokwe.finance.etf.Scrape;

public class JDBCUtil {
	static final Logger logger = LoggerFactory.getLogger(JDBCUtil.class);

	public static class ColumnInfo<E> {
		public final String   name;
		public final String   typeName;
		public final int      sqlType;
		public final Field    field;
		public final Class<E> clazz;
		public final int      columnIndex;
		
		private ColumnInfo(String name, String typeName, int sqlType, Field field, Class<E> clazz, int columnIndex) {
			this.name = name;
			this.typeName = typeName;
			this.sqlType = sqlType;
			this.field = field;
			this.clazz = clazz;
			this.columnIndex = columnIndex;
		}
	}
	
	public static <E> List<E> getResultAll(Statement statement, String sql, Class<E> clazz) {
		try {
			ResultSet resultSet = statement.executeQuery(sql);
			return getResultAll(resultSet, clazz);
		} catch (SQLException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new ETFException();
		}
	}

	
	public static <E> List<E> getResultAll(ResultSet resultSet, Class<E> clazz) {
		List<ColumnInfo<E>> columnInfoList = getColumnInfoList(resultSet, clazz);

		try {
			List<E> ret = new ArrayList<>();
			for(;;) {
				if (!resultSet.next()) break;
				ret.add(getInstance(resultSet, columnInfoList));
			}
			return ret;
		} catch (SQLException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new ETFException();
		}
	}
	
	public static <E> E getInstance(ResultSet resultSet, List<ColumnInfo<E>> columnInfoList) {
		try {
			E ret = columnInfoList.get(0).clazz.newInstance();
			
			for(ColumnInfo<E> columnInfo: columnInfoList) {
				final String name = columnInfo.name;
				final String typeName = columnInfo.typeName;
				final int sqlType = columnInfo.sqlType;
				final int columnIndex = columnInfo.columnIndex;
				final String stringValue = resultSet.getString(columnIndex);
				
				Class<?> type = columnInfo.field.getType();
				
				switch(sqlType) {
				case Types.VARCHAR:
					if (!type.equals(java.lang.String.class)) {
						logger.debug("XXX {}", type.toString());
						String message = String.format("Unexpected type %s %s %d", name, typeName, sqlType);
						logger.error(message);
						throw new ETFException(message);
					}
					{
						String value = Scrape.isValid(stringValue) ? stringValue : null;
						columnInfo.field.set(ret, value);
					}
					break;
				case Types.INTEGER:
					if (!type.equals(java.lang.Integer.TYPE)) {
						String message = String.format("Unexpected type %s %s %d", name, typeName, sqlType);
						logger.error(message);
						throw new ETFException(message);
					}
					{
						int value = Scrape.isValid(stringValue) ? resultSet.getInt(columnIndex) : -1;
						columnInfo.field.setInt(ret, value);
					}
					break;
				case Types.REAL:
					if (!type.equals(java.lang.Double.TYPE)) {
						String message = String.format("Unexpected type %s %s %d", name, typeName, sqlType);
						logger.error(message);
						throw new ETFException(message);
					}
					{
						double value = Scrape.isValid(stringValue) ? resultSet.getDouble(columnIndex) : -1;
						columnInfo.field.setDouble(ret, value);
					}
					break;
				default:
					String message = String.format("Unknown sqlType %d", sqlType);
					logger.error(message);
					throw new ETFException(message);
				}
			}
			return ret;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | SQLException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new ETFException();
		}
	}
	
	public static <E> List<ColumnInfo<E>> getColumnInfoList(ResultSet resultSet, Class<E> clazz) {
		List<ColumnInfo<E>> ret = new ArrayList<>();
		
		try {
			Field[] fields = clazz.getDeclaredFields();
			ResultSetMetaData metaData = resultSet.getMetaData();
			
			Map<String, Integer> sqlTypeMap = new TreeMap<>();
			Map<String, Integer> sqlColumnIndexMap = new TreeMap<>();
			final int columnCount = metaData.getColumnCount();
			for(int i = 1; i <= columnCount; i++) {
				String name = metaData.getColumnName(i);
				int type = metaData.getColumnType(i);
				sqlTypeMap.put(name, type);
				
				sqlColumnIndexMap.put(name, i);
//				logger.debug("sqlType {} {} {}", name, type, metaData.getColumnTypeName(i));
			}
			
			for(Field field: fields) {
				String name = field.getName();
				Class<?> type = field.getType();
				String typeName = type.getName();
				
				Integer sqlType = sqlTypeMap.get(name);
				if (sqlType == null) {
					String message = String.format("Unknown field %s!", name);
					logger.error(message);
					throw new ETFException(message);
				}
				Integer columnIndex = sqlColumnIndexMap.get(name);
				if (columnIndex == null) {
					String message = String.format("Unknown field %s!", name);
					logger.error(message);
					throw new ETFException(message);
				}
				
				switch(sqlType) {
				case Types.VARCHAR:
					if (!type.equals(java.lang.String.class)) {
						logger.debug("XXX {}", type.toString());
						String message = String.format("Unexpected type %s %s %d", name, typeName, sqlType);
						logger.error(message);
						throw new ETFException(message);
					}
					break;
				case Types.INTEGER:
					if (!type.equals(java.lang.Integer.TYPE)) {
						String message = String.format("Unexpected type %s %s %d", name, typeName, sqlType);
						logger.error(message);
						throw new ETFException(message);
					}
					break;
				case Types.REAL:
					if (!type.equals(java.lang.Double.TYPE)) {
						String message = String.format("Unexpected type %s %s %d", name, typeName, sqlType);
						logger.error(message);
						throw new ETFException(message);
					}
					break;
				default:
					String message = String.format("Unknown sqlType %d", sqlType);
					logger.error(message);
					throw new ETFException(message);
				}
				
				ret.add(new ColumnInfo<>(name, typeName, sqlType, field, clazz, columnIndex));
//				logger.debug("field {} {} {}", name, typeName, sqlType);
			}
		} catch (SQLException | SecurityException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new ETFException();
		}
		return ret;
	}
}