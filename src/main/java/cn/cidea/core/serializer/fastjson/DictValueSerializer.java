package cn.cidea.core.serializer.fastjson;

import cn.cidea.core.utils.function.IDict;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * 对实现了{@link IDict}的对象内部属性类型，序列化时输出{@link IDict#value()}
 * // TODO CIdea: 反序列化的，暂时别用
 * @author: CIdea
 */
public class DictValueSerializer implements ObjectSerializer {

    @Override
    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        if (object != null && object instanceof IDict) {
            serializer.write(((IDict) object).value());
        } else {
            serializer.write(object);
        }
    }
}
