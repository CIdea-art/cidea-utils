package cn.cidea.core.utils.function;

/**
 * @author CIdea
 */
@FunctionalInterface
public interface Function2Throwable<R, P1, P2> {

    R apply(P1 p1, P2 p2) throws Throwable;

}
