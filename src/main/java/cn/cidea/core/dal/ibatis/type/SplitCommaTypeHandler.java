package cn.cidea.core.dal.ibatis.type;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.util.List;

/**
 * 字符串分隔转集合
 * @author CIdea
 */
@Slf4j
@MappedTypes({List.class})
@MappedJdbcTypes(JdbcType.VARCHAR)
public class SplitCommaTypeHandler extends SplitTypeHandler<String> {

    private static final char SEPARATOR_CHARS = ',';

    @Override
    protected char getSeparatorChars() {
        return SEPARATOR_CHARS;
    }

}
