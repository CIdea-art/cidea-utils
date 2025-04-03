package cn.cidea.core.utils.function;

/**
 *
 * @param <T>
 */
@FunctionalInterface
public interface SupplierThrowable<T> {

    T get() throws Throwable;

}
