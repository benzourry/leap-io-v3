package com.benzourry.leap.filter;

import com.benzourry.leap.model.*;
import com.benzourry.leap.service.MailService;
import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.OptionalBooleanBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.criteria.*;
import lombok.Builder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Builder
public class EntryFilter {


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
//                    .notNullAnd(form.isLive(), cb.equal(root.get("live"), form.isLive()))
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

                    System.out.println("Extra keyset::::"+ _filtersKey);
                    _filtersKey.forEach(fk->paramPredicates.add(createPredicate(root, cb, mapJoinPrev, fk)));

                } else {

                    filters.keySet()
                            .stream().filter(f-> f.startsWith("$")) // make sure prefixed with "$". Weirdly, including filter without "$" will add or 1=1 to the condition (????)
                            .forEach(f -> {
//                                System.out.println("&&&&&&&&&&&&&:"+f);
                                paramPredicates.add(createPredicate(root, cb, mapJoinPrev, f));
                            });
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

//                        if ("$".equals(col[0])) {
//                            lForm = form;
//                        } else
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

//            System.out.println(cb.and(predicates.toArray(new Predicate[0])).);

            return cb.and(cb.and(predicates.toArray(new Predicate[0])),
                    params);

        };
    }

    private Predicate createPredicate(Root<Entry> root, CriteriaBuilder cb, Join<Entry, Entry> mapJoinPrev, String f) {
        List<Predicate> paramPredicates = new ArrayList<>();
        if (filters.get(f) != null) {

            String filterValue = String.valueOf(filters.get(f));
            String[] splitted1 = f.split("\\.");
            String rootCol = splitted1[0]; // data or prev

            // $ = data, $prev$ = prev, $$ = approval
            // $$.484.college

            Path<?> predRoot = null;

            long tierId;
            String fieldFull = ""; //$$.123.lookup.code -> lookup.code
            String fieldCode = ""; //$$.123.lookup.code -> lookup
            // what if list section?
            // $.address*.country.code
            // $.address*.date~from
            Form form = null;
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
                String[] splitted = f.split("\\.", 2); // utk capture $ | fieldcode.name OR $ | fieldcode.data.number~between
                fieldFull = splitted[1]; // utk capture fielcode.name
                fieldCode = (fieldFull.split("\\.")[0]).split("~")[0];
                predRoot = "$".equals(rootCol) ? root.get("data") : mapJoinPrev.get("data"); // new HibernateInlineExpression(cb, realRoot);
                form = "$".equals(rootCol) ? this.form : this.form != null ? this.form.getPrev() : null;
            }

            if (Arrays.asList("$$", "$", "$prev$").contains(rootCol)) {
//                System.out.println("param:" + f + ", fieldFull:"+ fieldFull + ", fieldCode: "+fieldCode+", value:"+filterValue);
                if (form != null && form.getItems() != null && form.getItems().get(fieldCode) != null && !fieldFull.contains("*")) {


                    String[] splitField = fieldFull.split("~");
//                    Expression<String> jsonValueString = cb.function("JSON_VALUE", String.class, predRoot, cb.literal("$." + fieldFull));
                    Expression<String> jsonValueString = cb.function("JSON_VALUE", String.class, predRoot, cb.literal("$." + splitField[0]));
                    Expression<Double> jsonValueDouble = cb.function("JSON_VALUE", Double.class, predRoot, cb.literal("$." + splitField[0]));

//                    System.out.println("...in processing");
                    if ("~null".equals(filterValue)) {
                        paramPredicates.add(cb.upper(jsonValueString).isNull());
                    } else if ("~notnull".equals(filterValue)) {
                        paramPredicates.add(cb.upper(jsonValueString).isNotNull());
                    } else {
//                        System.out.println("...bukan ~null, ~notnull");
                        if (LOOKUP_TYPES.contains(form.getItems().get(fieldCode).getType())) {
//                            System.out.println("... dlm lookup type");
                            if (fieldFull.contains("~")) { // utk handle $.lookup.data.number~between=10,11
//                                System.out.println("... ada ~");
//                                String[] splitField = fieldFull.split("~");
//                                Expression<String> jsonValueStringIn = cb.function("JSON_VALUE", String.class, predRoot, cb.literal("$." + splitField[0]));
                                if ("in".equals(splitField[1])){
                                    paramPredicates.add(cb.upper(jsonValueString).in(Arrays.stream(filterValue.toUpperCase().split(",")).map(i->i.trim()).toArray()));
                                }else if ("notin".equals(splitField[1])){
                                    paramPredicates.add(cb.not(cb.upper(jsonValueString).in(Arrays.stream(filterValue.toUpperCase().split(",")).map(i->i.trim()).toArray())));
                                }else if ("contain".equals(splitField[1])){
                                    paramPredicates.add(cb.like(cb.upper(jsonValueString), "%"+filterValue.toUpperCase()+"%"));
                                }else if ("notcontain".equals(splitField[1])){
                                    paramPredicates.add(cb.notLike(cb.upper(jsonValueString), "%"+filterValue.toUpperCase()+"%"));
                                }

                                try {
                                    if ("from".equals(splitField[1])) {
                                        paramPredicates.add(cb.greaterThanOrEqualTo(jsonValueDouble, Double.parseDouble(filterValue)));
                                    }else if ("to".equals(splitField[1])) {
                                        paramPredicates.add(cb.lessThanOrEqualTo(jsonValueDouble, Double.parseDouble(filterValue)));
                                    }else if ("between".equals(splitField[1])) {
                                        String[] time = filterValue.split(",");
                                        paramPredicates.add(cb.between(jsonValueDouble,
                                                Double.parseDouble(time[0]),
                                                Double.parseDouble(time[1])
                                        ));
                                    }
                                }catch (NumberFormatException nfe){
                                    System.out.println("Error parsing value for [App:"+this.form.getApp().getAppPath()+"]->[Form:"+this.form.getId()+"]->["+f+"]: "+filterValue);
                                }catch (Exception e){
                                    System.out.println("Error processing filter for [App:"+this.form.getApp().getAppPath()+"]->[Form:"+this.form.getId()+"]->["+f+"]: "+ e.getMessage());
                                }
                            }else{
                                paramPredicates.add(cb.like(cb.upper(jsonValueString), filterValue.toUpperCase()));
                            }
                        } else if (DATE_NUMBER_TYPES.contains(form.getItems().get(fieldCode).getType()) || List.of("$id","$counter").contains(fieldCode)) {
                            if (!filters.get(f).toString().isEmpty()) {
                                if (fieldFull.contains("~")) {
                                    try {
//                                        String[] splitField = fieldFull.split("~");
//                                        Expression<Double> jsonValueDouble = cb.function("JSON_VALUE", Double.class, predRoot, cb.literal("$." + splitField[0]));
                                        if ("from".equals(splitField[1])) {
                                            paramPredicates.add(cb.greaterThanOrEqualTo(jsonValueDouble, Double.parseDouble(filterValue)));
                                        } else if ("to".equals(splitField[1])) {
                                            paramPredicates.add(cb.lessThanOrEqualTo(jsonValueDouble, Double.parseDouble(filterValue)));
                                        } else if ("between".equals(splitField[1])) {
                                            String[] time = filterValue.split(",");
                                            paramPredicates.add(cb.between(jsonValueDouble,
                                                    Double.parseDouble(time[0]),
                                                    Double.parseDouble(time[1])
                                            ));
                                        }
                                    }catch (NumberFormatException nfe){
                                        System.out.println("Error parsing value for [App:"+this.form.getApp().getAppPath()+"]->[Form:"+this.form.getId()+"]->["+f+"]: "+filterValue);
                                    }catch (Exception e){
                                        System.out.println("Error processing filter for [App:"+this.form.getApp().getAppPath()+"]->[Form:"+this.form.getId()+"]->["+f+"]: "+ e.getMessage());
                                    }
                                } else {
                                    try {
                                        paramPredicates.add(
                                                cb.equal(
                                                        cb.function("JSON_VALUE", Double.class, predRoot, cb.literal("$." + fieldFull)),
                                                        Double.parseDouble(filterValue)
                                                )
                                        );
                                    }catch (NumberFormatException nfe){
                                        System.out.println("Error parsing value for [App:"+this.form.getApp().getAppPath()+"]->[Form:"+this.form.getId()+"]->["+f+"]: "+filterValue);
                                    }catch (Exception e){
                                        System.out.println("Error processing filter for [App:"+this.form.getApp().getAppPath()+"]->[Form:"+this.form.getId()+"]->["+f+"]: "+ e.getMessage());
                                    }
                                }
                            }
                        } else if (CHECKBOX_TYPES.contains(form.getItems().get(fieldCode).getType())) {
                            Expression<Boolean> jsonValueBoolean = cb.function("JSON_VALUE", Boolean.class, predRoot, cb.literal("$." + splitField[0]));
                            if (Boolean.parseBoolean(filterValue)) {
                                paramPredicates.add(cb.isTrue(jsonValueBoolean));
                            } else {
                                paramPredicates.add(cb.or(cb.isFalse(jsonValueBoolean), cb.isNull(jsonValueBoolean)));
                            }
                        } else if (TEXT_TYPES.contains(form.getItems().get(fieldCode).getType())) {
                            if (fieldFull.contains("~")) {
//                                String[] splitField = fieldFull.split("~");
//                                Expression<String> jsonValueStringIn = cb.function("JSON_VALUE", String.class, predRoot, cb.literal("$." + splitField[0]));

                                if ("in".equals(splitField[1])){
                                    paramPredicates.add(cb.upper(jsonValueString).in(Arrays.stream(filterValue.toUpperCase().split(",")).map(i->i.trim()).toArray()));
                                }else if ("notin".equals(splitField[1])){
                                    paramPredicates.add(cb.not(cb.upper(jsonValueString).in(Arrays.stream(filterValue.toUpperCase().split(",")).map(i->i.trim()).toArray())));
                                }else if ("contain".equals(splitField[1])){
                                    paramPredicates.add(cb.like(cb.upper(jsonValueString), "%"+filterValue.toUpperCase()+"%"));
                                }else if ("notcontain".equals(splitField[1])){
                                    paramPredicates.add(cb.notLike(cb.upper(jsonValueString), "%"+filterValue.toUpperCase()+"%"));
                                }
                            }else{
                                String searchText = "%" + filterValue + "%";
                                if ("input".equals(form.getItems().get(fieldCode).getSubType())) {
                                    // must be EXACT, cth mn mok compare staff ID
                                    searchText = filterValue;
                                }
                                paramPredicates.add(cb.like(cb.upper(jsonValueString), searchText.toUpperCase()));
                            }
                        } else {
                            // If cannot determine type
//                            System.out.println("... dlm outer else, jsonValueString:"+jsonValueString.toString()+", filterValue:"+ filterValue.toUpperCase());
                            if (fieldFull.contains("~")){

//                                System.out.println("@@@@@@");

                                if ("in".equals(splitField[1])){
//                                    System.out.println("value:"+ Arrays.stream(filterValue.split(",")).map(i->i.trim().toUpperCase()).collect(Collectors.joining()));
//                                    System.out.println("value:"+ Arrays.stream(filterValue.split(",")).map(i->i.trim().toUpperCase()).toArray());
                                    paramPredicates.add(cb.upper(jsonValueString).in(Arrays.stream(filterValue.split(",")).map(i->i.trim().toUpperCase()).toArray()));
                                }else if ("notin".equals(splitField[1])){
                                    paramPredicates.add(cb.not(cb.upper(jsonValueString).in(Arrays.stream(filterValue.toUpperCase().split(",")).map(i->i.trim()).toArray())));
                                }else if ("contain".equals(splitField[1])){
                                    paramPredicates.add(cb.like(cb.upper(jsonValueString), "%"+filterValue.toUpperCase()+"%"));
                                }else if ("notcontain".equals(splitField[1])){
                                    paramPredicates.add(cb.notLike(cb.upper(jsonValueString), "%"+filterValue.toUpperCase()+"%"));
                                }

                                try {
                                    if ("from".equals(splitField[1])) {
                                        paramPredicates.add(cb.greaterThanOrEqualTo(jsonValueDouble, Double.parseDouble(filterValue)));
                                    }else if ("to".equals(splitField[1])) {
                                        paramPredicates.add(cb.lessThanOrEqualTo(jsonValueDouble, Double.parseDouble(filterValue)));
                                    }else if ("between".equals(splitField[1])) {
                                        String[] time = filterValue.split(",");
                                        paramPredicates.add(cb.between(jsonValueDouble,
                                                Double.parseDouble(time[0]),
                                                Double.parseDouble(time[1])
                                        ));
                                    }
                                }catch (NumberFormatException nfe){
                                    System.out.println("Error parsing value for [App:"+this.form.getApp().getAppPath()+"]->[Form:"+this.form.getId()+"]->["+f+"]: "+filterValue);
                                }catch (Exception e){
                                    System.out.println("Error processing filter for [App:"+this.form.getApp().getAppPath()+"]->[Form:"+this.form.getId()+"]->["+f+"]: "+ e.getMessage());
                                }

                            }else{
                                System.out.println("-----filter eval field:"+splitField[0]+",value:"+filterValue.toLowerCase());
                                paramPredicates.add(cb.like(cb.lower(jsonValueString), filterValue.toLowerCase()));
                            }
                            // model picker masok ctok
                        }
                    }
                } else if (fieldFull.contains("*")) {
                    // after checkboxOption so checkboxOption can be executed first, then, check for section
                    // ideally check if fieldcode contain * or not, if not: the above, else: here

                    // CHECK EITHER CHECKBOXOPTION OR SECTION
                    // THEN GET FIELD_CODE, ETC

                    // SECTION SITOK

                    if (fieldFull.contains("~")) {
                        String [] fieldFullSplitted = fieldFull.split("~");
                        String fieldTranslated = fieldFullSplitted[0].replace("*", "[*]");
                        String [] fieldValueSplitted = filterValue.split(",");
                        List<Predicate> listOverlapPredicateList = new ArrayList<>();
                        for (String value : fieldValueSplitted) {
                            Expression<String> jsonValueListSearch = cb.function("JSON_SEARCH", String.class,
                                    cb.lower(predRoot.as(String.class)),
                                    cb.literal("one"),
                                    cb.literal(value.toLowerCase()), // 15-dec-2023: Remove %% wildcard from search. If need wildcard, need to be explicitly specified. cb.literal(("%" + filterValue + "%").toLowerCase())
                                    cb.nullLiteral(String.class),
                                    cb.literal("$." + fieldTranslated)
                            );

                            listOverlapPredicateList.add(cb.isNotNull(jsonValueListSearch));
                        }

                        paramPredicates.add(cb.or(listOverlapPredicateList.toArray(new Predicate[0])));

                    }else{
                        String fieldTranslated = fieldFull.replace("*", "[*]");

                        Expression<String> jsonValueListSearch = cb.function("JSON_SEARCH", String.class,
                                cb.lower(predRoot.as(String.class)),
                                cb.literal("one"),
                                cb.literal((filterValue).toLowerCase()), // 15-dec-2023: Remove %% wildcard from search. If need wildcard, need to be explicitly specified. cb.literal(("%" + filterValue + "%").toLowerCase())
                                cb.nullLiteral(String.class),
                                cb.literal("$." + fieldTranslated)
                        );
                        paramPredicates.add(cb.isNotNull(jsonValueListSearch));
                    }

                } else {
                    /// IF NOT a part of form
                    paramPredicates.add(
                            cb.like(cb.upper(cb.function("JSON_VALUE", String.class,
                                            predRoot,
                                            cb.literal("$." + fieldFull))),
                                    filterValue.toUpperCase()
                            )
                    );
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
                        try{
                            String[] splitField = fieldFull.split("~");
                            Double filterDouble = Double.parseDouble(filterValue);

                            if (fieldFull.contains("~")) {
                                if ("from".equals(splitField[1])) {
                                    paramPredicates.add(cb.greaterThanOrEqualTo(predRoot.get(fieldCode), filterDouble));
                                } else if ("to".equals(splitField[1])) {
                                    paramPredicates.add(cb.lessThanOrEqualTo(predRoot.get(fieldCode), filterDouble));
                                }
                            } else {
                                paramPredicates.add(cb.equal(predRoot.get(fieldCode), filterDouble));
                            }
                        }catch (NumberFormatException nfe){
                            System.out.println("Error parsing value for [App:"+this.form.getApp().getAppPath()+"]->[Form:"+this.form.getId()+"]->["+f+"]: "+filterValue);
                        }catch (Exception e){
                            System.out.println("Error processing filter for [App:"+this.form.getApp().getAppPath()+"]->[Form:"+this.form.getId()+"]->["+f+"]: "+ e.getMessage());
                        }
                    }
                } else if ("status".equals(fieldCode)) {
                    paramPredicates.add(
                            cb.equal(predRoot.get(fieldFull), filterValue)
                    );
                } else if ("remark".equals(fieldCode)) {
                    paramPredicates.add(
                            cb.like(cb.upper(predRoot.get(fieldFull)), ("%" + filterValue + "%").toUpperCase())
                    );
                } else if ("email".equals(fieldCode)) {
                    paramPredicates.add(
                            cb.like(cb.upper(predRoot.get(fieldFull)), filterValue.toUpperCase())
                    );
                }

            } else if ("$_".equals(rootCol)) {
                fieldCode = f.split("\\.")[1];
                String[] splitField = fieldCode.split("~");
                if ("email".equals(fieldCode)) {
                    paramPredicates.add(cb.equal(cb.upper(cb.trim(root.get("email"))), filterValue.trim().toUpperCase()));
                } else if (List.of("id","currentTier","currentTierId","currentEdit").contains(fieldCode)) {
                    paramPredicates.add(cb.equal(root.get(fieldCode), filters.get(f)));
//                } else if ("currentTier".equals(fieldCode)) {
//                    paramPredicates.add(cb.equal(root.get("currentTier"), filters.get(f)));
                } else if ("currentStatus".equals(fieldCode)) {
                    paramPredicates.add(cb.like(cb.upper(root.get("currentStatus")), filterValue.toUpperCase()));
                } else if (List.of("submissionDate","resubmissionDate","modifiedDate","createdDate").contains(splitField[0])) {
//                    System.out.println("is date");
                    if (!filterValue.isEmpty()) {


                        if (splitField.length>1) {
//                            System.out.println(splitField[0]+";"+splitField[1]+";"+filterValue);
                            if ("from".equals(splitField[1])) {
//                                System.out.println("from-"+splitField[0]+":"+filterValue);
                                paramPredicates.add(cb.greaterThanOrEqualTo(root.get(splitField[0]), new Date(Long.parseLong(filterValue))));
                            } else if ("to".equals(splitField[1])) {
//                                System.out.println("to-"+splitField[0]+":"+filterValue);
                                paramPredicates.add(cb.lessThanOrEqualTo(root.get(splitField[0]), new Date(Long.parseLong(filterValue))));
                            } else if ("between".equals(splitField[1])) {
//                                System.out.println("between-"+splitField[0]+":"+filterValue);
                                String[] time = filterValue.split(",");
                                paramPredicates.add(cb.between(root.get(splitField[0]),
                                        new Date(Long.parseLong(time[0])),
                                        new Date(Long.parseLong(time[1]))
                                ));
                            }
                        } else {
                            paramPredicates.add(cb.equal(root.get(fieldCode), new Date(Long.parseLong(filterValue))));
                        }

                    }
                }
            }
        }

        return cb.and(paramPredicates.toArray(new Predicate[0]));
    }

    // qwalker implementation
    private Predicate qWalker(Root<Entry> root, CriteriaBuilder cb, Join<Entry, Entry> mapJoinPrev, String cond, JsonNode qList, Set<String> keySet) {
        List<Predicate> plist = new ArrayList<>();

        qList.iterator().forEachRemaining(jsonNode -> {
            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
            fields.forEachRemaining(e -> {
                String key = e.getKey();
                JsonNode value = e.getValue();

                if ("$and".equals(key) || "$or".equals(key)) {
                    plist.add(qWalker(root, cb, mapJoinPrev, key, value, keySet));
                } else {
                    if (!filters.containsKey(key)) {
                        if (!value.isNull()){
                            // put dlm filter utk di retrieve dlm createPredicate
                            filters.put(key, Helper.compileTpl(e.getValue().asText(""),dataMap));
                        }
                    }
                    plist.add(createPredicate(root, cb, mapJoinPrev, key));
                    keySet.remove(key);
                }
            });
        });

        Predicate returnPredicate = null; // if no value specified

        if (plist.size()>0){
            returnPredicate = "$or".equals(cond) ? cb.or(plist.toArray(new Predicate[0])) : cb.and(plist.toArray(new Predicate[0]));
        }

        return returnPredicate;
    }

    public Predicate checkDecorator(CriteriaBuilder cb, Path<?> predRoot, String fieldFull, String filterValue){
        Predicate pred = null;
        if (fieldFull.contains("~")){
            String[] splitField = fieldFull.split("~");

            if (List.of("from","to","between").contains(splitField[1])){

                Expression<Double> jsonValueDouble = cb.function("JSON_VALUE", Double.class, predRoot, cb.literal("$." + splitField[0]));

                if ("from".equals(splitField[1])) {
                    return cb.greaterThanOrEqualTo(jsonValueDouble, Double.parseDouble(filterValue));
                } else if ("to".equals(splitField[1])) {
                    return cb.lessThanOrEqualTo(jsonValueDouble, Double.parseDouble(filterValue));
                } else if ("between".equals(splitField[1])) {
                    String[] time = filterValue.split(",");
                    return cb.between(jsonValueDouble,
                            Double.parseDouble(time[0]),
                            Double.parseDouble(time[1])
                    );
                }
            }else if (List.of("in","contain","notcontain").contains(splitField[1])){

                Expression<String> jsonValueString = cb.function("JSON_VALUE", String.class, predRoot, cb.literal("$." + splitField[0]));

                if ("in".equals(splitField[1])){
                    return cb.upper(jsonValueString).in(Arrays.stream(filterValue.toUpperCase().split(",")).map(i->i.trim()).toArray());
                }else if ("contain".equals(splitField[1])){
                    return cb.like(cb.upper(jsonValueString), "%"+filterValue.toUpperCase()+"%");
                }else if ("notcontain".equals(splitField[1])){
                    return cb.notLike(cb.lower(jsonValueString), "%"+filterValue.toLowerCase()+"%");
                }
            }else{
                return null; // return normal query
            }
        }

        return null;

    }
}
