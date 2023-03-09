package com.benzourry.leap.filter;

import com.benzourry.leap.model.App;
import com.benzourry.leap.utility.OptionalBooleanBuilder;
import lombok.Builder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import java.util.List;

@Builder
public class AppFilter {



    List<Long> ids;
    String searchText;
    String email;
    String emailNot;
    String appPath;
    List<String> status; // LOCAL,TEMPLATE, PUBLISHED
    List<String> tag;
//    Double priceFrom;
//    Double priceTo;
    Long cloneFrom;
    Long cloneTo;
    Boolean useGoogle;
    Boolean useUnimas;
    Boolean publicAccess;
    Boolean shared;


    public Specification<App> filter(){
        return (root, cq, cb) ->{
//            System.out.println(",,,,"+status);
//            System.out.println("cccc"+root.get("status").in(status));

            CriteriaBuilder.In<String> tagIn = cb.in(root.get("tag"));
            if (tag!=null) {
                tag.forEach(p -> tagIn.value(p));
            }
            CriteriaBuilder.In<String> statusIn = cb.in(root.get("status"));
            if (status!=null) {
                status.forEach(p -> statusIn.value(p));
            }
            CriteriaBuilder.In<Long> idIn = cb.in(root.get("id"));
            if (ids!=null) {
                ids.forEach(p -> idIn.value(p));
            }



            List<Predicate> predicates = new OptionalBooleanBuilder(cb)
                    .notNullAnd(searchText, cb.or(
                            cb.like(cb.upper(root.get("title")), searchText),
                            cb.like(cb.upper(root.get("description")), searchText),
                            cb.like(cb.upper(root.get("appPath")), searchText)
                    ))
//                    .notNullAnd(priceFrom, cb.greaterThan(root.get("price"), priceFrom))
//                    .notNullAnd(priceTo, cb.lessThan(root.get("price"), priceTo))
                    .notNullAnd(cloneFrom, cb.greaterThan(root.get("clone"), cloneFrom))
                    .notNullAnd(cloneTo, cb.lessThan(root.get("clone"), cloneTo))
                    .notNullAnd(email, cb.like(cb.concat(",",cb.concat(
                            cb.function("REPLACE",String.class,root.get("email"),cb.literal(" "),cb.literal(""))
                            ,",")), "%," + email + ",%"))
                    .notNullAnd(emailNot, cb.notLike(cb.concat(",", cb.concat(
                            cb.function("REPLACE",String.class,root.get("email"),cb.literal(" "),cb.literal(""))
                            , ",")), "%," + emailNot + ",%"))
                    .notNullAnd(appPath, cb.equal(root.get("appPath"), appPath))
//                    .notNullAnd(status, root.get("status").in(status))
                    .notNullAnd(status, statusIn)
//                    .notNullAnd(tag, root.get("tag").in(tag))
                    .notNullAnd(tag, tagIn)
//                    .notNullAnd(ids, root.get("id").in(ids))
                    .notNullAnd(ids, idIn)
                    .notNullAnd(useUnimas,cb.equal(root.get("useUnimas"), useUnimas))
                    .notNullAnd(useGoogle, cb.equal(root.get("useGoogle"), useGoogle))
//                    .notNullAnd(shared, cb.equal(root.get("shared"), shared))
                    .build();
            cq.distinct(true);
//            System.out.println("size:"+predicates.size());
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }
}
