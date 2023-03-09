package com.benzourry.leap.utility;

//import javafx.beans.binding.BooleanExpression;
//import org.apache.poi.ss.formula.functions.T;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class OptionalBooleanBuilder {

    private List<Predicate> predicates;
    private CriteriaBuilder cb;

    public OptionalBooleanBuilder(CriteriaBuilder cb) {
        predicates = new ArrayList<>();
        this.cb = cb;
    }

    public OptionalBooleanBuilder notNullAnd(Object value,Predicate p) {
        if (value != null) {
            predicates.add(p);
//            return new OptionalBooleanBuilder(predicate.and(p));
        }
        return this;
    }

    public OptionalBooleanBuilder and(Predicate p) {
//        if (value != null) {
            predicates.add(p);
//            return new OptionalBooleanBuilder(predicate.and(p));
//        }
        return this;
    }

    public OptionalBooleanBuilder notEmptyAnd(Object value, Predicate p) {
        if (!StringUtils.isEmpty(value)) {
            predicates.add(p);
//            return new OptionalBooleanBuilder(predicate.and(expressionFunction.apply(value)));
        }
        return this;
    }



    public <T> OptionalBooleanBuilder notEmptyAnd(Collection<T> value, Predicate p) {
        if (!CollectionUtils.isEmpty(value)) {
            predicates.add(p);
//            return new OptionalBooleanBuilder(predicate.and(expressionFunction.apply(collection)));
        }
        return this;
    }

    public List<Predicate> build() {
        return predicates;
//        return predicates;
    }

    public Predicate[] buildArray() {
        return predicates.toArray(new Predicate[]{});
//        return predicates;
    }
}