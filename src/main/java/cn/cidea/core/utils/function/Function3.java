package cn.cidea.core.utils.function;

/**
 * @author: CIdea
 */
@FunctionalInterface
public interface Function3<R, P1, P2, P3> {

    R apply(P1 p1, P2 p2, P3 p3);

}
