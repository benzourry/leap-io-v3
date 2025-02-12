package com.benzourry.leap.filter;//package com.benzourry.leap.filter;
//
//import com.benzourry.leap.model.Entry;
//import com.benzourry.leap.model.EntryApproval;
//import com.benzourry.leap.model.Form;
//import com.benzourry.leap.model.Tier;
//import com.benzourry.leap.utility.OptionalBooleanBuilder;
//import com.fasterxml.jackson.databind.JsonNode;
//import lombok.Builder;
//import org.springframework.data.jpa.domain.Specification;
//import org.springframework.util.CollectionUtils;
//
//import jakarta.persistence.criteria.*;
//
//import java.util.*;
//
//@Builder
//public class EntryFilterOld {
//
//
//    Long formId;
//    String searchText;
//    String email;
//    String approver;
//    String admin;
//    Date submissionDateFrom;
//    Date submissionDateTo;
//
//    List<String> sort;
//
//    Map<String, Object> filters;
//    Map<String, String> status;
//    Form form;
//    boolean action;
//    List<Long> ids;
//    JsonNode qBuilder;
//    String cond = "AND";
////    String[][] root = new String[][]{{"$", "data"}, {"$prev$", "prev"}, {"$$", "approval", "$$_", "approval"}};
//
//
//    public Specification<Entry> filter() {
//
//        return (root, cq, cb) -> {
//
//            Join<Entry, Entry> mapJoinPrev = root.join("prevEntry", JoinType.LEFT);
//
//            List<Predicate> predicates = new OptionalBooleanBuilder(cb)
//                    .notNullAnd(searchText, cb.or(
//                            cb.isNotNull(cb.function("JSON_SEARCH", String.class,
//                                    //                          new HibernateInlineExpression(cb, "JSON_UNQUOTE(data)"),
//                                    cb.function("JSON_UNQUOTE", String.class, root.get("data")),
//                                    cb.literal("all"),
//                                    cb.literal("%" + searchText + "%"))),
//                            cb.isNotNull(cb.function("JSON_SEARCH", String.class,
//                                    cb.function("JSON_UNQUOTE", String.class, mapJoinPrev.get("data")),
//                                    cb.literal("all"),
//                                    cb.literal("%" + searchText + "%")))
//                    ))
//                    .notNullAnd(submissionDateFrom, cb.greaterThan(root.get("submissionDate"), submissionDateFrom))
//                    .notNullAnd(submissionDateTo, cb.lessThan(root.get("submissionDate"), submissionDateTo))
//                    .notNullAnd(email, cb.like(cb.concat(",", cb.concat(
//                            cb.function("REPLACE", String.class, root.get("email"), cb.literal(" "), cb.literal(""))
//                            , ",")), "%," + email + ",%"))
//                    .notNullAnd(admin, cb.like(cb.concat(",", cb.concat(
//                            cb.function("REPLACE", String.class, root.get("email"), cb.literal(" "), cb.literal(""))
//                            , ",")), "%," + admin + ",%"))
//                    .notNullAnd(formId, cb.equal(root.get("form").get("id"), formId))
//                    .build();
//
//            if (ids != null && !CollectionUtils.isEmpty(ids)) {
//                predicates.add(root.get("id").in(ids));
//            }
//            /* If type action, join dgn tier, approver then polah condition entry.tierId.approver = approver*/
//            if (action) {
//                Join<Entry, Form> mapJoinForm = root.join("form");
//                Join<Form, Tier> mapJoinTier = mapJoinForm.join("tiers", JoinType.LEFT);
//                MapJoin<Entry, Long, String> mapJoinApprover = root.joinMap("approver", JoinType.LEFT);
////                mapJoinTie
//                //if type == 'ALL'
////                predicates.add(cb.equal(mapJoinTier.get("type"),"FIXED"));
//                predicates.add(cb.equal(root.get("currentTier"), mapJoinTier.get("sortOrder")));
//                predicates.add(
//                        cb.or(cb.equal(mapJoinTier.get("type"), "ALL"),
//                                cb.and(mapJoinTier.get("type").in(Arrays.asList("FIXED", "DYNAMIC", "ASSIGN", "GROUP")),
//                                        cb.equal(mapJoinApprover.key(), mapJoinTier.get("id")),
//                                        cb.like(cb.concat(",", cb.concat(
//                                                cb.function("REGEXP_REPLACE", String.class, mapJoinApprover, cb.literal("[\r\n ]"), cb.literal(""))
//                                                , ",")), "%," + approver + ",%")
//                                )
//                        )
//                );
//            }
//
//            if (status != null) {
//                List<Predicate> statusFilterPred = new ArrayList<>();
//                status.keySet().forEach(s -> {
//                    if (status.get(s) != null && !status.get(s).isEmpty()) {
////                    System.out.println(s);
//                        if ("-1".equals(s)) {
//                            // utk drafted,submitted (awal2) currentTierId = null
//                            statusFilterPred.add(cb.and(cb.isNull(root.get("currentTierId")),
//                                    root.get("currentStatus").in(status.get(s).split(","))));
//                        } else {
//                            // status=submitted + currentTier = approved
//                            statusFilterPred.add(cb.and(cb.equal(root.get("currentTierId"), s),
//                                    root.get("currentStatus").in(status.get(s).split(","))));
//                        }
//                    }
//
//                });
//
//                predicates.add(cb.or(statusFilterPred.toArray(new Predicate[statusFilterPred.size()])));
//
//            }
//
//            List<Predicate> paramPredicates = new ArrayList<>();
//
///**
//            List<Predicate> paramPredicatesAnd = new ArrayList<>();
//            List<Predicate> paramPredicatesOr = new ArrayList<>();
//
//             {
//                 $and:{
//                     $or:{
//                         "$.start~between":"111124232,76786786",
//                         "$.end~between":"111124232,76786786"
//                     },
//                     "$.venue":"BMU"
//                 }
//             }
//
//
//            Map<String, List<Predicate>> predMap = new HashMap<>();
//
//            qBuilder.at("/$and").fields().forEachRemaining(entry->{
//                List<Predicate> p = new ArrayList<>();
//                 if ("$or".equals(entry.getKey())){
//                     entry.getValue().fields().forEachRemaining(eo->{
//                         List<Predicate> o = new ArrayList<>();
//                         predMap.put(eo.getKey(), o);
//                     });
//                 }else if ("$and".equals(entry.getKey())) {
//                     entry.getValue().fields().forEachRemaining(ea->{
//                         List<Predicate> a = new ArrayList<>();
//                         predMap.put(ea.getKey(), a);
//                     });
//                 }else{
//                    predMap.put(entry.getKey(), p);
//                 }
//            });
//
//            //dalam filter dibah
//             predMap.get(f).add()
//             // time mok get all predicate
//
//             qBuilder.at("/$and").fields().forEachRemaining(entry->{
//                 List<Predicate> p = new ArrayList<>();
//                 if ("$or".equals(entry.getKey())){
//                         predMap.put(eo.getKey(), o);
//                 }else if ("$and".equals(entry.getKey())) {
//                     entry.getValue().fields().forEachRemaining(ea->{
//                         List<Predicate> a = new ArrayList<>();
//                         predMap.put(ea.getKey(), a);
//                     });
//                 }else{
//                    predMap.put(entry.getKey(), p);
//                 }
//             });
//
//             */
//
//            if (filters != null) {
//                filters.keySet().forEach(f -> {
//                    if (filters.get(f) != null) {
//
//                        String[] splitted1 = f.split("\\.");
//
//                        String rootCol = splitted1[0]; // data or prev
//
////                        String realRoot = rootCol.replace("$$", "approval")
////                                .replace("$prev$", "prev")
////                                .replace("$", "data");
//
//
//                        // $ = data, $prev$ = prev, $$ = approval
//                        // $$.484.college
//
//                        Path predRoot = null;
//
//                        Long tierId;
//                        String fieldFull = ""; //$$.123.lookup.code -> lookup.code
//                        String fieldCode = ""; //$$.123.lookup.code -> lookup
//                        // what if list section?
//                        // $.address*.country.code
//                        // $.address*.date~from
//                        Form form = null;
//                        if ("$$".equals(rootCol)) {
//                            String[] splitted = f.split("\\.", 3);
//                            tierId = Long.parseLong(splitted[1]);
//                            fieldFull = splitted[2];
//                            fieldCode = (fieldFull.split("\\.")[0]).split("~")[0];
//                            MapJoin<Entry, Long, EntryApproval> mapJoin = root.joinMap("approval", JoinType.LEFT);
//                            predRoot = "$$".equals(rootCol) ? mapJoin.get("data") : mapJoin;
//                            paramPredicates.add(cb.equal(mapJoin.key(), tierId));
//                            form = this.form;
//                        } else if ("$".equals(rootCol) || "$prev$".equals(rootCol)) {
//                            String[] splitted = f.split("\\.", 2);
//                            fieldFull = splitted[1];
//                            fieldCode = (fieldFull.split("\\.")[0]).split("~")[0];
//                            predRoot = "$".equals(rootCol) ? root.get("data") : mapJoinPrev.get("data"); // new HibernateInlineExpression(cb, realRoot);
//                            form = "$".equals(rootCol) ? this.form : this.form != null ? this.form.getPrev() : null;
//                        }
//
//                        if (Arrays.asList("$$", "$", "$prev$").contains(rootCol)) {
//                            if (form != null && form.getItems() != null && form.getItems().get(fieldCode) != null && !fieldFull.contains("*")) {
//
//                                if ("~null".equals(filters.get(f) + "")) {
//                                    paramPredicates.add(
//                                            cb.upper(cb.function("JSON_VALUE", String.class, predRoot, cb.literal("$." + fieldFull))).isNull()
//                                    );
//                                } else if ("~notnull".equals(filters.get(f) + "")) {
//                                    paramPredicates.add(
//                                            cb.upper(cb.function("JSON_VALUE", String.class, predRoot, cb.literal("$." + fieldFull))).isNotNull()
//                                    );
//                                } else {
//
//                                    if (Arrays.asList("select", "radio").contains(form.getItems().get(fieldCode).getType())) {
//                                        paramPredicates.add(cb.upper(cb.function("JSON_VALUE", String.class,
//                                                predRoot,
//                                                cb.literal("$." + fieldFull))).in(Arrays.asList((filters.get(f) + "").toUpperCase().split(",")))
//                                        );
//                                    } else if (Arrays.asList("date", "number", "scale", "scaleTo10", "scaleTo5").contains(form.getItems().get(fieldCode).getType())) {
//                                        // if $$.484.college~from
//                                        if (!filters.get(f).toString().isEmpty()) {
//                                            if (fieldFull.contains("~")) {
//                                                String[] splitField = fieldFull.split("~");
//                                                if ("from".equals(splitField[1])) {
//                                                    paramPredicates.add(cb.greaterThanOrEqualTo(cb.function("JSON_VALUE", Double.class,
//                                                            predRoot,
//                                                            cb.literal("$." + splitField[0])),
//                                                            Double.parseDouble(filters.get(f) + ""))
//                                                    );
//
//                                                }
//                                                if ("to".equals(splitField[1])) {
//                                                    paramPredicates.add(cb.lessThanOrEqualTo(cb.function("JSON_VALUE", Double.class,
//                                                            predRoot,
//                                                            cb.literal("$." + splitField[0])),
//                                                            Double.parseDouble(filters.get(f) + "")
//                                                    ));
//                                                }
//                                                if ("between".equals(splitField[1])) {
//                                                    String[] time = (filters.get(f) + "").split(",");
//                                                    paramPredicates.add(cb.between(cb.function("JSON_VALUE", Double.class,
//                                                            predRoot,
//                                                            cb.literal("$." + splitField[0])),
//                                                            Double.parseDouble(time[0]),
//                                                            Double.parseDouble(time[1])
//                                                    ));
//                                                }
//                                            } else {
//                                                paramPredicates.add(cb.equal(cb.function("JSON_VALUE", Double.class,
//                                                        predRoot,
//                                                        cb.literal("$." + fieldFull)),
//                                                        Double.parseDouble(filters.get(f) + "")
//                                                ));
//                                            }
//                                        }
//
//                                    } else if (List.of("checkbox").contains(form.getItems().get(fieldCode).getType())) {
//                                        Expression<Boolean> pre = cb.function("JSON_VALUE", Boolean.class,
//                                                predRoot,
//                                                cb.literal("$." + fieldFull));
//                                        if (Boolean.valueOf(filters.get(f) + "")) {
//                                            paramPredicates.add(cb.isTrue(pre));
//                                        } else {
//                                            paramPredicates.add(cb.or(cb.isFalse(pre), cb.isNull(pre)));
//                                        }
//                                    } else if (List.of("text").contains(form.getItems().get(fieldCode).getType())) {
//                                        String searchText = "%" + filters.get(f) + "%";
//                                        // if short text, dont add wildcard to the condition. Problem with email, example ymyati@unimas vs yati@unimas
//                                        if ("input".equals(form.getItems().get(fieldCode).getSubType())) {
//                                            searchText = filters.get(f) + "";
//                                        }
//                                        paramPredicates.add(
//                                                cb.like(cb.upper(cb.function("JSON_VALUE", String.class,
//                                                        predRoot,
//                                                        cb.literal("$." + fieldFull))),
//                                                        searchText.toUpperCase()
//                                                )
//                                        );
//                                    } else {
//                                        paramPredicates.add(
//                                                cb.like(cb.upper(cb.function("JSON_VALUE", String.class,
//                                                        predRoot,
//                                                        cb.literal("$." + fieldFull))),
//                                                        (filters.get(f) + "").toUpperCase())
//                                        );
//                                    }
//                                }
//                            } else if (fieldFull.contains("*")) {
//                                // after checkboxOption so checkboxOPtion can be executed first, then, check for section
//                                // ideally check if fieldcode contain * or not, if not: the above, else: here
//
//                                // CHECK EITHER CHECKBOXOPTION OR SECTION
//                                // THEN GET FIELD_CODE, ETC
//
//                                String fieldTranslated = fieldFull.replace("*", "[*]");
//
//                                Expression<String> a = cb.function("JSON_SEARCH", String.class,
//                                        cb.lower(predRoot.as(String.class)),
//                                        cb.literal("one"),
//                                        cb.literal(("%" + filters.get(f) + "%").toLowerCase()),
//                                        cb.nullLiteral(String.class),
//                                        cb.literal("$." + fieldTranslated)
//                                );
//
//                                paramPredicates.add(cb.isNotNull(a));
//
//                            } else {
//                                /// IF NOT a part of form
//                                paramPredicates.add(
//                                        cb.like(cb.upper(cb.function("JSON_VALUE", String.class,
//                                                predRoot,
//                                                cb.literal("$." + fieldFull))),
//                                                (filters.get(f) + "").toUpperCase()
//                                        )
//                                );
//                            }
//                        } else if ("$$_".equals(rootCol)) {
//                            String[] splitted = f.split("\\.", 3);
//                            tierId = Long.parseLong(splitted[1]);
//                            fieldFull = splitted[2];
//                            fieldCode = fieldFull.split("\\.")[0];
//                            MapJoin<Entry, Long, EntryApproval> mapJoin = root.joinMap("approval", JoinType.LEFT);
//                            predRoot = mapJoin;
//                            paramPredicates.add(cb.equal(mapJoin.key(), tierId));
//
//                            if ("timestamp".equals(fieldCode)) {
//                                if (!filters.get(f).toString().isEmpty()) {
//                                    if (fieldFull.contains("~")) {
//                                        String[] splitField = fieldFull.split("~");
//                                        if ("from".equals(splitField[1])) {
//                                            paramPredicates.add(cb.greaterThanOrEqualTo(predRoot.get(fieldCode),
//                                                    Double.parseDouble(filters.get(f) + "")
//                                            ));
//
//                                        }
//                                        if ("to".equals(splitField[1])) {
//                                            paramPredicates.add(cb.lessThanOrEqualTo(predRoot.get(fieldCode),
//                                                    Double.parseDouble(filters.get(f) + "")
//                                            ));
//                                        }
//                                    } else {
//                                        paramPredicates.add(cb.equal(predRoot.get(fieldCode),
//                                                Double.parseDouble(filters.get(f) + "")
//                                        ));
//
//                                    }
//                                }
//                            } else if ("status".equals(fieldCode)) {
//                                paramPredicates.add(
//                                        cb.equal(predRoot.get(fieldFull), (filters.get(f) + ""))
//                                );
//                            } else if ("remark".equals(fieldCode)) {
//                                paramPredicates.add(
//                                        cb.like(cb.upper(predRoot.get(fieldFull)), ("%" + filters.get(f) + "%").toUpperCase())
//                                );
//                            } else if ("email".equals(fieldCode)) {
//                                paramPredicates.add(
//                                        cb.like(cb.upper(predRoot.get(fieldFull)), (filters.get(f) + "").toUpperCase())
//                                );
//                            }
//                        } else if ("$_".equals(rootCol)) {
//                            fieldCode = f.split("\\.")[1];
//                            if ("email".equals(fieldCode)) {
//                                paramPredicates.add(cb.equal(cb.upper(cb.trim(root.get("email"))), (filters.get(f) + "").trim().toUpperCase()));
//                            } else if ("currentTier".equals(fieldCode)) {
//                                paramPredicates.add(cb.equal(root.get("currentTier"), filters.get(f)));
//                            } else if ("currentStatus".equals(fieldCode)) {
//                                paramPredicates.add(cb.like(cb.upper(root.get("currentStatus")), (filters.get(f) + "").toUpperCase()));
//                            }
//                        }
//                    }
//                });
//            }
//
//            cq.distinct(true);
//
//            List<Order> orders = new ArrayList<>();
//            if (sort != null) {
//                sort.forEach(s -> {
//                    if (s.contains("$")) {
//                        String[] splitted = s.split("~"); // ['$.category.name','asc']
//                        String[] col = splitted[0].split("\\.", 2); //['$','category.name']
//                        String fieldFull = col[1]; // 'category.name';
//                        String fieldCode = fieldFull.split("\\.")[0]; // 'category'
//                        String dir = "asc";
//                        Path pred = root.get("data");
//                        if ("$".equals(col[0])) {
//                        } else if ("$prev$".equals(col[0])) {
//                            pred = mapJoinPrev.get("data");
//                        }
//
//                        if (splitted.length == 2) {
//                            dir = splitted[1];
//                        }
//
//                        // process
//                        if (form.getItems().get(fieldCode) != null) {
//                            if ("asc".equals(dir)) {
//                                if (Arrays.asList("date", "number", "scale", "scaleTo10", "scaleTo5").contains(form.getItems().get(fieldCode).getType())) {
//                                    orders.add(cb.asc(cb.function("JSON_VALUE", Double.class,
//                                            pred,
//                                            cb.literal("$." + fieldFull)).as(Double.class)));
//                                } else {
//                                    orders.add(cb.asc(cb.function("JSON_VALUE", String.class,
//                                            pred,
//                                            cb.literal("$." + fieldFull))));
//                                }
//                            } else if ("desc".equals(dir)) {
//
//                                if (Arrays.asList("date", "number", "scale", "scaleTo10", "scaleTo5").contains(form.getItems().get(fieldCode).getType())) {
//                                    orders.add(cb.desc(cb.function("JSON_VALUE", Double.class,
//                                            pred,
//                                            cb.literal("$." + fieldFull)).as(Double.class)));
//
//                                } else {
//                                    orders.add(cb.desc(cb.function("JSON_VALUE", String.class,
//                                            pred,
//                                            cb.literal("$." + fieldFull))));
//                                }
//                            }
//                        }
//
//                    } else {
//                        String[] splitted = s.split("~");
//                        String field = splitted[0];
//                        String dir = "asc";
//                        if (splitted.length == 2) {
//                            dir = splitted[1];
//                        }
//
//                        // process
//                        if ("asc".equals(dir)) {
//                            orders.add(cb.asc(root.get(field)));
//                        } else if ("desc".equals(dir)) {
//                            orders.add(cb.desc(root.get(field)));
//                        }
//                    }
//                });
//
//                if (orders.size() > 0) {
//                    cq.orderBy(orders);
//                }
//            }
//
//            Predicate params = null;
//            if ("OR".equals(cond)) {
//                params = cb.or(paramPredicates.toArray(new Predicate[paramPredicates.size()]));
//            } else {
//                params = cb.and(paramPredicates.toArray(new Predicate[paramPredicates.size()]));
//            }
//
//            return cb.and(cb.and(predicates.toArray(new Predicate[predicates.size()])),
//                    params);
//
//        };
//    }
//
//}
