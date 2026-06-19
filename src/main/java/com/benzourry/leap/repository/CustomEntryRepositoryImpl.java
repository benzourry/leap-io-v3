package com.benzourry.leap.repository;

import com.benzourry.leap.model.*;
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

        return em.createQuery(query)
                .setHint(org.hibernate.jpa.HibernateHints.HINT_READ_ONLY, true)
                .getResultStream();
    }

    // Balanced
    public Page<EntryDto> findPaged(Form form, Specification<Entry> spec, Map<String, Set<String>> fields, boolean includeApproval, Pageable pageable) {
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
                    "JSON_MERGE_PATCH", // what is reason for using JSON_MERGE_PATCH instead of JSON_OBJECT? Is it because of null value handling
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

        // ======= Populate currentStatusText =======
//        if (form != null && !resultList.isEmpty()) {
//            for (EntryDto dto : resultList) {
//                dto.setCurrentStatusText(__computeStatusText(form, dto.getCurrentTier(), dto.getCurrentStatus()));
//            }
//        }


        if (form != null && !resultList.isEmpty()) {
            for (EntryDto dto : resultList) {
                // 1. Calculate the text
                String statusText = __computeStatusText(form, dto.getCurrentTier(), dto.getCurrentStatus());

                // 2. Inject it directly into the 'data' JSON payload
                if (dto.getData() != null && dto.getData().isObject()) {
                    ((com.fasterxml.jackson.databind.node.ObjectNode) dto.getData())
                            .put("$statusText", statusText);
                }

                // (Optional) You can still set it on the DTO field as well,
                // or you can delete the field from your DTO class entirely now!
                dto.setCurrentStatusText(statusText);
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


    private String __computeStatusText(Form form, Integer currentTier, String currentStatus) {
        if (currentStatus == null || currentStatus.isEmpty()) {
            return currentStatus;
        }

        // 1. Format system statuses cleanly upfront (e.g., "always_approve" -> "Approved")
        String cleanStatus;
        if ("always_approve".equalsIgnoreCase(currentStatus)) {
            cleanStatus = "Approved"; // Translates system code to a human-friendly label
        } else {
            // Capitalizes "submitted", "drafted", "returned" -> "Submitted", "Drafted", "Returned"
            cleanStatus = currentStatus.substring(0, 1).toUpperCase() + currentStatus.substring(1).toLowerCase();
        }

        try {
            // 2. Ensure form and tiers exist, and currentTier is valid
            if (form != null && form.getTiers() != null && currentTier != null && currentTier >= 0 && !form.getTiers().isEmpty()) {

                // Cap the index at the last tier for completed workflows
                int effectiveTierIndex = currentTier;
                if (effectiveTierIndex >= form.getTiers().size()) {
                    effectiveTierIndex = form.getTiers().size() - 1;
                }

                var tier = form.getTiers().get(effectiveTierIndex);

                if (tier != null) {
                    String tierName = tier.getName() != null ? tier.getName() : "Unknown Tier";

                    // Start with our cleanly formatted system status
                    String statusLabel = cleanStatus;

                    // 3. IF it's a custom action, override the system label with the mapped TierAction label
                    if (tier.getActions() != null && tier.getActions().containsKey(currentStatus)) {
                        var action = tier.getActions().get(currentStatus);
                        if (action != null && action.getLabel() != null && !action.getLabel().isEmpty()) {
                            statusLabel = action.getLabel();
                        }
                    }

                    return statusLabel + " (" + tierName + ")";
                }
            }
        } catch (Exception e) {
            // Failsafe: Catch any unexpected mapping failures safely
        }

        // 4. Ultimate Fallback (If tier logic completely fails or currentTier is null)
        return cleanStatus;
    }

    public Page<EntryDto> findDataPaged(Specification<Entry> spec, Map<String, Set<String>> fields, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<EntryDto> cq = cb.createQuery(EntryDto.class);
        Root<Entry> root = cq.from(Entry.class);

        // Apply predicates from Specification
        Predicate predicate = (spec != null) ? spec.toPredicate(root, cq, cb) : cb.conjunction();
        cq.where(predicate);

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
