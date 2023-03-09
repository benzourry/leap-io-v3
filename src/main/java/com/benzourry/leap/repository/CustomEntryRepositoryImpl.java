package com.benzourry.leap.repository;

import com.benzourry.leap.model.Entry;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Page;
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
}
