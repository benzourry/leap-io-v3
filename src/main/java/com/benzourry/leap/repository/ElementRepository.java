//package com.benzourry.reka.repository;
//
////import com.benzourry.reka.model.Element;
//import com.benzourry.reka.model.Item;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//@Repository
//public interface ElementRepository extends JpaRepository<Element, Long> {
//
//    @Query("select i from Element i" +
////            " left join i.section s " +
//            " left join i.form f " +
//            " where f.id = :formId")
//    Page<Item> findByFormId(@Param("formId") long formId, Pageable pageable);
//}
