package cn.cidea.core.dal.redis;

import cn.cidea.core.utils.function.IPK;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 集成了mybatisplus的缓存DAO
 * @author CIdea
 */
@Slf4j
public abstract class RedisCacheMapper<M extends BaseMapper<E>, E extends IPK> extends RedisCacheDAO<E> {

    @Autowired
    protected M mapper;

    @Override
    protected Function<Set<Serializable>, Collection<E>> loadByIds(){
        return ids -> {
            if(CollectionUtils.isEmpty(ids)){
                return Collections.EMPTY_LIST;
            } else if(ids.size() == 0){
                E entity = mapper.selectById(ids.iterator().next());
                if(entity == null){
                    return Collections.EMPTY_LIST;
                }
                return Collections.singleton(entity);
            } else {
                return mapper.selectBatchIds(ids);
            }
        };
    }

    @Override
    protected Supplier<Collection<E>> loadAll() {
        return () -> mapper.selectList(new QueryWrapper<>());
    }

}
