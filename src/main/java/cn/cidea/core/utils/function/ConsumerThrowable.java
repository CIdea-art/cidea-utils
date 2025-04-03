package cn.cidea.core.utils.function;

/**
 * @author: CIdea
 */
@FunctionalInterface
public interface ConsumerThrowable<T> {

    void accept(T t) throws Throwable;
}
