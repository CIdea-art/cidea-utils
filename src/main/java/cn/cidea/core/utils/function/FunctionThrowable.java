package cn.cidea.core.utils.function;

/**
 * @author CIdea
 */
@FunctionalInterface
public interface FunctionThrowable<R, P> {

    R apply(P parameter) throws Throwable;

}
