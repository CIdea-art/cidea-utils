package cn.cidea.core.dal.redis;

import cn.cidea.core.dal.DbDAO;
import cn.cidea.core.utils.SynchronizedUtils;
import cn.cidea.core.utils.function.IPK;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Redis缓存DAO
 * 可实现{@link #loadById(Serializable)}、{@link #loadByIds(Set)}、{@link #loadAll()}从数据源（如数据库）加载数据
 * // TODO CIdea: 缓存时间配置，过期时间配置
 * // TODO CIdea: 可自定义序列化方案
 * @author CIdea
 */
@Slf4j
public abstract class RedisCacheDAO<E extends IPK> extends DbDAO<E> {

    @Autowired
    protected RedissonClient redissonClient;

    protected abstract String cacheKey();

    // 不能PostConstruct，因为有时候组件没加载完全
    // @PostConstruct
    // 低版本spring会没有timeUnit
    // @Scheduled(initialDelay = 0, fixedDelay = 10 * 60 * 1000, timeUnit = TimeUnit.MINUTES)
    @Scheduled(initialDelay = 0, fixedRate = 10 * 60 * 1000)
    public void refresh() {
        Supplier<Collection<E>> loadAll = loadAll();
        if(loadAll == null){
            return;
        }
        // 全局读写锁。写单个数据时使用读锁，互相不冲突；写全部时使用写锁，完全排它
        RLock lock = redissonClient.getReadWriteLock(getLockKey()).writeLock();
        lock.lock();
        try {
            log.info("refresh cache, class = {}", this.getClass().getSimpleName());
            // TODO redisson文档，各个对象的使用说明
            // redissonClient.getMap();
            // redissonClient.getMapCache();
            // redissonClient.getLocalCachedMap();
            // RBucket<Object> bucket = redissonClient.getBucket();
            // TODO: 2023/3/6 什么时候适合全刷，分布式的话不太适合每台独自进行
            clear();
            insert(loadAll.get());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Collection<E> selectBatchIds(Set<Serializable> ids) {
        // bloom性质：hash位图。若不存在，则一定不存在；若存在，可能hash重复导致误判
        RBloomFilter<Serializable> bloomFilter = getBloomFilter();
        // 过滤出存在的id
        ids = ids.stream()
                .filter(bloomFilter::contains)
                .collect(Collectors.toSet());
        if (ids.size() == 0) {
            // （全部）不存在立即返回，避免访问数据库，造成穿透
            return new ArrayList<>(0);
        }

        RMap<Serializable, E> cache = getCache();
        Map<Serializable, E> data = cache.getAll(ids);

        Function<Set<Serializable>, Collection<E>> loadByIds = loadByIds();
        if(loadByIds == null){
            return data.values();
        }
        if(data.size() == ids.size()) {
            // 全部读取成功，直接返回
            return data.values();
        }
        // 避免hash误判，过滤缓存中不存在的id，加载一次（从数据库），小概率事件
        Set<Serializable> loadIds = ids.stream()
                .filter(id -> !data.containsKey(id))
                .collect(Collectors.toSet());
        // 全局读锁，与全局写锁互斥，先让刷新写数据
        RLock lockAll = redissonClient.getReadWriteLock(getLockKey()).readLock();
        lockAll.lock(1, TimeUnit.SECONDS);
        try {
            // 所有id的锁，避免重复加载
            SynchronizedUtils.lock(getLockKey() + ":", loadIds, () -> {
                // 获取到锁后重新从缓存读一次，避免加载队列从DB重复加载
                Map<Serializable, E> loadData = cache.getAll(loadIds);
                if (!loadData.isEmpty()) {
                    data.putAll(loadData);
                    loadIds.removeAll(loadData.keySet());
                }
                if (!loadIds.isEmpty()) {
                    Collection<E> coll = loadByIds.apply(loadIds);
                    insert(coll);
                }
            });
        } finally {
            if (lockAll.isLocked()) {
                try {
                    lockAll.unlock();
                } catch (Throwable e){
                    log.error("unlock error", e);
                }
            }
        }
        return data.values();
    }

    @Override
    public final void insert(Collection<E> coll) {
        if (CollectionUtils.isEmpty(coll)) {
            return;
        }
        RMap<Serializable, E> cache = getCache();
        cache.putAll(coll.stream().collect(Collectors.toMap(IPK::pkVal, u -> u)));

        RBloomFilter<Serializable> bloomFilter = getBloomFilter();
        coll.forEach(e -> bloomFilter.add(e.pkVal()));
    }

    @Override
    public final void clear() {
        getCache().clear();
        getBloomFilter().delete();
    }

    @Override
    public void deleteBatchIds(Collection<Serializable> ids) {
        // bloom好像没有删除的概念？
        // 可能是因为hash冲突的原因，一个数据对应一个hash，但hash可能对应复数数据，如果删除了这个数据对应的hash，就会导致hash对应的其它数据被误判不存在，无法确保bloom不存在则必定不存在的性质
        // TODO CIdea: bloom怎么应对删除问题，如果一个数据已经被删除，但bloom仍旧存在，就会一直去加载。思考方案一：用空对象覆盖缓存取代删除
        // RBloomFilter<Serializable> bloomFilter = redissonClient.getBloomFilter(getBloomKey());
        RMap<Serializable, E> cache = getCache();
        ids.forEach(cache::remove);
    }

    private String getLockKey() {
        return cacheKey() + ":lock";
    }

    private RMap<Serializable, E> getCache() {
        // TODO 若redis挂掉则改用本地缓存，避免雪崩
        // TODO CIdea: 本地缓存技术比较多，SPI结合预设方案
        String cacheKey = cacheKey() + ":cache";
        return redissonClient.getMap(cacheKey);
    }

    private RBloomFilter<Serializable> getBloomFilter() {
        String bloomKey = cacheKey() + ":bloom";
        RBloomFilter<Serializable> bloomFilter = redissonClient.getBloomFilter(bloomKey);
        bloomFilter.tryInit(1000, 0.001);
        return bloomFilter;
    }

}
