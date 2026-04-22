package com.benzourry.leap.filter;

import com.benzourry.leap.model.Entry;
import com.benzourry.leap.model.EntryApproval;
import com.benzourry.leap.model.Form;
import com.benzourry.leap.model.Tier;
import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.OptionalBooleanBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.criteria.*;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Builder
public class EntryFilter {

    private static final Logger logger = LoggerFactory.getLogger(EntryFilter.class);

    // Optimized to static final to prevent memory recreation per request
    private static final List<String> LOOKUP_TYPES = List.of("select", "radio");
    private static final List<String> DATE_NUMBER_TYPES = List.of("date", "number", "scale", "scaleTo10", "scaleTo5");
    private static final List<String> CHECKBOX_TYPES = List.of("checkbox");
    private static final List<String> TEXT_TYPES = List.of("text");

    Long formId;
    String searchText;
    String email;
    String approver;
    String admin;
    Date submissionDateFrom;
    Date submissionDateTo;
    List<String> sort;
    Map<String, Object> filters;
    Map<String, String> status;
    Form form;
    boolean action;
    List<Long> ids;
    JsonNode qBuilder;

    @Builder.Default
    String cond = "AND";

    Map<String, Object> dataMap;

    public Specification<Entry> filter() {

        return (root, cq, cb) -> {

            Join<Entry, Entry> mapJoinPrev = root.join("prevEntry", JoinType.LEFT);

            List<Predicate> predicates = new OptionalBooleanBuilder(cb)
                    .notNullAnd(searchText, cb.or(
                            cb.isNotNull(cb.function("JSON_SEARCH", String.class,
                                    cb.lower(cb.function("JSON_UNQUOTE", String.class, root.get("data"))),
                                    cb.literal("all"),
                                    cb.lower(cb.literal("%" + searchText + "%")))),
                            cb.isNotNull(cb.function("JSON_SEARCH", String.class,
                                    cb.lower(cb.function("JSON_UNQUOTE", String.class, mapJoinPrev.get("data"))),
                                    cb.literal("all"),
                                    cb.lower(cb.literal("%" + searchText + "%"))))
                    ))
                    .notNullAnd(submissionDateFrom, cb.greaterThan(root.get("submissionDate"), submissionDateFrom))
                    .notNullAnd(submissionDateTo, cb.lessThan(root.get("submissionDate"), submissionDateTo))
                    .notNullAnd(email, cb.like(cb.concat(",", cb.concat(
                            cb.function("REPLACE", String.class, root.get("email"), cb.literal(" "), cb.literal(""))
                            , ",")), "%," + email + ",%"))
                    .notNullAnd(admin, cb.like(cb.concat(",", cb.concat(
                            cb.function("REPLACE", String.class, root.get("email"), cb.literal(" "), cb.literal(""))
                            , ",")), "%," + admin + ",%"))
                    .notNullAnd(formId, cb.equal(root.get("form").get("id"), formId))
                    .build();

            // dev mode should be able to access to the live data for troubleshooting
            if (form.isLive()) { // if live , only fetch live=true
                predicates.add(cb.equal(root.get("live"), form.isLive()));
            } else if (form.getApp() != null && form.getApp().getX() != null && !form.getApp().getX().at("/devInData").asBoolean(false)) {
                // if devInData=null or devInData=false (default value)
                predicates.add(cb.equal(root.get("live"), form.isLive()));
                //else, dont add predicate, fetch everything
            }

            if (!CollectionUtils.isEmpty(ids)) {
                predicates.add(root.get("id").in(ids));
            }

            /* If type action, join dgn tier, approver then polah condition entry.tierId.approver = approver*/
            if (action) {
                Join<Entry, Form> mapJoinForm = root.join("form");
                Join<Form, Tier> mapJoinTier = mapJoinForm.join("tiers", JoinType.LEFT);
                MapJoin<Entry, Long, String> mapJoinApprover = root.joinMap("approver", JoinType.LEFT);

                //if type == 'ALL'
                //predicates.add(cb.equal(mapJoinTier.get("type"),"FIXED"));
                predicates.add(cb.equal(root.get("currentTier"), mapJoinTier.get("sortOrder")));
                predicates.add(
                        cb.or(cb.equal(mapJoinTier.get("type"), "ALL"),
                                cb.and(mapJoinTier.get("type").in("FIXED", "DYNAMIC", "ASSIGN", "GROUP"),
                                        cb.equal(mapJoinApprover.key(), mapJoinTier.get("id")),
                                        cb.like(cb.concat(",", cb.concat(
                                                cb.function("REGEXP_REPLACE", String.class, mapJoinApprover, cb.literal("[\r\n ]"), cb.literal(""))
                                                , ",")), "%," + approver + ",%")
                                )
                        )
                );
            }

            if (status != null && status.values().stream().anyMatch(val -> val != null && !val.isEmpty())) {
                // 1. FAST PRE-CHECK: Do we have at least one non-null, non-empty value?
                List<Predicate> statusFilterPred = new ArrayList<>();
                status.forEach((s, val) -> {
                    if (val != null && !val.isEmpty()) {
                        if ("-1".equals(s)) {
                            // utk drafted,submitted (awal2) currentTierId = null
                            // Problem bila entry dh pernah lalu approval (or submitted using codes, etc. Perlu investigate gk.
                            // For now, remove condition currentTierId=null
                            // Masalah nya akan akan captured juak if ada status dlm tier pake 'submitted'
                            statusFilterPred.add(cb.and(cb.isNull(root.get("currentTierId")),
                                    root.get("currentStatus").in((Object[]) val.split(","))));
                        } else {
                            // status=submitted + currentTier = approved
                            statusFilterPred.add(cb.and(cb.equal(root.get("currentTierId"), s),
                                    root.get("currentStatus").in((Object[]) val.split(","))));
                        }
                    }
                });
                predicates.add(cb.or(statusFilterPred.toArray(new Predicate[0])));
            }

            List<Predicate> paramPredicates = new ArrayList<>();

            // Build param predicate
            if (filters != null) {
                if (qBuilder != null && !qBuilder.isEmpty()) {
                    Set<String> _filtersKey = new HashSet<>(filters.keySet());
                    Predicate pred = qWalker(root, cb, mapJoinPrev, "$and", qBuilder, _filtersKey);

                    if (pred != null) {
                        paramPredicates.add(pred);
                    }

                    // _filterKey ialah semua keyset. Bila qWalker, akan remove key dari keyset
                    // balance key should still be evaluated
                    _filtersKey.forEach(fk -> paramPredicates.add(createPredicate(root, cb, mapJoinPrev, fk)));
                } else {
                    // make sure prefixed with "$". Weirdly, including filter without "$" will add or 1=1 to the condition (????)
                    for (String f : filters.keySet()) {
                        if (f.startsWith("$")) {
                            paramPredicates.add(createPredicate(root, cb, mapJoinPrev, f));
                        }
                    }
                }
            }

            cq.distinct(true);

            List<Order> orders = new ArrayList<>();
            if (sort != null) {
                sort.forEach(s -> {
                    String[] splitted = s.split("~"); // ['$.category.name','asc']
                    String dir = splitted.length == 2 ? splitted[1] : "asc";

                    if (s.contains("$")) {
                        String[] col = splitted[0].split("\\.", 2); //['$','category.name']
                        String fieldFull = col[1]; // 'category.name';
                        String fieldCode = fieldFull.split("\\.")[0]; // 'category'

                        Path<?> pred = root.get("data");
                        Form lForm = form;

                        if ("$prev$".equals(col[0])) {
                            lForm = form != null ? form.getPrev() : null;
                            pred = mapJoinPrev.get("data");
                        }

                        Expression<?> jsonValueExpression = null;

                        // process
                        if (lForm != null && lForm.getItems() != null && lForm.getItems().containsKey(fieldCode)) {
                            String fieldType = lForm.getItems().get(fieldCode).getType();

                            if (DATE_NUMBER_TYPES.contains(fieldType)) {
                                jsonValueExpression = cb.function("JSON_VALUE", Double.class, pred, cb.literal("$." + fieldFull));
                            } else {
                                jsonValueExpression = cb.function("JSON_VALUE", String.class, pred, cb.literal("$." + fieldFull));
                            }
                        } else if (List.of("$id", "$counter").contains(fieldCode)) {
                            jsonValueExpression = cb.function("JSON_VALUE", Double.class, pred, cb.literal("$." + fieldFull));
                        } else if (List.of("$code").contains(fieldCode)) {
                            jsonValueExpression = cb.function("JSON_VALUE", String.class, pred, cb.literal("$." + fieldFull));
                        }

                        if (jsonValueExpression != null) {
                            orders.add("asc".equals(dir) ? cb.asc(jsonValueExpression) : cb.desc(jsonValueExpression));
                        }
                    } else {
                        // process
                        orders.add("asc".equals(dir) ? cb.asc(root.get(splitted[0])) : cb.desc(root.get(splitted[0])));
                    }
                });

                if (!orders.isEmpty()) {
                    cq.orderBy(orders);
                }
            }

            Predicate params = "OR".equals(cond)
                    ? cb.or(paramPredicates.toArray(new Predicate[0]))
                    : cb.and(paramPredicates.toArray(new Predicate[0]));

            return cb.and(cb.and(predicates.toArray(new Predicate[0])), params);
        };
    }

    private Predicate createPredicate(Root<Entry> root, CriteriaBuilder cb, Join<Entry, Entry> mapJoinPrev, String f) {
        Object rawFilterObj = filters.get(f);
        if (rawFilterObj == null) {
            return cb.conjunction(); // RETURN EARLY
        }

        List<Predicate> paramPredicates = new ArrayList<>();
        String filterValue = String.valueOf(rawFilterObj);
        String[] splitted1 = f.split("\\.");
        String rootCol = splitted1[0]; // data or prev

        Path<?> predRoot = null;
        String fieldFull = "";
        String fieldCode = "";
        Form targetForm = null;
        String logContext = getLogContext(f); // Centralized logging string

        switch (rootCol) {
            case "$$":
            case "$":
            case "$prev$": {
                String[] splitted = f.split("\\.", "$$".equals(rootCol) ? 3 : 2);

                if ("$$".equals(rootCol)) {
                    // ie: $$.213.category.name~contain=abc
                    // ['$$', '213', 'category.name~contain']
                    long tierId = Long.parseLong(splitted[1]); // 213
                    fieldFull = splitted[2]; // category.name~contain
                    fieldCode = fieldFull.split("\\.")[0].split("~")[0]; // category
                    MapJoin<Entry, Long, EntryApproval> mapJoin = root.joinMap("approval", JoinType.LEFT);
                    predRoot = mapJoin.get("data");
                    paramPredicates.add(cb.equal(mapJoin.key(), tierId));
                    targetForm = this.form;
                } else {
                    // ie: $.category.name~contain=abc or $prev$.category.name~contain=abc
                    // ['$', 'category.name~contain']
                    fieldFull = splitted[1]; // category.name~contain
                    fieldCode = fieldFull.split("\\.")[0].split("~")[0]; // category
                    predRoot = "$".equals(rootCol) ? root.get("data") : mapJoinPrev.get("data");
                    targetForm = "$".equals(rootCol) ? this.form : (this.form != null ? this.form.getPrev() : null);
                }

                if (fieldFull.contains("*")) {
                    if (fieldFull.contains("~")) {
                        String[] fieldFullSplitted = fieldFull.split("~");
                        String fieldTranslated = fieldFullSplitted[0].replace("*", "[*]");
                        String[] fieldValueSplitted = filterValue.split(",");
                        List<Predicate> listOverlapPredicateList = new ArrayList<>();
                        for (String value : fieldValueSplitted) {
                            Expression<String> jsonValueListSearch = cb.function("JSON_SEARCH", String.class,
                                    cb.lower(predRoot.as(String.class)), cb.literal("one"), cb.literal(value.toLowerCase()),
                                    cb.nullLiteral(String.class), cb.literal("$." + fieldTranslated));
                            listOverlapPredicateList.add(cb.isNotNull(jsonValueListSearch));
                        }
                        paramPredicates.add(cb.or(listOverlapPredicateList.toArray(new Predicate[0])));
                    } else {
                        String fieldTranslated = fieldFull.replace("*", "[*]");
                        Expression<String> jsonValueListSearch = cb.function("JSON_SEARCH", String.class,
                                cb.lower(predRoot.as(String.class)), cb.literal("one"), cb.literal(filterValue.toLowerCase()),
                                cb.nullLiteral(String.class), cb.literal("$." + fieldTranslated));
                        paramPredicates.add(cb.isNotNull(jsonValueListSearch));
                    }
                } else if (targetForm != null && targetForm.getItems() != null && targetForm.getItems().containsKey(fieldCode)) {
                    String[] splitField = fieldFull.split("~");
                    Expression<String> jsonValueString = cb.function("JSON_VALUE", String.class, predRoot, cb.literal("$." + splitField[0]));
                    Expression<Double> jsonValueDouble = cb.function("JSON_VALUE", Double.class, predRoot, cb.literal("$." + splitField[0]));

                    if ("~null".equals(filterValue)) {
                        paramPredicates.add(cb.upper(jsonValueString).isNull());
                    } else if ("~notnull".equals(filterValue)) {
                        paramPredicates.add(cb.upper(jsonValueString).isNotNull());
                    } else {
                        String fieldType = targetForm.getItems().get(fieldCode).getType();

                        if (LOOKUP_TYPES.contains(fieldType)) {
                            if (fieldFull.contains("~")) {
                                applyDecoratorPredicate(cb, paramPredicates, splitField[1], filterValue, jsonValueString, jsonValueDouble, false, logContext);
                            } else {
                                paramPredicates.add(cb.like(cb.upper(jsonValueString), filterValue.toUpperCase()));
                            }
                        } else if (DATE_NUMBER_TYPES.contains(fieldType) || List.of("$id", "$counter").contains(fieldCode)) {
                            if (!filterValue.isEmpty()) {
                                if (fieldFull.contains("~")) {
                                    applyDecoratorPredicate(cb, paramPredicates, splitField[1], filterValue, null, jsonValueDouble, false, logContext);
                                } else {
                                    try {
                                        paramPredicates.add(cb.equal(jsonValueDouble, Double.parseDouble(filterValue)));
                                    } catch (NumberFormatException e) {
                                        logger.error("Error parsing value for " + logContext + ": " + filterValue);
                                    }
                                }
                            }
                        } else if (CHECKBOX_TYPES.contains(fieldType)) {
                            Expression<Boolean> jsonValueBoolean = cb.function("JSON_VALUE", Boolean.class, predRoot, cb.literal("$." + splitField[0]));
                            paramPredicates.add(Boolean.parseBoolean(filterValue)
                                    ? cb.isTrue(jsonValueBoolean)
                                    : cb.or(cb.isFalse(jsonValueBoolean), cb.isNull(jsonValueBoolean)));
                        } else if (TEXT_TYPES.contains(fieldType)) {
                            if (fieldFull.contains("~")) {
                                applyDecoratorPredicate(cb, paramPredicates, splitField[1], filterValue, jsonValueString, null, true, logContext);
                            } else {
                                String searchText = "input".equals(targetForm.getItems().get(fieldCode).getSubType()) ? filterValue : "%" + filterValue + "%"; // exact match
                                paramPredicates.add(cb.like(cb.upper(jsonValueString), searchText.toUpperCase()));
                            }
                        } else {
                            // Cannot determine type
                            if (fieldFull.contains("~")) {
                                applyDecoratorPredicate(cb, paramPredicates, splitField[1], filterValue, jsonValueString, jsonValueDouble, false, logContext);
                            } else {
                                paramPredicates.add(cb.like(cb.lower(jsonValueString), filterValue.toLowerCase()));
                            }
                        }
                    }
                } else {
                    // Not part of form
                    paramPredicates.add(cb.like(cb.upper(cb.function("JSON_VALUE", String.class, predRoot, cb.literal("$." + fieldFull))), filterValue.toUpperCase()));
                }
                break;
            }
            case "$$_": {
                // ie: $$_.213.timestamp~between=1609459200000,1640995200000
                String[] splitted = f.split("\\.", 3); // ['$$_', '213', 'timestamp~between']
                long tierId = Long.parseLong(splitted[1]); // 213
                fieldFull = splitted[2]; // timestamp~between
                String[] splitField = fieldFull.split("~"); // ['timestamp', 'between']
                fieldCode = splitField[0].split("\\.")[0]; // timestamp

                MapJoin<Entry, Long, EntryApproval> mapJoin = root.joinMap("approval", JoinType.LEFT);
                paramPredicates.add(cb.equal(mapJoin.key(), tierId));

                switch (fieldCode) {
                    case "timestamp":
                        if (!filterValue.isEmpty()) {
                            if (fieldFull.contains("~")) {
                                applyDecoratorPredicate(cb, paramPredicates, splitField[1], filterValue, null, mapJoin.get(fieldCode).as(Double.class), false, logContext);
                            } else {
                                try {
                                    paramPredicates.add(cb.equal(mapJoin.get(fieldCode), Double.parseDouble(filterValue)));
                                } catch (NumberFormatException e) {
                                    logger.error("Error parsing value for " + logContext + ": " + filterValue);
                                }
                            }
                        }
                        break;
                    case "status":
                        paramPredicates.add(cb.equal(mapJoin.get(fieldFull), filterValue));
                        break;
                    case "remark":
                        paramPredicates.add(cb.like(cb.upper(mapJoin.get(fieldFull).as(String.class)), ("%" + filterValue + "%").toUpperCase()));
                        break;
                    case "email":
                        paramPredicates.add(cb.like(cb.upper(mapJoin.get(fieldFull).as(String.class)), filterValue.toUpperCase()));
                        break;
                }
                break;
            }
            case "$_": {
                fieldCode = f.split("\\.")[1];
                String[] splitField = fieldCode.split("~");
                String baseField = splitField[0];

                switch (baseField) {
                    case "email":
                        paramPredicates.add(cb.equal(cb.upper(cb.trim(root.get("email"))), filterValue.trim().toUpperCase()));
                        break;
                    case "currentStatus":
                        paramPredicates.add(cb.like(cb.upper(root.get("currentStatus")), filterValue.toUpperCase()));
                        break;
                    case "id":
                    case "currentTier":
                    case "currentTierId":
                    case "currentEdit":
                        paramPredicates.add(cb.equal(root.get(fieldCode), rawFilterObj));
                        break;
                    case "submissionDate":
                    case "resubmissionDate":
                    case "modifiedDate":
                    case "createdDate":
                        if (!filterValue.isEmpty()) {
                            if (splitField.length > 1) {
                                applyDateDecoratorPredicate(cb, paramPredicates, splitField[1], filterValue, root.get(baseField).as(Date.class), logContext);
                            } else {
                                try {
                                    paramPredicates.add(cb.equal(root.get(baseField), new Date(Long.parseLong(filterValue))));
                                } catch (Exception e) {
                                    logger.error("Error processing date filter for " + logContext + ": " + e.getMessage());
                                }
                            }
                        }
                        break;
                }
                break;
            }
        }

        return cb.and(paramPredicates.toArray(new Predicate[0]));
    }

    private Predicate qWalker(Root<Entry> root, CriteriaBuilder cb, Join<Entry, Entry> mapJoinPrev, String cond, JsonNode qList, Set<String> keySet) {
        int estimatedSize = qList.size() > 0 ? qList.size() * 2 : 4;
        List<Predicate> predicateList = new ArrayList<>(estimatedSize);

        for (JsonNode jsonNode : qList) {
            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                JsonNode value = entry.getValue();

                switch (key) {
                    case "$and", "$or" -> {
                        // recursive combining
                        Predicate nestedPred = qWalker(root, cb, mapJoinPrev, key, value, keySet);
                        if (nestedPred != null) {
                            predicateList.add(nestedPred);
                        }
                    }
                    default -> {
                        // Only compute template once, skip nulls
                        if (!value.isNull()) {
                            filters.computeIfAbsent(key, k -> Helper.compileTpl(value.asText(""), dataMap));
                        }
                        predicateList.add(createPredicate(root, cb, mapJoinPrev, key));
                        keySet.remove(key);
                    }
                }
            }
        }

        if (predicateList.isEmpty()) {
            return null; // Return null so we don't accidentally append 1=1 to an OR clause!
        }

        Predicate[] arr = predicateList.toArray(new Predicate[0]);
        return "$or".equals(cond) ? cb.or(arr) : cb.and(arr);
    }

    private String getLogContext(String f) {
        if (this.form != null && this.form.getApp() != null) {
            return "[App:" + this.form.getApp().getAppPath() + "]->[Form:" + this.form.getId() + "]->[" + f + "]";
        }
        return "[" + f + "]";
    }

    private void applyDecoratorPredicate(CriteriaBuilder cb, List<Predicate> paramPredicates,
                                         String operator, String filterValue,
                                         Expression<String> stringExpr, Expression<Double> doubleExpr,
                                         boolean isTextType, String logContext) {
        try {
            switch (operator) {
                case "in":
                    if (isTextType && stringExpr != null) {
                        // IN operator for text is replaced with multiple LIKE operations to support wildcard
                        String[] patterns = Arrays.stream(filterValue.toUpperCase().split(",")).map(String::trim).toArray(String[]::new);
                        List<Predicate> likePredicates = new ArrayList<>();
                        for (String pattern : patterns) {
                            likePredicates.add(cb.like(cb.upper(stringExpr), pattern));
                        }
                        paramPredicates.add(cb.or(likePredicates.toArray(new Predicate[0])));
                    } else if (stringExpr != null) {
                        paramPredicates.add(cb.upper(stringExpr).in(Arrays.stream(filterValue.toUpperCase().split(",")).map(String::trim).toArray()));
                    }
                    break;
                case "notin":
                    if (stringExpr != null) {
                        paramPredicates.add(cb.not(cb.upper(stringExpr).in(Arrays.stream(filterValue.toUpperCase().split(",")).map(String::trim).toArray())));
                    }
                    break;
                case "contain":
                    if (stringExpr != null) {
                        paramPredicates.add(cb.like(cb.upper(stringExpr), "%" + filterValue.toUpperCase() + "%"));
                    }
                    break;
                case "notcontain":
                    if (stringExpr != null) {
                        paramPredicates.add(cb.notLike(cb.upper(stringExpr), "%" + filterValue.toUpperCase() + "%"));
                    }
                    break;
                case "from":
                    if (doubleExpr != null) paramPredicates.add(cb.greaterThanOrEqualTo(doubleExpr, Double.parseDouble(filterValue)));
                    break;
                case "to":
                    if (doubleExpr != null) paramPredicates.add(cb.lessThanOrEqualTo(doubleExpr, Double.parseDouble(filterValue)));
                    break;
                case "between":
                    if (doubleExpr != null) {
                        String[] time = filterValue.split(",");
                        paramPredicates.add(cb.between(doubleExpr, Double.parseDouble(time[0]), Double.parseDouble(time[1])));
                    }
                    break;
            }
        } catch (NumberFormatException nfe) {
            logger.error("Error parsing value for " + logContext + ": " + filterValue);
        } catch (Exception e) {
            logger.error("Error processing filter for " + logContext + ": " + e.getMessage());
        }
    }

    private void applyDateDecoratorPredicate(CriteriaBuilder cb, List<Predicate> paramPredicates,
                                             String operator, String filterValue,
                                             Expression<Date> dateExpr, String logContext) {
        try {
            switch (operator) {
                case "from":
                    paramPredicates.add(cb.greaterThanOrEqualTo(dateExpr, new Date(Long.parseLong(filterValue))));
                    break;
                case "to":
                    paramPredicates.add(cb.lessThanOrEqualTo(dateExpr, new Date(Long.parseLong(filterValue))));
                    break;
                case "between":
                    String[] time = filterValue.split(",");
                    paramPredicates.add(cb.between(dateExpr, new Date(Long.parseLong(time[0])), new Date(Long.parseLong(time[1]))));
                    break;
            }
        } catch (NumberFormatException nfe) {
            logger.error("Error parsing date value for " + logContext + ": " + filterValue);
        } catch (Exception e) {
            logger.error("Error processing date filter for " + logContext + ": " + e.getMessage());
        }
    }
}