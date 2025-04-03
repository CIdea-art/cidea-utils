package cn.cidea.core.dal.elasticsearch;

import cn.cidea.core.dal.DbDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Elasticsearch
 * @author: CIdea
 */
public abstract class ElasticsearchRep<E, R extends ElasticsearchRepository<E, ?>> extends DbDAO<E> {

    @Autowired
    private R rep;

    @Scheduled(initialDelay = 0, fixedRate = 10 * 60 * 1000)
    public void refresh() {
        Supplier<Collection<E>> loadAll = loadAll();
        if (loadAll == null) {
            return;
        }
        clear();
        insert(loadAll.get());
    }

    public void insert(Collection<E> coll) {
        if (coll == null || coll.isEmpty()) {
            return;
        }
        rep.saveAll(coll);
    }

    public void clear() {
        rep.deleteAll();
    }

}
