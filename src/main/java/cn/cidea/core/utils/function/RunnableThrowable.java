package cn.cidea.core.utils.function;

/**
 * @author: CIdea
 */
@FunctionalInterface
public interface RunnableThrowable {

    void run() throws Throwable;
}
