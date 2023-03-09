package com.benzourry.leap.repository;

import com.benzourry.leap.model.Entry;
import com.benzourry.leap.utility.OptionalBooleanBuilder;
import lombok.Builder;
//import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
//import org.hibernate.query.criteria.internal.compile.RenderingContext;
//import org.hibernate.query.criteria.internal.expression.LiteralExpression;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Repository
public class ChartQuery {

//    Long formId;
//    String searchText;
//    List<String> status;
//    String email;
//    String admin;
//    Date submissionDateFrom;
//    Date submissionDateTo;
//
//    Map<String, Object> filters;

    @PersistenceContext
    private EntityManager em;

//    public void setFilter(Long formId,
//            String searchText,
//            List<String> status,
//            String email,
//            String admin,
//            Date submissionDateFrom,
//            Date submissionDateTo){
//        this.formId = formId;
//        this.searchText = searchText;
//        this.status = status;
//        this.email = email;
//        this.admin = admin;
//        this.submissionDateFrom = submissionDateFrom;
//        this.submissionDateTo = submissionDateTo;
//    }

    public List<Tuple> chartFilter(String categoryField, String categoryParent,
                                   String valueField, String valueParent, String agg, Filter filter) {
//        return (root, cq, cb) -> {

//        EntityManager em = entityManagerFactory.createEntityManager();
        List<String> conditions = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        if (filter.filters != null) {

            filter.filters.keySet().forEach(f -> {

                if (filter.filters.get(f) != null) {

                    String [] splitted = f.split("~");

                    String type = splitted[0]; // $range,$bool or $str
                    String rootCol = splitted[1]; // data or prev
                    String col = splitted[2]; // from, to

                    if (f.contains("$range~")) {
//                            String fv = f.toString();
//                            String[] g = fv.split("~");

                        if ("from".equals(splitted[3])) {
//                            String a = "json_value("+rootCol+",'$." + col + "')>="+filter.filters.get(f);
                            conditions.add("json_value("+rootCol+",'$." + col + "')>= ?");
                            values.add(filter.filters.get(f));
//                            predicates.add(cb.greaterThanOrEqualTo(cb.function("JSON_VALUE", Double.class,
//                                    new ChartQuery.HibernateInlineExpression(cb, rootCol),
//                                    new ChartQuery.HibernateInlineExpression(cb, "'$." + col + "'")),
//                                    new LiteralExpression<>((CriteriaBuilderImpl) cb, Double.class, Double.parseDouble(filter.filters.get(f) + ""))));

                        }
                        if ("to".equals(splitted[3])) {
                            conditions.add("json_value("+rootCol+",'$." + col + "')<= ?");
                            values.add(filter.filters.get(f));
//                            predicates.add(cb.lessThanOrEqualTo(cb.function("JSON_VALUE", Double.class,
//                                    new ChartQuery.HibernateInlineExpression(cb, rootCol),
//                                    new ChartQuery.HibernateInlineExpression(cb, "'$." + col + "'")),
//                                    new LiteralExpression<>((CriteriaBuilderImpl) cb, Double.class, Double.parseDouble(filter.filters.get(f) + ""))));

                        }

                    } else if (f.contains("$bool~")) {
//                            String[] g = f.toString().split("~");

//                        if (Boolean.valueOf(filter.filters.get(f) + "")) {
                        conditions.add("json_value("+rootCol+",'$." + col + "') is ?");
                        values.add(Boolean.valueOf(filter.filters.get(f) + ""));
//                        }else{
//                            conditions.add("json_value("+rootCol+",'$." + col + "') is false");
//                        }

//                        Expression<Boolean> pre =cb.function("JSON_VALUE", Boolean.class,
//                                new ChartQuery.HibernateInlineExpression(cb, rootCol),
//                                new ChartQuery.HibernateInlineExpression(cb, "'$." + col + "'"));
//
//                        if (Boolean.valueOf(filter.filters.get(f) + "")) {
//                            predicates.add(cb.isTrue(pre));
//                        } else {
//                            predicates.add(cb.or(cb.isFalse(pre),cb.isNull(pre)));
//                        }
                    } else if (f.contains("$str~")) {
                        conditions.add("upper(json_value("+rootCol+",'$." + col + "')) like ?");
                        values.add((filter.filters.get(f) + "").toUpperCase());
//                        predicates.add(
//                                cb.like(cb.upper(cb.function("JSON_VALUE", String.class,
//                                        new ChartQuery.HibernateInlineExpression(cb, rootCol),
//                                        new ChartQuery.HibernateInlineExpression(cb, "'$." + col + "'"))),
//                                        new LiteralExpression<>((CriteriaBuilderImpl) cb, String.class, (filter.filters.get(f) + "").toUpperCase()))
//                        );
                    } else {
                        conditions.add("json_value("+rootCol+",'$." + col + "') like ?");
                        values.add(filter.filters.get(f) + "");

//                        predicates.add(
//                                cb.like(cb.function("JSON_VALUE", String.class,
//                                        new ChartQuery.HibernateInlineExpression(cb, rootCol),
//                                        new ChartQuery.HibernateInlineExpression(cb, "'$." + col + "'")),
//                                        new LiteralExpression<>((CriteriaBuilderImpl) cb, String.class, filter.filters.get(f) + ""))
//                        );
                    }
                }

            });
        }



//        String additionalFilter =

        em.createNativeQuery("select coalesce(json_value("+categoryParent+",:categoryField),'n/a') as name " +
                "    "+agg+"(json_value(e.data,:value)) as value " +
                "    from entry e " +
                "    where formId = :formId " +
                "    group by coalesce(json_value("+valueParent+",:valueField),'n/a')");

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Entry> root = query.from(Entry.class);

//            CriteriaQuery<Tuple> query = cb.createQuery(Tuple.class);


        List<Predicate> predicates = new OptionalBooleanBuilder(cb)
//                    .notNullAnd(submissionDateFrom, cb.greaterThan(root.get("submissionDate"), submissionDateFrom))
//                    .notNullAnd(submissionDateTo, cb.lessThan(root.get("submissionDate"), submissionDateTo))
//                    .notNullAnd(email, cb.like(cb.concat(",", cb.concat(
//                            cb.function("REPLACE",String.class,root.get("email"),cb.literal(" "),cb.literal(""))
//                            , ",")), "%," + email + ",%"))
//                    .notNullAnd(admin, cb.like(cb.concat(",", cb.concat(
//                            cb.function("REPLACE",String.class,root.get("email"),cb.literal(" "),cb.literal(""))
//                            , ",")), "%," + admin + ",%"))
//                    .notNullAnd(status, root.get("currentStatus").in(status))
//                    .notNullAnd(formId, cb.equal(root.get("form"), formId))
                .notNullAnd(filter.submissionDateFrom, cb.greaterThan(root.get("submissionDate"), filter.submissionDateFrom))
                .notNullAnd(filter.submissionDateTo, cb.lessThan(root.get("submissionDate"), filter.submissionDateTo))
                .notNullAnd(filter.email, cb.like(cb.concat(",", cb.concat(
                        cb.function("REPLACE",String.class,root.get("email"),cb.literal(" "),cb.literal(""))
                        , ",")), "%," + filter.email + ",%"))
                .notNullAnd(filter.admin, cb.like(cb.concat(",", cb.concat(
                        cb.function("REPLACE",String.class,root.get("email"),cb.literal(" "),cb.literal(""))
                        , ",")), "%," + filter.admin + ",%"))
//                    .notNullAnd(filter.status, root.get("currentStatus").in(filter.status))
                .notNullAnd(filter.formId, cb.equal(root.get("form"), filter.formId))
                .build();

        if (filter.filters != null) {
            filter.filters.keySet().forEach(f -> {

                if (filter.filters.get(f) != null) {

                    String [] splitted = f.split("~");

                    String type = splitted[0]; // $range,$bool or $str
                    String rootCol = splitted[1]; // data or prev
                    String col = splitted[2]; // from, to

                    if (f.contains("$range~")) {
//                            String fv = f.toString();
//                            String[] g = fv.split("~");

                        if ("from".equals(splitted[3])) {
                            predicates.add(cb.greaterThanOrEqualTo(cb.function("JSON_VALUE", Double.class,
                                    cb.literal(rootCol),
                                    cb.literal("'$." + col + "'")),
                                    Double.parseDouble(filter.filters.get(f) + "")));

                        }
                        if ("to".equals(splitted[3])) {
                            predicates.add(cb.lessThanOrEqualTo(cb.function("JSON_VALUE", Double.class,
                                    cb.literal(rootCol),
                                    cb.literal("'$." + col + "'")),
                                    Double.parseDouble(filter.filters.get(f) + "")));

                        }

                    } else if (f.contains("$bool~")) {
//                            String[] g = f.toString().split("~");

                        Expression<Boolean> pre =cb.function("JSON_VALUE", Boolean.class,
                                cb.literal(rootCol),
                                cb.literal("'$." + col + "'"));

                        if (Boolean.valueOf(filter.filters.get(f) + "")) {
                            predicates.add(cb.isTrue(pre));
                        } else {
                            predicates.add(cb.or(cb.isFalse(pre),cb.isNull(pre)));
                        }
                    } else if (f.contains("$str~")) {
                        predicates.add(
                                cb.like(cb.upper(cb.function("JSON_VALUE", String.class,
                                        cb.literal(rootCol),
                                        cb.literal("'$." + col + "'"))),
                                        (filter.filters.get(f) + "").toUpperCase())
                        );
                    } else {

                        predicates.add(
                                cb.like(cb.function("JSON_VALUE", String.class,
                                        cb.literal(rootCol),
                                        cb.literal("'$." + col + "'")),
                                        filter.filters.get(f) + "")
                        );
                    }
                }

            });
        }

            /*

    select coalesce(json_value(e.data,:code),'n/a') as name
    agg(json_value(e.data,:value)) as value
    from entry e
    where formId = :formId
    group by coalesce(json_value(e.data,:code),'n/a')

    * */

        Expression<String> name = cb.function("JSON_VALUE", String.class,
                cb.literal(categoryParent),
                cb.literal("'$."+categoryField+"'"));

        Expression<Double> value = cb.function("JSON_VALUE", Double.class,
                cb.literal(valueParent),
                cb.literal("'$."+valueField+"'"));

        Expression<Double> exc1 = null;
        Expression<Long> exc2 = null;
        if ("sum".equals(agg)){
            exc1 = cb.sum(value);
        }
        if ("count".equals(agg)){
            exc2 = cb.count(value);
        }
        if ("avg".equals(agg)){
            exc1 = cb.avg(value);
        }
        if ("max".equals(agg)){
            exc1 = cb.avg(value);
        }
        if ("min".equals(agg)){
            exc1 = cb.avg(value);
        }

        query.select(cb.tuple(name.alias("name"), "count".equals(agg)?exc2.alias("value"):exc1.alias("value")));

        query.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));

        query.groupBy(cb.function("JSON_VALUE", String.class,
                cb.literal(categoryParent),
                cb.literal("'$."+categoryField+"'")));


        TypedQuery<Tuple> typedQuery = em.createQuery(query);
        List<Tuple> resultList = typedQuery.getResultList();
//            resultList.forEach(tuple -> {
//                System.out.printf("Name: %s, Value: %s%n",
//                        tuple.get(0, String.class), tuple.get(1, Double.class));
//            });


        return resultList;
//            cq.distinct(true);
//            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
//        };
    }

//    static class HibernateInlineExpression extends LiteralExpression<String> {
//
//        public HibernateInlineExpression(CriteriaBuilder criteriaBuilder, String literal) {
//            super((CriteriaBuilderImpl) criteriaBuilder, literal);
//        }
//
//        @Override
//        public String render(RenderingContext renderingContext) {
//            return getLiteral();
//        }
//    }

    @Builder
    static public class Filter{
        Long formId;
        String searchText;
        List<String> status;
        String email;
        String admin;
        Date submissionDateFrom;
        Date submissionDateTo;
        Map<String, Object> filters;

    }


}
