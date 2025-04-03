package cn.cidea.core.utils.asserts;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * 断言工具
 * 由异常枚举类实现{@link #build}接口创建异常，断言不通过则创建异常并抛出
 * 支持链式表达
 * @author CIdea
 */
public interface IAssert {

    /**
     * 创建异常
     * @param args
     * @return
     */
    RuntimeException build(String... args);


    /**
     * <p>断言对象<code>obj</code>非空。如果对象<code>obj</code>为空，则抛出异常
     * <p>异常信息<code>message</code>支持传递参数方式，避免在判断之前进行字符串拼接操作
     *
     * @param obj 待判断对象
     * @param args message占位符对应的参数列表
     */
    default IAssert nonNull(Object obj, String... args) {
        return isTrue(Objects.nonNull(obj), args);
    }

    default IAssert isNull(Object obj, String... args) {
        return isTrue(Objects.isNull(obj), args);
    }

    /**
     * 字符串非空
     * @param cs
     * @param args
     * @return
     */
    default IAssert isNotBlank(CharSequence cs, String... args) {
        if (StringUtils.isNotBlank(cs)) {
            return this;
        }
        throw build(args);
    }

    default IAssert isEmpty(Collection<?> collection, String... args) {
        if (CollectionUtils.isEmpty(collection)) {
            return this;
        }
        throw build(args);
    }

    default IAssert isNotEmpty(Collection<?> collection, String... args) {
        if (CollectionUtils.isNotEmpty(collection)) {
            return this;
        }
        throw build(args);
    }


    default IAssert isNotEmpty(Map map, String... args) {
        if (MapUtils.isNotEmpty(map)) {
            return this;
        }
        throw build(args);
    }


    default IAssert isNotEmpty(Object[] array, String... args) {
        if (ArrayUtils.isNotEmpty(array)) {
            return this;
        }
        throw build(args);
    }

    default IAssert equals(Object o1, Object o2, String... args) {
        if (Objects.equals(o1, o2)) {
            return this;
        }
        throw build(args);
    }

    default IAssert notEquals(Object o1, Object o2, String... args) {
        if (!Objects.equals(o1, o2)) {
            return this;
        }
        throw build(args);
    }

    default IAssert notIn(Object obj, Object[] array, String args){
        for (Object o : array) {
            if(Objects.equals(obj, o)){
                throw build(args);
            }
        }
        return this;
    }

    default IAssert in(Object obj, Object[] array, String args){
        for (Object o : array) {
            if(Objects.equals(obj, o)){
                return this;
            }
        }
        throw build(args);
    }

    default IAssert isTrue(Boolean b, String... args){
        if(BooleanUtils.isTrue(b)){
            return this;
        }
        throw build(args);
    }

    default IAssert isFalse(Boolean b, String... args){
        if(BooleanUtils.isFalse(b)){
            return this;
        }
        throw build(args);
    }

}
