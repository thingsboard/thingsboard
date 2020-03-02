package org.thingsboard.server.service.install.migrate;

import com.datastax.driver.core.Row;
import lombok.Data;
import org.thingsboard.server.common.data.UUIDConverter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

@Data
public class CassandraToSqlColumn {

    private String cassandraColumnName;
    private String sqlColumnName;
    private CassandraToSqlColumnType type;
    private int sqlType;
    private Class<? extends Enum> enumClass;

    public static CassandraToSqlColumn idColumn(String name) {
        return new CassandraToSqlColumn(name, CassandraToSqlColumnType.ID);
    }

    public static CassandraToSqlColumn stringColumn(String name) {
        return new CassandraToSqlColumn(name, CassandraToSqlColumnType.STRING);
    }

    public static CassandraToSqlColumn stringColumn(String cassandraColumnName, String sqlColumnName) {
        return new CassandraToSqlColumn(cassandraColumnName, sqlColumnName);
    }

    public static CassandraToSqlColumn bigintColumn(String name) {
        return new CassandraToSqlColumn(name, CassandraToSqlColumnType.BIGINT);
    }

    public static CassandraToSqlColumn doubleColumn(String name) {
        return new CassandraToSqlColumn(name, CassandraToSqlColumnType.DOUBLE);
    }

    public static CassandraToSqlColumn booleanColumn(String name) {
        return new CassandraToSqlColumn(name, CassandraToSqlColumnType.BOOLEAN);
    }

    public static CassandraToSqlColumn enumToIntColumn(String name, Class<? extends Enum> enumClass) {
        return new CassandraToSqlColumn(name, CassandraToSqlColumnType.ENUM_TO_INT, enumClass);
    }

    public CassandraToSqlColumn(String columnName) {
        this(columnName, columnName, CassandraToSqlColumnType.STRING, null);
    }

    public CassandraToSqlColumn(String columnName, CassandraToSqlColumnType type) {
        this(columnName, columnName, type, null);
    }

    public CassandraToSqlColumn(String columnName, CassandraToSqlColumnType type, Class<? extends Enum> enumClass) {
        this(columnName, columnName, type, enumClass);
    }

    public CassandraToSqlColumn(String cassandraColumnName, String sqlColumnName) {
        this(cassandraColumnName, sqlColumnName, CassandraToSqlColumnType.STRING, null);
    }

    public CassandraToSqlColumn(String cassandraColumnName, String sqlColumnName, CassandraToSqlColumnType type,
                                Class<? extends Enum> enumClass) {
        this.cassandraColumnName = cassandraColumnName;
        this.sqlColumnName = sqlColumnName;
        this.type = type;
        this.enumClass = enumClass;
        switch (this.type) {
            case ID:
            case STRING:
                this.sqlType = Types.VARCHAR;
                break;
            case DOUBLE:
                this.sqlType = Types.DOUBLE;
                break;
            case INTEGER:
            case ENUM_TO_INT:
                this.sqlType = Types.INTEGER;
                break;
            case FLOAT:
                this.sqlType = Types.FLOAT;
                break;
            case BIGINT:
                this.sqlType = Types.BIGINT;
                break;
            case BOOLEAN:
                this.sqlType = Types.BOOLEAN;
                break;
        }
    }

    public void prepareColumnValue(Row row, PreparedStatement sqlInsertStatement, int index) throws SQLException {
        String value = this.getColumnValue(row, index);
        this.setColumnValue(sqlInsertStatement, index, value);
    }

    private String getColumnValue(Row row, int index) {
        if (row.isNull(index)) {
            return null;
        } else {
            switch (this.type) {
                case ID:
                    return UUIDConverter.fromTimeUUID(row.getUUID(index));
                case DOUBLE:
                    return Double.toString(row.getDouble(index));
                case INTEGER:
                    return Integer.toString(row.getInt(index));
                case FLOAT:
                    return Float.toString(row.getFloat(index));
                case BIGINT:
                    return Long.toString(row.getLong(index));
                case BOOLEAN:
                    return Boolean.toString(row.getBool(index));
                case STRING:
                case ENUM_TO_INT:
                default:
                   return row.getString(index);
            }
        }
    }

    private void setColumnValue(PreparedStatement sqlInsertStatement, int index, String value) throws SQLException {
        if (value == null) {
            sqlInsertStatement.setNull(index, this.sqlType);
        } else {
            switch (this.type) {
                case DOUBLE:
                    sqlInsertStatement.setDouble(index, Double.parseDouble(value));
                    break;
                case INTEGER:
                    sqlInsertStatement.setInt(index, Integer.parseInt(value));
                    break;
                case FLOAT:
                    sqlInsertStatement.setFloat(index, Float.parseFloat(value));
                    break;
                case BIGINT:
                    sqlInsertStatement.setLong(index, Long.parseLong(value));
                    break;
                case BOOLEAN:
                    sqlInsertStatement.setBoolean(index, Boolean.parseBoolean(value));
                    break;
                case ENUM_TO_INT:
                    Enum enumVal = Enum.valueOf(this.enumClass, value);
                    int intValue = enumVal.ordinal();
                    sqlInsertStatement.setInt(index, intValue);
                    break;
                case STRING:
                case ID:
                default:
                    sqlInsertStatement.setString(index, value);
                    break;
            }
        }
    }

}

