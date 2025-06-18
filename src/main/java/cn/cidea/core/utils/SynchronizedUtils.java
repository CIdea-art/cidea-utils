package cn.cidea.core.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonMultiLock;
import org.redisson.api.RFuture;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 同步工具
 * 锁、事务
 * 引了redisson、spring-tx、spring-context
 *
 * @version 2022-12-08
 * @author: CIdea
 */
@Slf4j
@Component
@ConditionalOnClass({RedissonClient.class, ThreadPoolTaskExecutor.class, PlatformTransactionManager.class})
public class SynchronizedUtils implements ApplicationContextAware {

    @Autowired(required = false)
    private ThreadPoolTaskExecutor threadPoolExecutor;
    @Autowired(required = false)
    private PlatformTransactionManager transactionManager;
    @Autowired(required = false)
    private RedissonClient redissonClient;

    private static SynchronizedUtils self;

    /**
     * 事务提交后执行
     * 注册的{@link TransactionSynchronization}会被添加到{@link TransactionSynchronizationManager#synchronizations}集合中，然后在合适的时机执行
     * WARN: 同一个事务中不可嵌套，只有最外层会生效。简单的集合遍历问题，遍历过程中不可再往集合中添加元素，{@link org.springframework.transaction.support.TransactionSynchronizationUtils#invokeAfterCommit}
     * 场景一：异常导致事务回滚，执行一些无法回滚的代码，比如消息推送
     */
    public static void afterTrxCommit(Runnable runnable) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            // 没有事务，直接执行
            log.warn("run none transaction");
            runnable.run();
        } else {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("run after transaction commit");
                    runnable.run();
                }
            });
        }
    }

    public static void beforeTrxCommit(Runnable runnable) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            // 没有事务，直接执行
            log.warn("run none transaction");
            runnable.run();
        } else {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    log.info("run before transaction commit");
                    runnable.run();
                }
            });
        }
    }

    /**
     * 新线程执行
     *
     * @return
     */
    public static ListenableFuture<?> newThr(Runnable runnable) {
        return self.threadPoolExecutor.submitListenable(() -> {
            log.info("new thread start");
            runnable.run();
            log.info("new thread end");
        });
    }

    /**
     * 事务提交后新线程执行
     */
    public static void afterTrxCommitAndNewThr(Runnable runnable) {
        afterTrxCommit(() -> newThr(runnable));
    }

    /**
     * 新事务，嵌套时注意避免行锁
     */
    public static void newTrx(Runnable runnable) {
        newTrx(() -> {
            runnable.run();
            return null;
        });
    }

    public static <T> T newTrx(Supplier<T> supplier) {
        log.info("new transaction start");
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus ts = self.transactionManager.getTransaction(def);
        try {
            return supplier.get();
        } catch (Throwable e) {
            ts.setRollbackOnly();
            log.error("new transaction rollback", e);
            throw e;
        } finally {
            self.transactionManager.commit(ts);
            log.info("new transaction commit");
        }
    }

    /**
     * 单例锁
     * Supplier带返回值，Runnable不带
     */
    public static <T> T lock(String name, Supplier<T> supplier) {
        return lock(Collections.singleton(name), supplier);
    }

    public static void lock(String name, Runnable runnable) {
        lock(Collections.singleton(name), () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 联合锁
     * 拼接preffix和names里的元素生成锁的key
     *
     * @param preffix
     * @param names
     * @param supplier
     */
    public static void lock(String preffix, Collection<?> names, Runnable supplier) {
        Set<String> keys = names.parallelStream()
                .map(name -> preffix + name)
                .collect(Collectors.toSet());
        lock(keys, supplier);
    }

    public static <T> T lock(String preffix, Collection<?> names, Supplier<T> supplier) {
        Set<String> keys = names.stream()
                .map(name -> preffix + name)
                .collect(Collectors.toSet());
        return lock(keys, supplier);
    }

    public static void lock(Collection<String> names, Runnable runnable) {
        lock(names, () -> {
            runnable.run();
            return null;
        });
    }

    public static <T> T lock(Collection<String> names, Supplier<T> supplier) {
        if (!(names instanceof Set)) {
            names = names.stream().collect(Collectors.toSet());
        }
        Assert.notEmpty(names, "lock names not be empty!");
        RLock lock;
        Iterator<String> iterator = names.iterator();
        if (names.size() == 1) {
            lock = self.redissonClient.getLock(iterator.next());
        } else {
            RLock[] subLocks = new RLock[names.size()];
            for (int i = 0; i < names.size(); i++) {
                subLocks[i] = self.redissonClient.getLock(iterator.next());
            }
            lock = self.redissonClient.getMultiLock(subLocks);
        }
        boolean b;
        log.info("try lock for {}", JSONObject.toJSONString(names));
        try {
            b = lock.tryLock(6, 30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("redisson tryLock error!", e);
            throw new RuntimeException("系统繁忙，请稍后重试");
        }
        if (!b) {
            throw new RuntimeException("系统处理中，请勿重复点击");
        }
        log.info("locked for {}", JSONObject.toJSONString(names));
        try {
            return supplier.get();
        } finally {
            RFuture<Void> unlockFuture = lock.unlockAsync();
            Collection<String> finalNames = names;
            unlockFuture.whenCompleteAsync((unused, e) -> {
                if (e != null) {
                    log.error("unlock error! key = " + JSONObject.toJSONString(finalNames), e);
                } else {
                    log.info("manual unlock for {}", JSONObject.toJSONString(finalNames));
                }
            });
            // if (lock.isLocked()) {
            //     // MultiLock不支持
            //     try {
            //         lock.unlock();
            //     } catch (Throwable e) {
            //         log.error("unlock error! key = " + JSONObject.toJSONString(names), e);
            //         if (lock.isLocked()) {
            //             // 解锁失败
            //             throw e;
            //         }
            //     }
            // } else {
            //     // 锁已经自动释放
            //     // 场景一，超出持有时间
            //     log.info("lock already release for {}", JSONObject.toJSONString(names));
            // }
        }
    }

    /**
     * 全局锁并发起新事务
     * 用途：保证只有一个事务在操作状态，同时是最新状态，注意事务隔离和行锁
     */
    public static <T> T lockNewTrx(Collection<String> names, Supplier<T> supplier) {
        return lock(names, () -> newTrx(supplier));
    }

    public static void lockNewTrx(Collection<String> names, Runnable runnable) {
        lock(names, () -> newTrx(runnable));
    }

    public static <T> T lockNewTrx(String name, Supplier<T> supplier) {
        return lock(name, () -> newTrx(supplier));
    }

    public static void lockNewTrx(String name, Runnable runnable) {
        lock(name, () -> newTrx(runnable));
    }

    public static void lockNewTrx(String preffix, Collection<?> names, Runnable runnable) {
        lock(preffix, names, () -> newTrx(runnable));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        self = applicationContext.getBean(SynchronizedUtils.class);
    }

}
