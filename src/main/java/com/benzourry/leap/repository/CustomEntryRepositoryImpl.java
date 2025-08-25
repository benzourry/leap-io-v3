package com.benzourry.leap.repository;

import com.benzourry.leap.model.Entry;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.stream.Stream;

@Repository
public class CustomEntryRepositoryImpl implements CustomEntryRepository{

    @PersistenceContext
    private EntityManager em;

    @Override
    public Stream<Entry> streamAll(Specification<Entry> spec) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Entry> query = cb.createQuery(Entry.class);
        Root<Entry> root = query.from(Entry.class);
        query.where(spec.toPredicate(root, query, cb));

        return em.createQuery(query).getResultStream();
    }

    @Override
    public List<JsonNode> findDataPaged(Specification<Entry> spec, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<JsonNode> query = cb.createQuery(JsonNode.class);
        Root<Entry> root = query.from(Entry.class);
        query.select(root.get("data"));
        query.where(spec.toPredicate(root, query, cb));


        return em.createQuery(query).getResultList();
    }


    @Override
    public Page<Long> findAllIds(Specification<Entry> spec, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        // Build count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Entry> countRoot = countQuery.from(Entry.class);
        countQuery.select(cb.count(countRoot));
        if (spec != null) {
            Predicate predicate = spec.toPredicate(countRoot, countQuery, cb);
            if (predicate != null) countQuery.where(predicate);
        }
        Long total = em.createQuery(countQuery).getSingleResult();

        // Build select query for IDs only
        CriteriaQuery<Long> selectQuery = cb.createQuery(Long.class);
        Root<Entry> root = selectQuery.from(Entry.class);
        selectQuery.select(root.get("id"));
        if (spec != null) {
            Predicate predicate = spec.toPredicate(root, selectQuery, cb);
            if (predicate != null) selectQuery.where(predicate);
        }

        List<Long> ids = em.createQuery(selectQuery)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        return new PageImpl<>(ids, pageable, total);
    }
}
