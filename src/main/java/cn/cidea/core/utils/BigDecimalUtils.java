package cn.cidea.core.utils;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

/**
 * @version 2022-12-13
 * @author Charlotte
 */
public class BigDecimalUtils {

    /**
     * 计算集合中某个字段的总和
     * @param collection
     * @param function
     * @return
     * @param <T>
     */
    public static <T> BigDecimal sum(Collection<T> collection, Function<T, BigDecimal> function){
        return collection.stream()
                .filter(Objects::nonNull)
                .map(function::apply)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static <T> BigDecimal sumInt(Collection<T> collection, Function<T, Integer> function) {
        return collection.stream()
                .filter(Objects::nonNull)
                .map(function::apply)
                .filter(Objects::nonNull)
                .map(BigDecimal::new)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static <T> BigDecimal sumString(Collection<T> collection, Function<T, String> function) {
        return collection.stream()
                .filter(Objects::nonNull)
                .map(function::apply)
                .filter(StringUtils::isNotBlank)
                .map(BigDecimal::new)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 移除科学计数法
     */
    public static BigDecimal strip(BigDecimal decimal) {
        if (decimal == null) {
            return null;
        }
        decimal = decimal.stripTrailingZeros();
        if (decimal.scale() < 0) {
            // 避免stripTrailingZeros后变为科学计数法
            decimal = decimal.setScale(0);
        }
        return decimal;
    }

}
