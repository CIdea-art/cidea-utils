package cn.cidea.core.dal;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 数据库DAO
 * @author: CIdea
 */
public abstract class DbDAO<E> {

    public abstract Collection selectBatchIds(Set<Serializable> ids);
    public E selectById(Serializable id) {
        if(id == null){
            return null;
        }
        Collection<E> collection = selectBatchIds(Collections.singleton(id));
        if(collection.size() == 0){
            return null;
        }
        return collection.iterator().next();
    }

    public abstract void insert(Collection<E> coll);
    public final void insert(E entity) {
        if (entity == null) {
            return;
        }
        insert(Collections.singleton(entity));
    }

    public abstract void clear();
    public abstract void deleteBatchIds(Collection<Serializable> ids);
    public void deleteById(Serializable id) {
        deleteBatchIds(Collections.singleton(id));
    }

    /**
     * 从数据库加载
     * @return
     */
    protected Function<Set<Serializable>, Collection<E>> loadByIds(){
        return null;
    }

    /**
     * 从数据库加载
     * @return
     */
    protected Supplier<Collection<E>> loadAll(){
        return null;
    }

}
