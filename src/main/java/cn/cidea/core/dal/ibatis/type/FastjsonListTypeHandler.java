package cn.cidea.core.dal.ibatis.type;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.lang.reflect.ParameterizedType;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * JSONArray互转List
 * @author CIdea
 * @version 2022-10-19
 */
@Slf4j
@MappedTypes({Collection.class})
@MappedJdbcTypes(JdbcType.VARCHAR)
public abstract class FastjsonListTypeHandler<T> extends AbstractJsonTypeHandler<List<T>> {

    @Override
    protected List<T> parse(String json) {
        if(StringUtils.isBlank(json)){
            return new ArrayList<>(0);
        }
        Class<T> tClass = (Class<T>)((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        List<T> list = JSONArray.parseArray(json, tClass);
        return list;
    }

    @Override
    protected String toJson(List<T> list) {
        if(CollectionUtils.isEmpty(list)){
            return null;
        }
        return JSONArray.toJSONString(list);
    }

    @Override
    public List<T> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        final String json = rs.getString(columnName);
        return parse(json);
    }

    @Override
    public List<T> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        final String json = rs.getString(columnIndex);
        return parse(json);
    }

    @Override
    public List<T> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        final String json = cs.getString(columnIndex);
        return parse(json);
    }
}
