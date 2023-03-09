package com.benzourry.leap.repository;

import com.benzourry.leap.model.Action;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScreenActionRepository extends JpaRepository<Action, Long> {

//    @Query("select s from Screen s where s.form.id = :formId")
//    Page<Screen> findByFormId(@Param("formId") long formId, Pageable pageable);

//    @Query("select s from Screen s where s.app.id = :appId")
//    List<Screen> findByAppId(@Param("appId") long appId);
}
