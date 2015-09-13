package yokwe.finance.etf;

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

public class JdbcHelper<E> {
	static final Logger logger = LoggerFactory.getLogger(JdbcHelper.class);

	public final String   name;
	public final String   typeName;
	public final int      sqlType;
	public final Field    field;
	public final Class<E> clazz;
	public final int      columnIndex;
	
	private JdbcHelper(String name, String typeName, int sqlType, Field field, Class<E> clazz, int columnIndex) {
		this.name = name;
		this.typeName = typeName;
		this.sqlType = sqlType;
		this.field = field;
		this.clazz = clazz;
		this.columnIndex = columnIndex;
	}
	
	public static <E> List<E> getResultAll(Statement statement, String sql, Class<E> clazz) {
		ResultSet resultSet;
		try {
			resultSet = statement.executeQuery(sql);
			return getResultAll(resultSet, clazz);
		} catch (SQLException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new ETFException("");
		}
	}

	
	public static <E> List<E> getResultAll(ResultSet resultSet, Class<E> clazz) {
		List<E> ret = new ArrayList<>();
		List<JdbcHelper<E>> metaDataList = toList(resultSet, clazz);

		try {
			for(;;) {
				if (!resultSet.next()) break;
				ret.add(getInstance(resultSet, metaDataList));
			}
		} catch (SQLException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new ETFException("");
		}
		return ret;
	}
	
	
	public static <E> E getInstance(ResultSet resultSet, List<JdbcHelper<E>> metaDataList) {
		try {
			E ret = metaDataList.get(0).clazz.newInstance();
			
			for(JdbcHelper<E> metaData: metaDataList) {
				final String name = metaData.name;
				final String typeName = metaData.typeName;
				final int sqlType = metaData.sqlType;
				final int columnIndex = metaData.columnIndex;
				final String stringValue = resultSet.getString(columnIndex);
				
				Class<?> type = metaData.field.getType();
				
				switch(sqlType) {
				case Types.VARCHAR:
					if (!type.equals(java.lang.String.class)) {
						logger.debug("XXX {}", type.toString());
						String message = String.format("Unexpected type %s %s %d", name, typeName, sqlType);
						logger.error(message);
						throw new ETFException(message);
					}
					{
						String value = (stringValue.equals(Scrape.NO_VALUE)) ? null : stringValue;
						metaData.field.set(ret, value);
					}
					break;
				case Types.INTEGER:
					if (!type.equals(java.lang.Integer.TYPE)) {
						String message = String.format("Unexpected type %s %s %d", name, typeName, sqlType);
						logger.error(message);
						throw new ETFException(message);
					}
					{
						int value = (stringValue.equals(Scrape.NO_VALUE)) ? -1 : resultSet.getInt(columnIndex);
						metaData.field.setInt(ret, value);
					}
					break;
				case Types.REAL:
					if (!type.equals(java.lang.Double.TYPE)) {
						String message = String.format("Unexpected type %s %s %d", name, typeName, sqlType);
						logger.error(message);
						throw new ETFException(message);
					}
					{
						double value = (stringValue.equals(Scrape.NO_VALUE)) ? -1 : resultSet.getDouble(columnIndex);
						metaData.field.setDouble(ret, value);
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
			throw new ETFException("");
		}
	}
	
	public static <E> List<JdbcHelper<E>> toList(ResultSet resultSet, Class<E> clazz) {
		List<JdbcHelper<E>> ret = new ArrayList<>();
		
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
				logger.debug("sqlType {} {} {}", name, type, metaData.getColumnTypeName(i));
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
				
				ret.add(new JdbcHelper<>(name, typeName, sqlType, field, clazz, columnIndex));
				
				logger.debug("field {} {} {}", name, typeName, sqlType);
			}
		} catch (SQLException | SecurityException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new ETFException("");
		}
		return ret;
	}
}