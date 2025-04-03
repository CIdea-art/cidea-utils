package cn.cidea.core.dal.ibatis.type;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Collection;

/**
 * {@link Duration#parse(CharSequence)}
 * @author: CIdea
 */
@Slf4j
@MappedTypes({Collection.class})
@MappedJdbcTypes(JdbcType.VARCHAR)
public class DurationTypeHandler extends BaseTypeHandler<Duration> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Duration parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, toString(parameter));
    }

    @Override
    public Duration getNullableResult(ResultSet rs, String columnName) throws SQLException {
        final String str = rs.getString(columnName);
        return parse(str);
    }

    @Override
    public Duration getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        final String str = rs.getString(columnIndex);
        return parse(str);
    }

    @Override
    public Duration getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        final String str = cs.getString(columnIndex);
        return parse(str);
    }

    private String toString(Duration duration) {
        if(duration == null){
            return null;
        }
        return duration.toString();
    }

    private Duration parse(String str) {
        if(StringUtils.isBlank(str)){
            return null;
        }
        return Duration.parse(str);
    }
}
