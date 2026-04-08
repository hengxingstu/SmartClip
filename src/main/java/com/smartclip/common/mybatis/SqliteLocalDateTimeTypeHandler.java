package com.smartclip.common.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(LocalDateTime.class)
@MappedJdbcTypes(value = JdbcType.VARCHAR, includeNullJdbcType = true)
/**
 * SQLite LocalDateTime 类型处理器，在 Java 时间对象和 ISO-8601 文本之间转换。
 */
public class SqliteLocalDateTimeTypeHandler extends BaseTypeHandler<LocalDateTime> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    /**
     * 将非空 LocalDateTime 写入 SQLite 文本字段。
     */
    public void setNonNullParameter(PreparedStatement ps, int i, LocalDateTime parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, FORMATTER.format(parameter));
    }

    @Override
    /**
     * 按列名读取 SQLite 文本时间并转换为 LocalDateTime。
     */
    public LocalDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    /**
     * 按列序号读取 SQLite 文本时间并转换为 LocalDateTime。
     */
    public LocalDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    /**
     * 从存储过程结果中读取文本时间；保留该实现以满足 MyBatis TypeHandler 契约。
     */
    public LocalDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private LocalDateTime parse(String value) throws SQLException {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.replace(' ', 'T'), FORMATTER);
        } catch (RuntimeException exception) {
            throw new SQLException("Cannot parse SQLite LocalDateTime value: " + value, exception);
        }
    }

    @Override
    /**
     * 兼容空值写入，避免 SQLite 文本字段收到不明确的 JDBC 类型。
     */
    public void setParameter(PreparedStatement ps, int i, LocalDateTime parameter, JdbcType jdbcType)
            throws SQLException {
        if (parameter == null) {
            ps.setNull(i, Types.VARCHAR);
        } else {
            setNonNullParameter(ps, i, parameter, jdbcType);
        }
    }
}
