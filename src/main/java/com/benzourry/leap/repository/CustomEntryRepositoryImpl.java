package com.benzourry.leap.repository;

import com.benzourry.leap.model.Entry;
import com.benzourry.leap.model.EntryApproval;
import com.benzourry.leap.model.EntryDto;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.*;
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

    public Page<EntryDto> findPaged(Specification<Entry> spec, Map<String, Set<String>> fields, boolean includeApproval, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<EntryDto> cq = cb.createQuery(EntryDto.class);
        Root<Entry> root = cq.from(Entry.class);

        // Apply predicates from Specification
        Predicate predicate = (spec != null) ? spec.toPredicate(root, cq, cb) : cb.conjunction();
        cq.where(predicate);

        // Apply ORDER BY
        // Apply ORDER BY from specification
        if (cq.getOrderList().isEmpty() && pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            pageable.getSort().forEach(order -> {
                Path<Object> path = root.get(order.getProperty());
                orders.add(order.isAscending() ? cb.asc(path) : cb.desc(path));
            });
            cq.orderBy(orders);
        }


        Join<Entry, Entry> prevEntryJoin = root.join("prevEntry", JoinType.LEFT);
        Expression<String> dataJson;
        Expression<Object> prevJson;
        if (fields == null || fields.isEmpty()) {
            dataJson = cb.function("JSON_EXTRACT", String.class, root.get("data"), cb.literal("$"));

            prevJson = cb.selectCase()
                    .when(cb.isNotNull(prevEntryJoin.get("id")),
                            cb.function("JSON_EXTRACT", String.class, prevEntryJoin.get("data"), cb.literal("$"))
                    )
                    .otherwise(cb.nullLiteral(String.class));
        }else{
            // Build JSON field selections
            List<Expression<?>> dataJsonArgs = new ArrayList<>();
            for (String key : fields.getOrDefault("$", Collections.emptySet())) {
                dataJsonArgs.add(cb.literal(key));
                dataJsonArgs.add(cb.function("JSON_EXTRACT", Object.class, root.get("data"), cb.literal("$." + key)));
            }
            dataJson = cb.function(
                    "JSON_MERGE_PATCH",
                    String.class,
                    cb.literal("{}"),
                    cb.function("JSON_OBJECT", String.class, dataJsonArgs.toArray(new Expression[0]))
            );

            List<Expression<?>> prevJsonArgs = new ArrayList<>();
            for (String key : fields.getOrDefault("$prev$", Collections.emptySet())) {
                prevJsonArgs.add(cb.literal(key));
                prevJsonArgs.add(cb.function("JSON_EXTRACT", Object.class, prevEntryJoin.get("data"), cb.literal("$." + key)));
            }
            prevJson = cb.selectCase()
                    .when(cb.isNotNull(prevEntryJoin.get("id")),
                            cb.function(
                                    "JSON_MERGE_PATCH",
                                    String.class,
                                    cb.literal("{}"),
                                    cb.function("JSON_OBJECT", String.class, prevJsonArgs.toArray(new Expression[0]))
                            )
                    )
                    .otherwise(cb.nullLiteral(String.class));
        }

        cq.select(cb.construct(
                EntryDto.class,
                root.get("id"),
                dataJson,
                prevJson,
                root.get("formId"),
                root.get("currentTier"),
                root.get("currentStatus"),
                root.get("currentTierId"),
                root.get("finalTierId"),
                root.get("submissionDate"),
                root.get("resubmissionDate"),
                root.get("currentEdit"),
                root.get("createdDate"),
                root.get("createdBy"),
                root.get("modifiedDate"),
                root.get("modifiedBy"),
                root.get("live"),
                root.get("email"),
                cb.function("JSON_EXTRACT", String.class, root.get("txHash"), cb.literal("$"))
        ));

        TypedQuery<EntryDto> query = em.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize());

        List<EntryDto> resultList = query.getResultList();

        // ======= Bulk fetch approvals & approvers =======

        if (!resultList.isEmpty()) {
            if (includeApproval) {
                // Pre-size maps to reduce rehashing
                int size = resultList.size();
                List<Long> ids = new ArrayList<>(size);
                for (EntryDto dto : resultList) ids.add(dto.getId());

                // Fetch EntryApproval in bulk
                List<Object[]> approvalRows = em.createQuery(
                                "SELECT a.entryId, a.tierId, a FROM EntryApproval a WHERE a.entryId IN :ids", Object[].class)
                        .setParameter("ids", ids)
                        .getResultList();

                Map<Long, Map<Long, EntryApproval>> approvals = new HashMap<>(size);
                for (Object[] row : approvalRows) {
                    Long entryId = (Long) row[0];
                    Long tier = (Long) row[1];
                    EntryApproval appr = (EntryApproval) row[2];
                    approvals.computeIfAbsent(entryId, k -> new HashMap<>(approvalRows.size() / size + 1))
                            .put(tier, appr);
                }

                // Fetch approvers in bulk
//            List<Object[]> approverRows = em.createQuery(
//                            "SELECT e.id, KEY(ap), ap FROM Entry e JOIN e.approver ap WHERE e.id IN :ids", Object[].class)
//                    .setParameter("ids", ids)
//                    .getResultList();
//
//            Map<Long, Map<Long, String>> approvers = new HashMap<>(size);
//            for (Object[] row : approverRows) {
//                Long entryId = (Long) row[0];
//                Long tier = (Long) row[1];
//                String name = (String) row[2];
//                approvers.computeIfAbsent(entryId, k -> new HashMap<>(approverRows.size() / size + 1))
//                        .put(tier, name);
//            }

                // Set into DTOs
                for (EntryDto dto : resultList) {
                    dto.setApproval(approvals.getOrDefault(dto.getId(), Map.of()));
//                dto.setApprover(approvers.getOrDefault(dto.getId(), Map.of()));
                }
            }
        }

        // ======= COUNT QUERY =======
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Entry> countRoot = countQuery.from(Entry.class);
        Predicate countPredicate = (spec != null) ? spec.toPredicate(countRoot, countQuery, cb) : cb.conjunction();
        countQuery.select(cb.count(countRoot)).where(countPredicate);
        Long total = em.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(resultList, pageable, total);
    }
    public Page<EntryDto> findDataPaged(Specification<Entry> spec, Map<String, Set<String>> fields, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<EntryDto> cq = cb.createQuery(EntryDto.class);
        Root<Entry> root = cq.from(Entry.class);

        // Apply predicates from Specification
        Predicate predicate = (spec != null) ? spec.toPredicate(root, cq, cb) : cb.conjunction();
        cq.where(predicate);

        // Apply ORDER BY
        // Apply ORDER BY from specification
        if (cq.getOrderList().isEmpty() && pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            pageable.getSort().forEach(order -> {
                Path<Object> path = root.get(order.getProperty());
                orders.add(order.isAscending() ? cb.asc(path) : cb.desc(path));
            });
            cq.orderBy(orders);
        }

        Join<Entry, Entry> prevEntryJoin = root.join("prevEntry", JoinType.LEFT);
        Expression<String> dataJson;
        Expression<Object> prevJson;
        if (fields == null || fields.isEmpty()) {
            dataJson = cb.function("JSON_EXTRACT", String.class, root.get("data"), cb.literal("$"));

            prevJson = cb.selectCase()
                    .when(cb.isNotNull(prevEntryJoin.get("id")),
                            cb.function("JSON_EXTRACT", String.class, prevEntryJoin.get("data"), cb.literal("$"))
                    )
                    .otherwise(cb.nullLiteral(String.class));
        }else{
            // Build JSON field selections
            List<Expression<?>> dataJsonArgs = new ArrayList<>();
            for (String key : fields.getOrDefault("$", Collections.emptySet())) {
                dataJsonArgs.add(cb.literal(key));
                dataJsonArgs.add(cb.function("JSON_EXTRACT", Object.class, root.get("data"), cb.literal("$." + key)));
            }
            dataJson = cb.function(
                    "JSON_MERGE_PATCH",
                    String.class,
                    cb.literal("{}"),
                    cb.function("JSON_OBJECT", String.class, dataJsonArgs.toArray(new Expression[0]))
            );

            List<Expression<?>> prevJsonArgs = new ArrayList<>();
            for (String key : fields.getOrDefault("$prev$", Collections.emptySet())) {
                prevJsonArgs.add(cb.literal(key));
                prevJsonArgs.add(cb.function("JSON_EXTRACT", Object.class, prevEntryJoin.get("data"), cb.literal("$." + key)));
            }
            prevJson = cb.selectCase()
                    .when(cb.isNotNull(prevEntryJoin.get("id")),
                            cb.function(
                                    "JSON_MERGE_PATCH",
                                    String.class,
                                    cb.literal("{}"),
                                    cb.function("JSON_OBJECT", String.class, prevJsonArgs.toArray(new Expression[0]))
                            )
                    )
                    .otherwise(cb.nullLiteral(String.class));
        }

        cq.select(cb.construct(
                EntryDto.class,
                root.get("id"),
                dataJson,
                prevJson
        ));

        TypedQuery<EntryDto> query = em.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize());

        List<EntryDto> resultList = query.getResultList();

               // ======= COUNT QUERY =======
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Entry> countRoot = countQuery.from(Entry.class);
        Predicate countPredicate = (spec != null) ? spec.toPredicate(countRoot, countQuery, cb) : cb.conjunction();
        countQuery.select(cb.count(countRoot)).where(countPredicate);
        Long total = em.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(resultList, pageable, total);
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
