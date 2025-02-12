package com.benzourry.leap.filter;//package com.benzourry.leap.filter;
//
//import com.benzourry.leap.model.Entry;
//import com.benzourry.leap.model.EntryApproval;
//import com.benzourry.leap.model.Form;
//import com.benzourry.leap.model.Tier;
//import com.benzourry.leap.utility.OptionalBooleanBuilder;
//import lombok.Builder;
//import org.springframework.data.jpa.domain.Specification;
//
//import jakarta.persistence.criteria.*;
//import java.util.*;
//
//@Builder
//public class EntryFilterChart {
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
////                    .notNullAnd(searchText,cb.like(root.get("prev"), "%"))
//                    .notNullAnd(searchText, cb.or(
//                            cb.isNotNull(cb.function("JSON_SEARCH", String.class,
//    //                          new HibernateInlineExpression(cb, "JSON_UNQUOTE(data)"),
//                                cb.function("JSON_UNQUOTE", String.class,root.get("data")),
//                                cb.literal("all"),
//                                cb.literal("%" + searchText + "%"))),
//                            cb.isNotNull(cb.function("JSON_SEARCH", String.class,
//                                cb.function("JSON_UNQUOTE", String.class,mapJoinPrev.get("data")),
////                              new HibernateInlineExpression(cb, "JSON_UNQUOTE(prev)"),
//                                cb.literal("all"),
//                                cb.literal("%" + searchText + "%")))
//                        ))
//                    .notNullAnd(submissionDateFrom, cb.greaterThan(root.get("submissionDate"), submissionDateFrom))
//                    .notNullAnd(submissionDateTo, cb.lessThan(root.get("submissionDate"), submissionDateTo))
//                    .notNullAnd(email, cb.like(cb.concat(",", cb.concat(
//                            cb.function("REPLACE", String.class, root.get("email"), cb.literal(" "), cb.literal(""))
//                            , ",")), "%," + email + ",%"))
//                    .notNullAnd(admin, cb.like(cb.concat(",", cb.concat(
//                            cb.function("REPLACE", String.class, root.get("email"), cb.literal(" "), cb.literal(""))
//                            , ",")), "%," + admin + ",%"))
//                    .notNullAnd(formId, cb.equal(root.get("form"), formId))
//                    .build();
//
//            /* If type action, join dgn tier, approver the polah condition entry.tierId.approver = approver*/
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
//                            predicates.add(cb.equal(mapJoin.key(), tierId));
//                            form = this.form;
//                        } else if ("$".equals(rootCol) || "$prev$".equals(rootCol)) {
//                            String[] splitted = f.split("\\.", 2);
//                            fieldFull = splitted[1];
//                            fieldCode = (fieldFull.split("\\.")[0]).split("~")[0];
//                            predRoot = "$".equals(rootCol) ? root.get("data"): mapJoinPrev.get("data"); // new HibernateInlineExpression(cb, realRoot);
//                            form = "$".equals(rootCol) ? this.form : this.form != null ? this.form.getPrev() : null;
//                        }
////                        System.out.println("formId:"+form.getId());
//
//                        if (Arrays.asList("$$", "$", "$prev$").contains(rootCol)) {
////                            System.out.println("fieldcode:" + fieldCode +", form:"+form);
//                            if (form != null && form.getItems() != null && form.getItems().get(fieldCode) != null && !fieldFull.contains("*")) {
//                                if (Arrays.asList("select", "radio").contains(form.getItems().get(fieldCode).getType())) {
//                                    predicates.add(cb.like(cb.upper(cb.function("JSON_VALUE", String.class,
//                                                    predRoot,
//                                                    cb.literal("$." + fieldFull))),
//                                            (filters.get(f) + "").toUpperCase()
////                                            new LiteralExpression<>((CriteriaBuilderImpl) cb, String.class, (filters.get(f) + "").toUpperCase())
//                                    ));
//
//                                } else if (Arrays.asList("date", "number", "scale", "scaleTo10", "scaleTo5").contains(form.getItems().get(fieldCode).getType())) {
//                                    // if $$.484.college~from
//                                    if (filters.get(f)!=null && !filters.get(f).toString().isEmpty()) {
//                                        if (fieldFull.contains("~")) {
//                                            String[] splitField = fieldFull.split("~");
//                                            if ("from".equals(splitField[1])) {
//                                                    predicates.add(cb.greaterThanOrEqualTo(cb.function("JSON_VALUE", Double.class,
//                                                            predRoot,
//                                                            cb.literal("$." + splitField[0])),
//                                                            Double.parseDouble(filters.get(f) + ""))
////                                                            new LiteralExpression<>((CriteriaBuilderImpl) cb, Double.class, Double.parseDouble(filters.get(f) + "")))
//                                                );
//
//                                            }
//                                            if ("to".equals(splitField[1])) {
//                                                    predicates.add(cb.lessThanOrEqualTo(cb.function("JSON_VALUE", Double.class,
//                                                            predRoot,
//                                                            cb.literal("$." + splitField[0])),
//                                                            Double.parseDouble(filters.get(f) + "")
////                                                            new LiteralExpression<>((CriteriaBuilderImpl) cb, Double.class, Double.parseDouble(filters.get(f) + "")))
//                                                ));
//                                            }
//                                        } else {
//                                                predicates.add(cb.equal(cb.function("JSON_VALUE", Double.class,
//                                                        predRoot,
//                                                        cb.literal("$." + fieldFull)),
//                                                        Double.parseDouble(filters.get(f) + "")
////                                                        new LiteralExpression<>((CriteriaBuilderImpl) cb, Double.class, Double.parseDouble(filters.get(f) + ""))
//                                                ));
//                                        }
//                                    }
//
//                                } else if (List.of("checkbox").contains(form.getItems().get(fieldCode).getType())) {
//                                    Expression<Boolean> pre = cb.function("JSON_VALUE", Boolean.class,
//                                            predRoot,
//                                            cb.literal( "$." + fieldFull));
//                                    if (Boolean.valueOf(filters.get(f) + "")) {
//                                        predicates.add(cb.isTrue(pre));
//                                    } else {
//                                        predicates.add(cb.or(cb.isFalse(pre), cb.isNull(pre)));
//                                    }
//                                } else if (List.of("text").contains(form.getItems().get(fieldCode).getType())) {
//                                    String searchText = "%" + filters.get(f) + "%";
//                                    // if short text, dont add wildcard to the condition. Problem with email, example ymyati@unimas vs yati@unimas
//                                    if ("input".equals(form.getItems().get(fieldCode).getSubType())){
//                                        searchText = filters.get(f)+"";
//                                    }
//                                    predicates.add(
//                                            cb.like(cb.upper(cb.function("JSON_VALUE", String.class,
//                                                            predRoot,
//                                                    cb.literal("$." + fieldFull))),
//                                                    searchText.toUpperCase()
////                                                    new LiteralExpression<>((CriteriaBuilderImpl) cb, String.class, searchText.toUpperCase())
//                                            )
//                                    );
//                                } else {
//                                    predicates.add(
//                                            cb.like(cb.upper(cb.function("JSON_VALUE", String.class,
//                                                            predRoot,
//                                                            cb.literal("$." + fieldFull))),
//                                                            (filters.get(f) + "").toUpperCase())
////                                                    new LiteralExpression<>((CriteriaBuilderImpl) cb, String.class, (filters.get(f) + "").toUpperCase()))
//                                    );
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
//                                    Expression<String> a = cb.function("JSON_SEARCH", String.class,
//                                            cb.lower(predRoot),
//                                            cb.literal("one"),
//                                            cb.literal(("%" + filters.get(f) + "%").toLowerCase()),
////                                            new LiteralExpression<>((CriteriaBuilderImpl) cb, String.class, ("%" + filters.get(f) + "%").toLowerCase()),
//                                            cb.nullLiteral(String.class),
//                                            cb.literal("$." + fieldTranslated)
//                            );
//
//                                    predicates.add(cb.isNotNull(a));
//
//                            } else {
//                                /// IF NOT a part of form
//                                predicates.add(
//                                        cb.like(cb.upper(cb.function("JSON_VALUE", String.class,
//                                                        predRoot,
//                                                        cb.literal("$." + fieldFull))),
//                                                (filters.get(f) + "").toUpperCase()
////                                                new LiteralExpression<>((CriteriaBuilderImpl) cb, String.class, (filters.get(f) + "").toUpperCase())
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
//                            predicates.add(cb.equal(mapJoin.key(), tierId));
//
//                            if ("timestamp".equals(fieldCode)) {
//                                if (filters.get(f)!=null && !filters.get(f).toString().isEmpty()) {
//                                    if (fieldFull.contains("~")) {
//                                        String[] splitField = fieldFull.split("~");
//                                        if ("from".equals(splitField[1])) {
//                                            predicates.add(cb.greaterThanOrEqualTo(predRoot.get(fieldCode),
//                                                    Double.parseDouble(filters.get(f) + "")
////                                                    new LiteralExpression<>((CriteriaBuilderImpl) cb, Double.class, Double.parseDouble(filters.get(f) + ""))
//                                            ));
//
//                                        }
//                                        if ("to".equals(splitField[1])) {
//                                            predicates.add(cb.lessThanOrEqualTo(predRoot.get(fieldCode),
//                                                    Double.parseDouble(filters.get(f) + "")
////                                                    new LiteralExpression<>((CriteriaBuilderImpl) cb, Double.class, Double.parseDouble(filters.get(f) + ""))
//                                            ));
//                                        }
//                                    } else {
//                                        predicates.add(cb.equal(predRoot.get(fieldCode),
//                                                Double.parseDouble(filters.get(f) + "")
////                                                new LiteralExpression<>((CriteriaBuilderImpl) cb, Double.class, Double.parseDouble(filters.get(f) + ""))
//                                        ));
//
//                                    }
//                                }
//                            } else if ("status".equals(fieldCode)) {
//                                predicates.add(
//                                        cb.equal(predRoot.get(fieldFull), (filters.get(f) + ""))
//                                );
//                            } else if ("remark".equals(fieldCode)) {
//                                predicates.add(
//                                        cb.like(cb.upper(predRoot.get(fieldFull)), ("%" + filters.get(f) + "%").toUpperCase())
//                                );
//                            }
//                        } else if ("$_".equals(rootCol)) {
//                            fieldCode = f.split("\\.")[1];
////                            System.out.println("&&&&&&&&&&&&&&&&&&:"+filters.get(f)+","+fieldCode);
//                            if ("email".equals(fieldCode)) {
//                                predicates.add(cb.equal(cb.upper(cb.trim(root.get("email"))), (filters.get(f) + "").trim().toUpperCase()));
//                            } else if ("currentTier".equals(fieldCode)) {
//                                predicates.add(cb.equal(root.get("currentTier"), filters.get(f)));
//                            } else if ("currentStatus".equals(fieldCode)) {
//                                predicates.add(cb.like(cb.upper(root.get("currentStatus")), (filters.get(f) + "").toUpperCase()));
//                            }
////                            else{
////                                predicates.add(cb.like(cb.upper(root.get(fieldCode)), (filters.get(f)+"").toUpperCase()));
////                            }
//
//
//                        }
//                    }
//                });
//            }
//
//            cq.distinct(true);
//
//            List<Order> orders = new ArrayList<>();
//            if (sort!=null) {
//                sort.forEach(s -> {
//                   // System.out.println("s:"+s);
//                    if (s.contains("$")) {
//                        String[] splitted = s.split("~"); // ['$.category.name','asc']
//                        String[] col = splitted[0].split("\\.",2); //['$','category.name']
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
////                        Expression path = cb.function("JSON_VALUE", String.class,
////                                pred,
////                                new HibernateInlineExpression(cb, "'$." + col[1] + "'"));
////
////                        if (Arrays.asList("date", "number", "scaleTo10", "scaleTo5").contains(form.getItems().get(col[1]).getType())) {
////                            path = cb.function("JSON_VALUE", Double.class,
////                                    pred,
////                                    new HibernateInlineExpression(cb, "'$." + col[1] + "'")).as(Double.class);
////                        }
//
//                        // process
//                    //    System.out.println(fieldCode);
//                        if (form.getItems().get(fieldCode)!=null) {
//                            if ("asc".equals(dir)) {
////                            orders.add(cb.asc(path));
//                                if (Arrays.asList("date", "number", "scale", "scaleTo10", "scaleTo5").contains(form.getItems().get(fieldCode).getType())) {
//                                //    System.out.println("is number #####");
//                                    orders.add(cb.asc(cb.function("JSON_VALUE", Double.class,
//                                            pred,
//                                            cb.literal("$." + fieldFull)).as(Double.class)));
//                                } else {
//                                //    System.out.println("is not number #####:" + fieldCode);
//                                    orders.add(cb.asc(cb.function("JSON_VALUE", String.class,
//                                            pred,
//                                            cb.literal("$." + fieldFull))));
//                                }
//
//                            } else if ("desc".equals(dir)) {
//
////                            orders.add(cb.desc(path));
//
//                                if (Arrays.asList("date", "number", "scale", "scaleTo10", "scaleTo5").contains(form.getItems().get(fieldCode).getType())) {
//                              //      System.out.println("is number #####");
//                                    orders.add(cb.desc(cb.function("JSON_VALUE", Double.class,
//                                            pred,
//                                            cb.literal("$." + fieldFull)).as(Double.class)));
//
//                                } else {
//                                   // System.out.println("is not number #####:" + fieldCode);
//                                    orders.add(cb.desc(cb.function("JSON_VALUE", String.class,
//                                            pred,
//                                            cb.literal("$." + fieldFull))));
//                                }
//
//                            }
//                        }
//
//                    } else {
//                        String[] splitted = s.split("~");
//                       // System.out.println("field:"+splitted[0]);
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
//
//
//            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
//        };
//    }
//
//
////    static class HibernateInlineExpression extends LiteralExpression<String> {
////
////        public HibernateInlineExpression(CriteriaBuilder criteriaBuilder, String literal) {
////            super((CriteriaBuilderImpl) criteriaBuilder, literal);
////        }
////
////        @Override
////        public String render(RenderingContext renderingContext) {
////            return getLiteral();
////        }
////    }
//}
