package com.benzourry.leap.repository;


import com.vladmihalcea.hibernate.query.MapResultTransformer;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;
import java.util.Map;

/**
 * Created by MohdRazif on 4/27/2017.
 */
@Repository
public class DynamicSQLRepositoryImpl implements DynamicSQLRepository {
    @PersistenceContext
    private EntityManager em;

    /**
     * Ada digunakan internally oleh dashboard, so polah method baru utk return map
     * @param q
     * @param nativeQuery
     * @return
     * @throws Exception
     */
    public List runQuery(String q, boolean nativeQuery) throws Exception {
        Query query = nativeQuery ? em.createNativeQuery(q) : em.createQuery(q);
        return query.getResultList();
    }

    /**
     * asMap untuk kegunaan lambda select()
     * @param q
     * @param nativeQuery
     * @return
     * @throws Exception
     */
    public List runQueryAsMap(String q, boolean nativeQuery) throws Exception {
        Query query = nativeQuery ? em.createNativeQuery(q) : em.createQuery(q);
        if (nativeQuery) {
            return query
                    .unwrap(org.hibernate.query.Query.class)
                    .setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE)
                    .getResultList();
        }else{
            return query.getResultList();
        }
    }

    public int executeQuery(String q) throws Exception {
        Query query = em.createNativeQuery(q);
        return query.executeUpdate();
    }

    @Override
    public List runPagedQuery(String q, boolean nativeQuery, Pageable pageable) throws Exception {
//        Map<String, Object> data = new HashMap<>();
        Query query = nativeQuery ? em.createNativeQuery(q) : em.createQuery(q);
        query.setFirstResult(pageable.getPageNumber() * pageable.getPageSize());
        query.setMaxResults(pageable.getPageSize());
        return query.getResultList();
    }

    /**
     * asMap utk kegunaan lambda select()
     * @param q
     * @param nativeQuery
     * @param pageable
     * @return
     * @throws Exception
     */
    @Override
    public List runPagedQueryAsMap(String q, boolean nativeQuery, Pageable pageable) throws Exception {
//        Map<String, Object> data = new HashMap<>();
        Query query = nativeQuery ? em.createNativeQuery(q) : em.createQuery(q);
        query.setFirstResult(pageable.getPageNumber() * pageable.getPageSize());
        query.setMaxResults(pageable.getPageSize());

        if (nativeQuery) {
            return query
                    .unwrap(org.hibernate.query.Query.class)
                    .setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE)
                    .getResultList();
        }else{
            return query.getResultList();
        }
    }


    @Override
    public int getQueryCount(String sql, boolean nativeQuery) {
//        sql.replaceAll("(.*?select)" + "(.*?)" + "(from.*)", "$1 count(*) $3")

        String queryString = nativeQuery ? "SELECT COUNT(*) FROM (" + sql + ")" : sql.replaceAll("(.*?select)(.*?)(from.)(\\w+.)(\\w+)", "$1 count($5) $3 $4 $5 ");
        Query query = nativeQuery ? em.createNativeQuery(queryString) : em.createQuery(queryString);
        return Integer.parseInt(query.getSingleResult() + "");
    }

}
