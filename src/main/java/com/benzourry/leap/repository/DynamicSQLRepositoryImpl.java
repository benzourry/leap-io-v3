package com.benzourry.leap.repository;


import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

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
     */
    public List runQuery(String q, Map<String, Object> params, boolean nativeQuery) {
        Query query = nativeQuery ? em.createNativeQuery(q) : em.createQuery(q);
        if (params!=null) {
            params.keySet().forEach(k -> {
                query.setParameter(k, params.get(k));
            });
        }
        return query.getResultList();
    }

    /**
     * asMap untuk kegunaan lambda select()
     * @param q
     * @param nativeQuery
     * @return
     */
    public List runQueryAsMap(String q, Map<String, Object> params, boolean nativeQuery) {
        Query query = nativeQuery ? em.createNativeQuery(q) : em.createQuery(q);
        if (params!=null) {
            params.keySet().forEach(k -> {
                query.setParameter(k, params.get(k));
            });
        }
        if (nativeQuery) {
            return query
                    .unwrap(org.hibernate.query.Query.class)
                    .setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE)
                    .getResultList();
        }else{
            return query.getResultList();
        }
    }


    public int executeQuery(String q, Map<String, Object> params) {
        Query query = em.createNativeQuery(q);
        if (params!=null){
            params.keySet().forEach(k->{
                query.setParameter(k, params.get(k));
            });
        }

        return query.executeUpdate();
    }

    @Override
    public List runPagedQuery(String q, Map<String, Object> params, boolean nativeQuery, Pageable pageable) {
//        Map<String, Object> data = new HashMap<>();
        Query query = nativeQuery ? em.createNativeQuery(q) : em.createQuery(q);
        if (params!=null) {
            params.keySet().forEach(k -> {
                query.setParameter(k, params.get(k));
            });
        }
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
     */
    @Override
    public List runPagedQueryAsMap(String q, Map<String,Object> params,boolean nativeQuery, Pageable pageable) {
//        Map<String, Object> data = new HashMap<>();
        Query query = nativeQuery ? em.createNativeQuery(q) : em.createQuery(q);
        query.setFirstResult(pageable.getPageNumber() * pageable.getPageSize());
        query.setMaxResults(pageable.getPageSize());
        if (params!=null) {
            params.keySet().forEach(k -> {
                query.setParameter(k, params.get(k));
            });
        }

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
    public int getQueryCount(String sql, Map<String, Object> params,boolean nativeQuery) {
//        sql.replaceAll("(.*?select)" + "(.*?)" + "(from.*)", "$1 count(*) $3")

        String queryString = nativeQuery ? "SELECT COUNT(*) FROM (" + sql + ")" : sql.replaceAll("(.*?select)(.*?)(from.)(\\w+.)(\\w+)", "$1 count($5) $3 $4 $5 ");
        Query query = nativeQuery ? em.createNativeQuery(queryString) : em.createQuery(queryString);
        if (params!=null) {
            params.keySet().forEach(k -> {
                query.setParameter(k, params.get(k));
            });
        }
        return Integer.parseInt(query.getSingleResult() + "");
    }

}
