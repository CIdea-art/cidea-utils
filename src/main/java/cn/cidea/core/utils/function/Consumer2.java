package cn.cidea.core.utils.function;

/**
 * @author: CIdea
 */
@FunctionalInterface
public interface Consumer2<P1, P2> {

    void accept(P1 p1, P2 p2);
}
