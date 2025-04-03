package cn.cidea.core.dal.ibatis.type;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.lang.reflect.ParameterizedType;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * JSONArray互转Set
 * @author CIdea
 * @version 2022-10-19
 */
@Slf4j
@MappedTypes({Collection.class})
@MappedJdbcTypes(JdbcType.VARCHAR)
public abstract class FastjsonSetTypeHandler<T> extends AbstractJsonTypeHandler<Set<T>> {

    @Override
    protected Set<T> parse(String json) {
        if(StringUtils.isBlank(json)){
            return new HashSet<>(0);
        }
        Class<T> tClass = (Class<T>)((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        List<T> list = JSONArray.parseArray(json, tClass);
        return new HashSet<>(list);
    }

    @Override
    protected String toJson(Set<T> list) {
        return JSONArray.toJSONString(ObjectUtils.firstNonNull(list, Collections.EMPTY_SET));
    }

    @Override
    public Set<T> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        final String json = rs.getString(columnName);
        return parse(json);
    }

    @Override
    public Set<T> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        final String json = rs.getString(columnIndex);
        return parse(json);
    }

    @Override
    public Set<T> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        final String json = cs.getString(columnIndex);
        return parse(json);
    }
}
