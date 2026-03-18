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
//    Dataset dataset;
    boolean action;
    List<Long> ids;
    JsonNode qBuilder;
    String cond = "AND";

    Map<String, Object> dataMap;

//    String[][] root = new String[][]{{"$", "data"}, {"$prev$", "prev"}, {"$$", "approval", "$$_", "approval"}};

    final List<String> LOOKUP_TYPES = List.of("select", "radio");
    final List<String> DATE_NUMBER_TYPES = List.of("date", "number", "scale", "scaleTo10", "scaleTo5");
    final List<String> CHECKBOX_TYPES = List.of("checkbox");
    final List<String> TEXT_TYPES = List.of("text");

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
            if (form.isLive()) {// if live , only fetch live=true
                predicates.add(cb.equal(root.get("live"), form.isLive()));
            } else {// if dev
                // if devInData=null or devInData=false (default value)
                if (form.getApp() != null && form.getApp().getX() != null && !form.getApp().getX().at("/devInData").asBoolean(false)) {
                    predicates.add(cb.equal(root.get("live"), form.isLive()));
                }
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
//                mapJoinTie
                //if type == 'ALL'
//                predicates.add(cb.equal(mapJoinTier.get("type"),"FIXED"));
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

            if (status != null) {
                // 1. FAST PRE-CHECK: Do we have at least one non-null, non-empty value?
                boolean hasValidData = status.values().stream()
                        .anyMatch(val -> val != null && !val.isEmpty());

                if (hasValidData) {
                    List<Predicate> statusFilterPred = new ArrayList<>();
                    status.keySet().forEach(s -> {
                        if (status.get(s) != null && !status.get(s).isEmpty()) {
                            if ("-1".equals(s)) {
                                // utk drafted,submitted (awal2) currentTierId = null
                                // Problem bila entry dh pernah lalu approval (or submitted using codes, etc. Perlu investigate gk.
                                // For now, remove condition currentTierId=null
                                // Masalah nya akan akan captured juak if ada status dlm tier pake 'submitted'
//                            statusFilterPred.add(cb.and(cb.isNull(root.get("currentTierId")),
//                                    root.get("currentStatus").in((Object[]) status.get(s).split(","))));

                                statusFilterPred.add(cb.and(cb.isNull(root.get("currentTierId")),
                                        root.get("currentStatus").in((Object[]) status.get(s).split(","))));
                            } else {
                                // status=submitted + currentTier = approved
                                statusFilterPred.add(cb.and(cb.equal(root.get("currentTierId"), s),
                                        root.get("currentStatus").in((Object[]) status.get(s).split(","))));
                            }
                        }
                    });
                    predicates.add(cb.or(statusFilterPred.toArray(new Predicate[0])));
                }
            }

            List<Predicate> paramPredicates = new ArrayList<>();

            // Build param predicate
            if (filters != null) {
                if (qBuilder != null && !qBuilder.isEmpty()) {

                    Set<String> _filtersKey = filters.keySet();

                    Predicate pred = qWalker(root, cb, mapJoinPrev, "$and", qBuilder, _filtersKey);
                    if (pred!=null){
                        paramPredicates.add(pred);
                    }
                    // _filterKey ialah semua keyset. Bila qWalker, akan remove key dari keyset
                    // balance key should still be evaluated
                    _filtersKey.forEach(fk->paramPredicates.add(createPredicate(root, cb, mapJoinPrev, fk)));

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
                    if (s.contains("$")) {
                        String[] col = splitted[0].split("\\.", 2); //['$','category.name']
                        String fieldFull = col[1]; // 'category.name';
                        String fieldCode = fieldFull.split("\\.")[0]; // 'category'
                        String dir = "asc";
                        Path pred = root.get("data");
                        Form lForm = form;

                        if ("$prev$".equals(col[0])) {
                            lForm = form.getPrev();
                            pred = mapJoinPrev.get("data");
                        }

                        if (splitted.length == 2) {
                            dir = splitted[1];
                        }

                        Expression<?> jsonValueExpression=null;
                        // process
                        if (lForm.getItems().containsKey(fieldCode)) {
                            String fieldType = lForm.getItems().get(fieldCode).getType();

                            if (DATE_NUMBER_TYPES.contains(fieldType)) {
                                jsonValueExpression = cb.function("JSON_VALUE", Double.class, pred, cb.literal("$." + fieldFull)).as(Double.class);
                            } else {
                                jsonValueExpression = cb.function("JSON_VALUE", String.class, pred, cb.literal("$." + fieldFull));
                            }

                        }else{
                            if(List.of("$id","$counter").contains(fieldCode)){
                                jsonValueExpression = cb.function("JSON_VALUE", Double.class, pred, cb.literal("$." + fieldFull)).as(Double.class);
                            }
                            if(List.of("$code").contains(fieldCode)){
                                jsonValueExpression = cb.function("JSON_VALUE", String.class, pred, cb.literal("$." + fieldFull));
                            }
                        }

                        if (jsonValueExpression!=null){
                            if ("asc".equals(dir)) {
                                orders.add(cb.asc(jsonValueExpression));
                            } else if ("desc".equals(dir)) {
                                orders.add(cb.desc(jsonValueExpression));
                            }
                        }

                    } else {
                        String field = splitted[0];
                        String dir = "asc";
                        if (splitted.length == 2) {
                            dir = splitted[1];
                        }

                        // process
                        if ("asc".equals(dir)) {
                            orders.add(cb.asc(root.get(field)));
                        } else if ("desc".equals(dir)) {
                            orders.add(cb.desc(root.get(field)));
                        }
                    }
                });

                if (orders.size() > 0) {
                    cq.orderBy(orders);
                }
            }

            Predicate params;
            if ("OR".equals(cond)) {
                params = cb.or(paramPredicates.toArray(new Predicate[0]));
            } else {
                params = cb.and(paramPredicates.toArray(new Predicate[0]));
            }

            return cb.and(cb.and(predicates.toArray(new Predicate[0])),
                    params);

        };
    }

    private Predicate createPredicate(Root<Entry> root, CriteriaBuilder cb, Join<Entry, Entry> mapJoinPrev, String f) {
        List<Predicate> paramPredicates = new ArrayList<>();
        Object rawFilterObj = filters.get(f);

        if (rawFilterObj == null) {
            return cb.and(paramPredicates.toArray(new Predicate[0])); // RETURN EARLY
        }

        String filterValue = String.valueOf(rawFilterObj);
        String[] splitted1 = f.split("\\.");
        String rootCol = splitted1[0]; // data or prev

        Path<?> predRoot = null;
        long tierId;
        String fieldFull = "";
        String fieldCode = "";
        Form form = null;
        String logContext = getLogContext(f); // Centralized logging string

        if ("$$".equals(rootCol)) {
            String[] splitted = f.split("\\.", 3);
            tierId = Long.parseLong(splitted[1]);
            fieldFull = splitted[2];
            fieldCode = (fieldFull.split("\\.")[0]).split("~")[0];
            MapJoin<Entry, Long, EntryApproval> mapJoin = root.joinMap("approval", JoinType.LEFT);
            predRoot = mapJoin.get("data");
            paramPredicates.add(cb.equal(mapJoin.key(), tierId));
            form = this.form;
        } else if ("$".equals(rootCol) || "$prev$".equals(rootCol)) {
            String[] splitted = f.split("\\.", 2);
            fieldFull = splitted[1];
            fieldCode = (fieldFull.split("\\.")[0]).split("~")[0];
            predRoot = "$".equals(rootCol) ? root.get("data") : mapJoinPrev.get("data");
            form = "$".equals(rootCol) ? this.form : (this.form != null ? this.form.getPrev() : null);
        }

        if (Arrays.asList("$$", "$", "$prev$").contains(rootCol)) {
            if (form != null && form.getItems() != null && form.getItems().get(fieldCode) != null && !fieldFull.contains("*")) {

                String[] splitField = fieldFull.split("~");
                Expression<String> jsonValueString = cb.function("JSON_VALUE", String.class, predRoot, cb.literal("$." + splitField[0]));
                Expression<Double> jsonValueDouble = cb.function("JSON_VALUE", Double.class, predRoot, cb.literal("$." + splitField[0]));

                if ("~null".equals(filterValue)) {
                    paramPredicates.add(cb.upper(jsonValueString).isNull());
                } else if ("~notnull".equals(filterValue)) {
                    paramPredicates.add(cb.upper(jsonValueString).isNotNull());
                } else {
                    String fieldType = form.getItems().get(fieldCode).getType();

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
                                    paramPredicates.add(cb.equal(cb.function("JSON_VALUE", Double.class, predRoot, cb.literal("$." + fieldFull)), Double.parseDouble(filterValue)));
                                } catch (NumberFormatException nfe) {
                                    logger.error("Error parsing value for " + logContext + ": " + filterValue);
                                } catch (Exception e) {
                                    logger.error("Error processing filter for " + logContext + ": " + e.getMessage());
                                }
                            }
                        }
                    } else if (CHECKBOX_TYPES.contains(fieldType)) {
                        Expression<Boolean> jsonValueBoolean = cb.function("JSON_VALUE", Boolean.class, predRoot, cb.literal("$." + splitField[0]));
                        if (Boolean.parseBoolean(filterValue)) {
                            paramPredicates.add(cb.isTrue(jsonValueBoolean));
                        } else {
                            paramPredicates.add(cb.or(cb.isFalse(jsonValueBoolean), cb.isNull(jsonValueBoolean)));
                        }
                    } else if (TEXT_TYPES.contains(fieldType)) {
                        if (fieldFull.contains("~")) {
                            applyDecoratorPredicate(cb, paramPredicates, splitField[1], filterValue, jsonValueString, null, true, logContext);
                        } else {
                            String searchText = "%" + filterValue + "%";
                            if ("input".equals(form.getItems().get(fieldCode).getSubType())) {
                                searchText = filterValue; // exact match
                            }
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
            } else if (fieldFull.contains("*")) {
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
            } else {
                // Not part of form
                paramPredicates.add(cb.like(cb.upper(cb.function("JSON_VALUE", String.class, predRoot, cb.literal("$." + fieldFull))), filterValue.toUpperCase()));
            }
        } else if ("$$_".equals(rootCol)) {
            String[] splitted = f.split("\\.", 3);
            tierId = Long.parseLong(splitted[1]);
            fieldFull = splitted[2];
            fieldCode = fieldFull.split("\\.")[0];
            MapJoin<Entry, Long, EntryApproval> mapJoin = root.joinMap("approval", JoinType.LEFT);
            predRoot = mapJoin;
            paramPredicates.add(cb.equal(mapJoin.key(), tierId));

            if ("timestamp".equals(fieldCode)) {
                if (!filterValue.isEmpty()) {
                    String[] splitField = fieldFull.split("~");
                    if (fieldFull.contains("~")) {
                        // Delegate to helper. Note: cast predRoot to Double expression
                        applyDecoratorPredicate(cb, paramPredicates, splitField[1], filterValue, null, predRoot.get(fieldCode).as(Double.class), false, logContext);
                    } else {
                        try {
                            paramPredicates.add(cb.equal(predRoot.get(fieldCode), Double.parseDouble(filterValue)));
                        } catch (NumberFormatException nfe) {
                            logger.error("Error parsing value for " + logContext + ": " + filterValue);
                        }
                    }
                }
            } else if ("status".equals(fieldCode)) {
                paramPredicates.add(cb.equal(predRoot.get(fieldFull), filterValue));
            } else if ("remark".equals(fieldCode)) {
                paramPredicates.add(cb.like(cb.upper(predRoot.get(fieldFull).as(String.class)), ("%" + filterValue + "%").toUpperCase()));
            } else if ("email".equals(fieldCode)) {
                paramPredicates.add(cb.like(cb.upper(predRoot.get(fieldFull).as(String.class)), filterValue.toUpperCase()));
            }

        } else if ("$_".equals(rootCol)) {
            fieldCode = f.split("\\.")[1];
            String[] splitField = fieldCode.split("~");

            if ("email".equals(fieldCode)) {
                paramPredicates.add(cb.equal(cb.upper(cb.trim(root.get("email"))), filterValue.trim().toUpperCase()));
            } else if (List.of("id", "currentTier", "currentTierId", "currentEdit").contains(fieldCode)) {
                paramPredicates.add(cb.equal(root.get(fieldCode), rawFilterObj));
            } else if ("currentStatus".equals(fieldCode)) {
                paramPredicates.add(cb.like(cb.upper(root.get("currentStatus")), filterValue.toUpperCase()));
            } else if (List.of("submissionDate", "resubmissionDate", "modifiedDate", "createdDate").contains(splitField[0])) {
                if (!filterValue.isEmpty()) {
                    if (splitField.length > 1) {
                        applyDateDecoratorPredicate(cb, paramPredicates, splitField[1], filterValue, root.get(splitField[0]).as(Date.class), logContext);
                    } else {
                        try {
                            paramPredicates.add(cb.equal(root.get(fieldCode), new Date(Long.parseLong(filterValue))));
                        } catch (Exception e) {
                            logger.error("Error processing date filter for " + logContext + ": " + e.getMessage());
                        }
                    }
                }
            }
        }

        return cb.and(paramPredicates.toArray(new Predicate[0]));
    }

    /**
     * Helper to handle structured query using qWalker.
     */
    private Predicate qWalker(Root<Entry> root, CriteriaBuilder cb, Join<Entry, Entry> mapJoinPrev, String cond, JsonNode qList, Set<String> keySet) {
        // Pre-size to avoid resizing penalty
        int estimatedSize = qList.size() > 0 ? qList.size() * 2 : 4;
        List<Predicate> predicateList = new ArrayList<>(estimatedSize);

        for (JsonNode jsonNode : qList) {
            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                JsonNode value = entry.getValue();

                switch (key) {
                    case "$and", "$or" ->
                        // recursive combining
                            predicateList.add(qWalker(root, cb, mapJoinPrev, key, value, keySet));
                    default -> {
                        // Only compute template once, skip nulls
                        if (!value.isNull()) {
                            filters.computeIfAbsent(key, k ->
                                    Helper.compileTpl(value.asText(""), dataMap)
                            );
                        }
                        predicateList.add(createPredicate(root, cb, mapJoinPrev, key));
                        keySet.remove(key);
                    }
                }
            }
        }

        if (predicateList.isEmpty()) {
            return null;
        }

        Predicate[] arr = predicateList.toArray(new Predicate[predicateList.size()]);

        return "$or".equals(cond) ? cb.or(arr) : cb.and(arr);
    }

    /**
     * Helper to generate standardized log context strings.
     */
    private String getLogContext(String f) {
        if (this.form != null && this.form.getApp() != null) {
            return "[App:" + this.form.getApp().getAppPath() + "]->[Form:" + this.form.getId() + "]->[" + f + "]";
        }
        return "[" + f + "]";
    }

    /**
     * Applies standard operators (~in, ~from, ~contain, etc.) for Strings and Numbers.
     */
    private void applyDecoratorPredicate(CriteriaBuilder cb, List<Predicate> paramPredicates,
                                         String operator, String filterValue,
                                         Expression<String> stringExpr, Expression<Double> doubleExpr,
                                         boolean isTextType, String logContext) {
        try {
            switch (operator) {
                case "in":
                    if (isTextType && stringExpr != null) {
                        // IN operator for text is replaced with multiple LIKE operations to support wildcard
                        String[] patterns = Arrays.stream(filterValue.toUpperCase().split(","))
                                .map(String::trim)
                                .toArray(String[]::new);
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
                    if (doubleExpr != null) {
                        paramPredicates.add(cb.greaterThanOrEqualTo(doubleExpr, Double.parseDouble(filterValue)));
                    }
                    break;
                case "to":
                    if (doubleExpr != null) {
                        paramPredicates.add(cb.lessThanOrEqualTo(doubleExpr, Double.parseDouble(filterValue)));
                    }
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

    /**
     * Applies standard operators (~from, ~to, ~between) specifically for $_.submissionDate/resubmissionDate/modifiedDate/createdDate"
     */
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
