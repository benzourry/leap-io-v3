package com.benzourry.leap.repository;

import com.benzourry.leap.model.Tab;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TabRepository extends JpaRepository<Tab, Long> {

    @Query("select s from Tab s where s.form.id = :formId")
    Page<Tab> findByFormId(@Param("formId") long formId, Pageable pageable);

}
