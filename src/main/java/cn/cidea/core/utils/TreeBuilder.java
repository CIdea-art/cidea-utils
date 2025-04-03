package cn.cidea.core.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 树构建
 * @author: CIdea
 */
@Slf4j
public class TreeBuilder {

    public static <PK extends Serializable, T extends TreeNode<PK, T>> Forest<PK, T> build(Collection<T> collection) {
        return build(null, collection);
    }

    /**
     * @param rootIds   指定的根节点ID，空时以parentId为空的作为根节点
     * @param collection 节点集合
     * @return 复数树构成的森林
     */
    public static <PK extends Serializable, T extends TreeNode<PK, T>> Forest<PK, T> build(Collection<PK> rootIds, Collection<T> collection) {
        Forest<PK, T> forest = new Forest<>();

        Map<PK, T> ref = collection.stream()
                .collect(Collectors.toMap(TreeNode::getId, a -> a));
        forest.setRef(ref);
        List<T> trees = new ArrayList<>();
        forest.setTrees(trees);
        for (T cur : collection) {
            if (rootIds != null && rootIds.contains(cur.getId())) {
                // 指定的根节点
                trees.add(cur);
            }
            if (rootIds == null && cur.getParentId() == null) {
                // 未指定根节点时，则默认parent为空的为根节点
                trees.add(cur);
            }
            if (cur.getParentId() != null) {
                // parent不为空，关联parent
                T parent = ref.get(cur.getParentId());
                if (parent == null) {
                    log.warn("Tree build warn, parent not found! parentId = {}", cur.getParentId());
                    continue;
                }
                Collection<T> children = parent.getChildren();
                if (children == null) {
                    children = new ArrayList<>();
                    parent.setChildren(children);
                }
                children.add(cur);
            }
        }
        if (rootIds != null) {
            // 指定的根节点之间可能有上下级，合并
            filterDownOfFront(forest);
        }
        return forest;
    }

    public static <PK extends Serializable, T extends TreeNode<PK, T>> Collection<T> filterDownOfFront(Forest<PK, T> forest) {
        Collection<T> trees = filterDownOfFront(forest.getTrees(), forest.getRef());
        forest.setTrees(trees);
        return trees;
    }

    /**
     * 如果front之间有上下级关系，需要移除front的下级，因为下级已经包含在front里的上级中了
     */
    public static <PK extends Serializable, T extends TreeNode<PK, T>> Collection<T> filterDownOfFront(Collection<T> trees, Map<PK, T> ref) {
        if (trees == null) {
            return new ArrayList<>(0);
        }
        Set<PK> rootIds = trees.stream().filter(Objects::nonNull).map(TreeNode::getId).collect(Collectors.toSet());
        trees = trees.stream().filter(f -> {
            TreeNode cur = f;
            while (cur != null && cur.getParentId() != null) {
                if (rootIds.contains(cur.getParentId())) {
                    // 往上追溯到已有，则剔除
                    return false;
                }
                cur = ref.get(cur.getParentId());
            }
            return true;
        }).collect(Collectors.toList());
        return trees;
    }

    /**
     * 树节点
     */
    public interface TreeNode<PK extends Serializable, T extends TreeNode<PK, T>> {
        PK getId();

        PK getParentId();

        Collection<T> getChildren();

        T setChildren(Collection<T> collection);
    }

    /**
     * 森林
     *
     * @param <PK>
     * @param <T>
     */
    @Data
    public static class Forest<PK extends Serializable, T extends TreeNode<PK, T>> {

        /**
         * 树根节点集合
         */
        private Collection<T> trees;

        /**
         * id索引
         */
        private Map<PK, T> ref;

        /**
         * 下面所有的子节点，可能有专有名词？
         * @param id
         * @return
         */
        public Set<T> allChildren(Serializable id){
            T t = ref.get(id);
            if(t == null || CollectionUtils.isEmpty(t.getChildren())){
                return Collections.emptySet();
            }
            Set<T> collect = t.getChildren().stream().flatMap(c -> allChildren(c.getId()).stream()).collect(Collectors.toSet());
            return collect;
        }

    }
}
