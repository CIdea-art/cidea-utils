package cn.cidea.core.dal.ibatis.type;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 字符串分隔符处理
 * @author CIdea
 */
public abstract class SplitTypeHandler<E> extends BaseTypeHandler<List> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List parameter, JdbcType jdbcType) throws SQLException {
        String value = null;
        if (CollectionUtils.isNotEmpty(parameter)) {
            value = StringUtils.join(parameter, getSeparatorChars());
        }
        ps.setString(i, value);
    }

    @Override
    public List getNullableResult(ResultSet rs, String columnName) throws SQLException {
        final String str = rs.getString(columnName);
        return StringUtils.isBlank(str) && rs.wasNull() ? null : split(str);
    }

    @Override
    public List getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        final String str = rs.getString(columnIndex);
        return StringUtils.isBlank(str) && rs.wasNull() ? null : split(str);
    }

    @Override
    public List getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        final String str = cs.getString(columnIndex);
        return StringUtils.isBlank(str) && cs.wasNull() ? null : split(str);
    }

    private List split(String str) {
        if (StringUtils.isBlank(str)) {
            return Collections.EMPTY_LIST;
        }
        List<String> list = Arrays.asList(StringUtils.split(str, getSeparatorChars()));
        Function<String, E> parse = parse();
        if (parse == null) {
            return list;
        }
        List<E> collect = list.stream()
                .map(parse)
                .collect(Collectors.toList());
        return collect;
    }

    /**
     * 字符串格式化
     * @return
     */
    public Function<String, E> parse() {
        return null;
    }

    /**
     * 分隔符
     * @return
     */
    protected abstract char getSeparatorChars();
}
