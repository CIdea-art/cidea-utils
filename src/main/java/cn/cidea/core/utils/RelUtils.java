package cn.cidea.core.utils;

import cn.cidea.core.utils.function.Consumer2;
import cn.cidea.core.utils.function.IPK;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sun.org.apache.regexp.internal.RE;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据结构关联拼接工具
 *
 * @author CIdea
 * @version 2022-09-29 {@link #relMany(Collection, Function, Function, Consumer2, Function)}
 */
@Slf4j
public class RelUtils {

    /**
     * 主表关联子表，子表中存主表ID
     *
     * @param coll
     * @param subFkGetter 子表关联ID，getterFunction
     * @param subGetter   主表getter
     * @param subSetter   主表seter
     * @param getSub      主表ID查询字表function
     * @param <E>
     * @param <SUB>
     * @param <RID>
     */
    public static <E extends IPK, SUB, RID> void relSubOne(
            Collection<E> coll,
            Function<SUB, RID> subFkGetter,
            Function<E, SUB> subGetter,
            Consumer2<E, SUB> subSetter,
            Function<Set<Serializable>, List<SUB>> getSub) {
        coll = coll.stream()
                .filter(entity -> subGetter.apply(entity) == null)
                .collect(Collectors.toList());
        if (coll.size() == 0) {
            return;
        }
        Set<Serializable> ids = coll.stream()
                .map(IPK::pkVal)
                .collect(Collectors.toSet());
        Map<RID, SUB> map = getSub.apply(ids)
                .stream()
                .collect(Collectors.toMap(subFkGetter, t -> t));
        for (E entity : coll) {
            SUB sub = map.get(entity.pkVal());
            subSetter.accept(entity, sub);
        }
    }

    /**
     * 主表关联多子表，子表中存主表ID
     *
     * @param <E>
     * @param <SUB>
     * @param <RID>
     * @param coll
     * @param subFkGetter
     * @param subGetter
     * @param subSetter
     * @param getSub
     */
    public static <E extends IPK, SUB, RID> void relSubMany(
            Collection<E> coll,
            Function<SUB, RID> subFkGetter,
            Function<E, List<SUB>> subGetter,
            Consumer2<E, List<SUB>> subSetter,
            Function<Set<Serializable>, List<SUB>> getSub) {
        coll = coll.stream()
                .filter(entity -> subGetter.apply(entity) == null)
                .collect(Collectors.toList());
        if (coll.size() == 0) {
            return;
        }
        Set<Serializable> idList = coll.stream()
                .map(IPK::pkVal)
                .collect(Collectors.toSet());
        Map<RID, List<SUB>> map = getSub.apply(idList)
                .stream()
                .filter(s -> subFkGetter.apply(s) != null)
                .collect(Collectors.groupingBy(subFkGetter));
        for (E entity : coll) {
            List<SUB> subList = map.getOrDefault(entity.pkVal(), Collections.EMPTY_LIST);
            subSetter.accept(entity, subList);
        }
    }

    public static <E, RID extends Serializable, RE extends IPK> void relOne(
            Collection<E> coll,
            Function<E, RID> fkGetter,
            Function<E, RE> relGetter,
            Consumer2<E, RE> relSetter,
            Function<Set<RID>, List<RE>> getRel) {
        relOne(coll, fkGetter, relGetter, relSetter, IPK::pkVal, getRel);
    }

    /**
     * 引用，表存其它表主键
     *
     * @param coll
     * @param fkGetter    主表引用的外键
     * @param relGetter   getter
     * @param relSetter   setter
     * @param relPkGetter 引用表的逻辑主键（不一定是ID）
     * @param getRel      用引用的外键，查询引用表
     * @param <E>
     * @param <RID>
     * @param <RE>
     */
    public static <E, RID extends Serializable, RE, RPK extends Serializable> void relOne(
            Collection<E> coll,
            Function<E, RID> fkGetter,
            Function<E, RE> relGetter,
            Consumer2<E, RE> relSetter,
            Function<RE, RPK> relPkGetter,
            Function<Set<RID>, List<RE>> getRel) {
        if (CollectionUtils.isEmpty(coll)) {
            return;
        }
        coll = coll.stream()
                .filter(entity -> {
                    RID rid = fkGetter.apply(entity);
                    if (rid == null) {
                        // 无关联ID
                        return false;
                    }
                    // 有关联ID，是否有关联对象
                    return relGetter.apply(entity) == null;
                })
                .collect(Collectors.toList());
        Set<RID> ridList = coll.stream().map(fkGetter).collect(Collectors.toSet());
        if (ridList.size() == 0) {
            return;
        }
        Map<Serializable, RE> reMap = getRel.apply(ridList).stream()
                .collect(Collectors.toMap(relPkGetter, re -> re));
        for (E entity : coll) {
            RID rid = fkGetter.apply(entity);
            RE re = reMap.get(rid);
            if (re == null) {
                log.warn("关联id异常{}!", rid);
            }
            relSetter.accept(entity, re);
        }
    }

    public static <E, REL extends IPK, RID extends Serializable> void relMany(
            Collection<E> coll,
            Function<E, Collection<RID>> fkGetter,
            Function<E, List<REL>> relGetter,
            Consumer2<E, List<REL>> relSetter,
            Function<Set<Serializable>, List<REL>> getRel) {
        coll = coll.stream()
                .filter(entity -> relGetter.apply(entity) == null)
                .collect(Collectors.toList());
        if (coll.size() == 0) {
            return;
        }

        Map<E, Set<RID>> opeRelMap = coll.stream().collect(Collectors.toMap(
                e -> e,
                e -> new HashSet<>(CollectionUtils.emptyIfNull(fkGetter.apply(e)))));
        Set<RID> relIds = opeRelMap.entrySet().stream()
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        if (relIds.size() == 0) {
            coll.forEach(opt -> relSetter.accept(opt, new ArrayList<>(0)));
            return;
        }
        Map<Serializable, REL> relPkMap = getRel.apply(new HashSet<>(relIds)).stream().collect(Collectors.toMap(REL::pkVal, e -> e));
        opeRelMap.entrySet().forEach(entry -> {
            List<REL> list = entry.getValue().stream().map(relPkMap::get).filter(Objects::nonNull).collect(Collectors.toList());
            relSetter.accept(entry.getKey(), list);
        });
    }

    /**
     * 含中间表的多对多
     *
     * @param coll
     * @param midGet 中间表查询
     * @param mlid   中间表获取主表的id
     * @param mrid   中间表获取右表的id
     * @param reGet  根据id获取右表数据
     * @param setter 设置右表
     * @param <E>    主表
     * @param <MID>  中间表
     * @param <RE>   右表
     */
    public static <E extends IPK, MID, RE extends IPK> void manyToMany(
            Collection<E> coll,
            Function<List<Serializable>, List<MID>> midGet,
            Function<MID, Serializable> mlid,
            Function<MID, Serializable> mrid,
            Function<Set<Serializable>, List<RE>> reGet,
            Consumer2<E, List<RE>> setter
    ) {

        if (CollectionUtils.isEmpty(coll)) {
            return;
        }
        List<Serializable> ids = coll.stream().map(IPK::pkVal).collect(Collectors.toList());

        List<MID> mids = midGet.apply(ids);
        Map<Serializable, List<MID>> midGroup = mids.parallelStream()
                .collect(Collectors.groupingBy(mlid));

        Set<Serializable> rids = mids.stream().map(mrid).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(rids)) {
            return;
        }
        Map<Serializable, RE> relMap = reGet.apply(rids).parallelStream().collect(Collectors.toMap(IPK::pkVal, r -> r));

        coll.parallelStream().forEach(entity -> {
            List<MID> em = midGroup.getOrDefault(entity.pkVal(), Collections.emptyList());
            List<RE> rs = em.parallelStream().map(mid -> relMap.get(mrid.apply(mid))).filter(Objects::nonNull).collect(Collectors.toList());
            setter.accept(entity, rs);
        });
    }
}
