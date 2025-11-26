package com.benzourry.leap.repository;

import com.benzourry.leap.model.Entry;
import com.benzourry.leap.model.EntryApproval;
import com.benzourry.leap.model.EntryDto;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
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

        return em.createQuery(query)
                .setHint(org.hibernate.jpa.HibernateHints.HINT_READ_ONLY, true)
                .getResultStream();
    }

    /// Slowest
    /*
    public Page<EntryDto> findPagedOld3(Specification<Entry> spec, Map<String, Set<String>> fields, boolean includeApproval, Pageable pageable) {
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
        Expression<String> prevJson;
        if (fields == null || fields.isEmpty()) {
            dataJson = cb.function("JSON_EXTRACT", String.class, root.get("data"), cb.literal("$"));

            prevJson = cb.<String>selectCase()
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
            prevJson = cb.<String>selectCase()
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

        if (includeApproval && !resultList.isEmpty()) {
                // Pre-size maps to reduce rehashing
            int N = resultList.size();
            List<Long> ids = new ArrayList<>(N);
            for (EntryDto dto : resultList) ids.add(dto.getId());

            int approvalMapSize = Math.max((int) (N / 0.75f), 16);
            Map<Long, Map<Long, EntryApproval>> approvals = new HashMap<>(approvalMapSize);

            if (ids.size() < 1000) {
                // use getResultList()
                // Fetch EntryApproval in bulk
                List<Object[]> approvalRows = em.createQuery(
                                "SELECT a.entryId, a.tierId, a FROM EntryApproval a WHERE a.entryId IN :ids", Object[].class)
                        .setParameter("ids", ids)
                        .getResultList();

                for (Object[] row : approvalRows) {
                    Long entryId = (Long) row[0];
                    Long tier = (Long) row[1];
                    EntryApproval appr = (EntryApproval) row[2];
                    approvals.computeIfAbsent(entryId, k -> new HashMap<>())
                            .put(tier, appr);
                }
            } else {
                // use Hibernate stream + fetch size
                org.hibernate.query.Query<Object[]> streamQuery = em.createQuery(
                                "SELECT a.entryId, a.tierId, a FROM EntryApproval a WHERE a.entryId IN :ids", Object[].class)
                        .setParameter("ids", ids)
                        .unwrap(org.hibernate.query.Query.class)   // unwrap to Hibernate Query
                        .setFetchSize(50);                         // fetch 50 rows at a time

                // Stream results using Hibernate ScrollableResults
                try (java.util.stream.Stream<Object[]> stream = streamQuery.stream()) {
                    stream.forEach(row -> {
                        Long entryId = (Long) row[0];
                        Long tierId = (Long) row[1];
                        EntryApproval appr = (EntryApproval) row[2];

                        approvals.computeIfAbsent(entryId, k -> new HashMap<>())
                                .put(tierId, appr);
                    });
                }
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
            final Map<Long, EntryApproval> EMPTY_APPROVAL = Map.of();
            for (EntryDto dto : resultList) {
                dto.setApproval(approvals.getOrDefault(dto.getId(), EMPTY_APPROVAL));
//                dto.setApprover(approvers.getOrDefault(dto.getId(), Map.of()));
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
    */
    // Balanced
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
        Expression<String> prevJson;
        if (fields == null || fields.isEmpty()) {
            dataJson = cb.function("JSON_EXTRACT", String.class, root.get("data"), cb.literal("$"));

            prevJson = cb.<String>selectCase()
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
            prevJson = cb.<String>selectCase()
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

        if (includeApproval && !resultList.isEmpty()) {
                // Pre-size maps to reduce rehashing
            int N = resultList.size();
            List<Long> ids = new ArrayList<>(N);
            for (EntryDto dto : resultList) ids.add(dto.getId());

            int approvalMapSize = Math.max((int) (N / 0.75f), 16);
            Map<Long, Map<Long, EntryApproval>> approvals = new HashMap<>(approvalMapSize);

                // use getResultList()
                // Fetch EntryApproval in bulk
            List<Object[]> approvalRows = em.createQuery(
                            "SELECT a.entryId, a.tierId, a FROM EntryApproval a WHERE a.entryId IN :ids", Object[].class)
                    .setParameter("ids", ids)
                    .getResultList();

            for (Object[] row : approvalRows) {
                Long entryId = (Long) row[0];
                Long tier = (Long) row[1];
                EntryApproval appr = (EntryApproval) row[2];
                approvals.computeIfAbsent(entryId, k -> new HashMap<>())
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
            final Map<Long, EntryApproval> EMPTY_APPROVAL = Map.of();
            for (EntryDto dto : resultList) {
                dto.setApproval(approvals.getOrDefault(dto.getId(), EMPTY_APPROVAL));
//                dto.setApprover(approvers.getOrDefault(dto.getId(), Map.of()));
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


//    Fastest
    /*
    public Page<EntryDto> findPagedOld1(
            Specification<Entry> spec,
            Map<String, Set<String>> fields,
            boolean includeApproval,
            Pageable pageable
    ) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<Entry> root = cq.from(Entry.class);

        // ====== WHERE ======
        Predicate predicate = (spec != null)
                ? spec.toPredicate(root, cq, cb)
                : cb.conjunction();
        cq.where(predicate);

        // ====== ORDER BY ======
        if (cq.getOrderList().isEmpty() && pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            pageable.getSort().forEach(order -> {
                Path<Object> path = root.get(order.getProperty());
                orders.add(order.isAscending() ? cb.asc(path) : cb.desc(path));
            });
            cq.orderBy(orders);
        }

        // ====== PREV JOIN ======
        Join<Entry, Entry> prevEntryJoin = root.join("prevEntry", JoinType.LEFT);

        // ------------------------------------------------------------------------------------
        // JSON FIELD HANDLING — min allocations, no arg arrays
        // ------------------------------------------------------------------------------------

        Expression<String> dataJson;
        Expression<String> prevJson;

        if (fields == null || fields.isEmpty()) {

            dataJson = cb.function("JSON_EXTRACT", String.class,
                    root.get("data"), cb.literal("$"));

            prevJson = cb.<String>selectCase()
                    .when(cb.isNotNull(prevEntryJoin.get("id")),
                            cb.function("JSON_EXTRACT", String.class,
                                    prevEntryJoin.get("data"), cb.literal("$"))
                    )
                    .otherwise(cb.nullLiteral(String.class));

        } else {

            // ====== DATA JSON ======
            List<Expression<?>> dataArgs = new ArrayList<>();
            for (String key : fields.getOrDefault("$", Set.of())) {
                dataArgs.add(cb.literal(key));
                dataArgs.add(cb.function("JSON_EXTRACT", Object.class,
                        root.get("data"), cb.literal("$." + key)));
            }

            dataJson = cb.function("JSON_MERGE_PATCH",
                    String.class,
                    cb.literal("{}"),
                    cb.function("JSON_OBJECT", String.class,
                            dataArgs.toArray(new Expression<?>[0])));

            // ====== PREV JSON ======
            List<Expression<?>> prevArgs = new ArrayList<>();
            for (String key : fields.getOrDefault("$prev$", Set.of())) {
                prevArgs.add(cb.literal(key));
                prevArgs.add(cb.function("JSON_EXTRACT", Object.class,
                        prevEntryJoin.get("data"), cb.literal("$." + key)));
            }

            prevJson = cb.<String>selectCase()
                    .when(cb.isNotNull(prevEntryJoin.get("id")),
                            cb.function("JSON_MERGE_PATCH",
                                    String.class,
                                    cb.literal("{}"),
                                    cb.function("JSON_OBJECT", String.class,
                                            prevArgs.toArray(new Expression<?>[0])))
                    )
                    .otherwise(cb.nullLiteral(String.class));
        }

        // ------------------------------------------------------------------------------------
        // SELECT TUPLE (no cb.construct → no reflection → saves 30–50% memory)
        // ------------------------------------------------------------------------------------
        cq.multiselect(
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
        );

        TypedQuery<Tuple> query = em.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize());

        List<Tuple> tuples = query.getResultList();

        // ------------------------------------------------------------------------------------
        // MAP → DTO (no reflection, direct constructor)
        // ------------------------------------------------------------------------------------
        List<EntryDto> resultList = new ArrayList<>(tuples.size());
        for (Tuple t : tuples) {
            EntryDto dto = new EntryDto(
                    t.get(0, Long.class),          // id
                    t.get(1, String.class),        // data
                    t.get(2, String.class),        // prev
                    t.get(3, Long.class),          // formId
                    t.get(4, Integer.class),       // currentTier
                    t.get(5, String.class),        // currentStatus
                    t.get(6, Long.class),          // currentTierId
                    t.get(7, Long.class),          // finalTierId
                    t.get(8, Date.class),          // submissionDate
                    t.get(9, Date.class),          // resubmissionDate
                    t.get(10, Boolean.class),      // currentEdit
                    t.get(11, Date.class),         // createdDate
                    t.get(12, String.class),       // createdBy
                    t.get(13, Date.class),         // modifiedDate
                    t.get(14, String.class),       // modifiedBy
                    t.get(15, Boolean.class),      // live
                    t.get(16, String.class),       // email
                    t.get(17, String.class)        // txHash JSON
            );
            resultList.add(dto);
        }

        // ------------------------------------------------------------------------------------
        // BULK FETCH APPROVALS — streaming, no List<Object[]> allocated
        // ------------------------------------------------------------------------------------
        if (includeApproval && !resultList.isEmpty()) {

            int N = resultList.size();
            List<Long> ids = new ArrayList<>(N);
            for (EntryDto dto : resultList) ids.add(dto.getId());

            // Pre-size top-level map
            int approvalMapSize = Math.max((int) (N / 0.75f), 16);
            Map<Long, Map<Long, EntryApproval>> approvals = new HashMap<>(approvalMapSize);

            em.createQuery(
                            "SELECT a.entryId, a.tierId, a FROM EntryApproval a WHERE a.entryId IN :ids",
                            Object[].class
                    )
                    .setParameter("ids", ids)
                    .getResultStream()
                    .forEach(row -> {
                        Long entryId = (Long) row[0];
                        Long tierId = (Long) row[1];
                        EntryApproval appr = (EntryApproval) row[2];

                        approvals
                                .computeIfAbsent(entryId, k -> new HashMap<>())
                                .put(tierId, appr);
                    });

            // Assign to DTOs
            final Map<Long, EntryApproval> EMPTY_APPROVAL = Map.of();
            for (EntryDto dto : resultList) {
                dto.setApproval(approvals.getOrDefault(dto.getId(), EMPTY_APPROVAL));
            }
        }

        // ------------------------------------------------------------------------------------
        // COUNT QUERY
        // ------------------------------------------------------------------------------------
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Entry> countRoot = countQuery.from(Entry.class);

        Predicate countPredicate = (spec != null)
                ? spec.toPredicate(countRoot, countQuery, cb)
                : cb.conjunction();

        countQuery.select(cb.count(countRoot)).where(countPredicate);

        long total = em.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(resultList, pageable, total);
    }
    */

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
        Expression<String> prevJson;
        if (fields == null || fields.isEmpty()) {
            dataJson = cb.function("JSON_EXTRACT", String.class, root.get("data"), cb.literal("$"));

            prevJson = cb.<String>selectCase()
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
            prevJson = cb.<String>selectCase()
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
