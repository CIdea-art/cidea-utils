package cn.cidea.core.utils.function;

import java.io.Serializable;

/**
 * @author: CIdea
 */
public interface IDict<T extends Serializable> {

    /**
     * 字典值
     * @return
     */
    T value();

    /**
     * 字典文本
     * @return
     */
    String text();

}
