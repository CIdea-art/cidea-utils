package cn.cidea.core.dal.redis;

import cn.cidea.core.utils.function.IPK;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import org.apache.commons.collections4.MapUtils;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 缓存逻辑主键-》物理主键
 * @author: CIdea
 */
public abstract class RedisCacheLPK<T extends IPK> {

    @Autowired
    private RedissonClient redissonClient;

    private static final String NULL = "NULL";
    private static final String SPLIT = ":";

    public abstract String cacheKey();

    public abstract List<Function<T, ?>> lpkf();

    private String lpk(T t){
        List<Function<T, ?>> lpkf = lpkf();
        Assert.notEmpty(lpkf, this.getClass() + "lpkf not be empty");
        List<String> lpk = new ArrayList<>();
        for (Function<T, ?> function : lpkf) {
            String str = Optional.ofNullable(function.apply(t)).map(Object::toString).orElse(NULL);
            Assert.doesNotContain(str, SPLIT, "lpk value can't contain keyword '" + SPLIT + "'");
            lpk.add(str);
        }
        return lpk.stream().collect(Collectors.joining(SPLIT));
    }

    private RMapCache<String, Serializable> cache() {
        return redissonClient.getMapCache(cacheKey());
    }

    public Serializable find(T entity){
        return cache().get(lpk(entity));
    }

    public void save(Collection<T> list){
        Map<String, Serializable> collect = list.parallelStream().collect(Collectors.toMap(this::lpk, IPK::pkVal, (s1, s2) -> s1));
        cache().putAll(collect);
    }

    public void clear(){
        cache().clear();
    }

    public void delete(T entity){
        cache().get(lpk(entity));
    }

    @Deprecated
    public void delete(Serializable pk){
        // TODO CIdea:
    }
}
