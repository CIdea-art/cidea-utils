package cn.cidea.core.dal.ibatis.type;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.util.Collection;

/**
 * JSONArray互转Set<Long>
 * @author: CIdea
 */
@Slf4j
@MappedTypes({Collection.class})
@MappedJdbcTypes(JdbcType.VARCHAR)
public class LongSetTypeHandler extends FastjsonSetTypeHandler<Long> {

}
