package cn.cidea.core.utils.function;

/**
 * @author: CIdea
 */
@FunctionalInterface
public interface Function4<R, P1, P2, P3, P4> {

    R apply(P1 p1, P2 p2, P3 p3, P4 p4);

}
