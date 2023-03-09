//package com.benzourry.reka.repository;
//
//import com.benzourry.reka.model.Element;
//import com.benzourry.reka.model.Item;
//import com.benzourry.reka.model.Model;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//@Repository
//public interface ModelRepository extends JpaRepository<Model, Long> {
//
////    @Query("select i from Model i" +
//////            " left join i.section s " +
////            " left join i.form f " +
////            " where f.id = :formId")
////    Page<Model> findByFormId(@Param("formId") long formId, Pageable pageable);
//}
