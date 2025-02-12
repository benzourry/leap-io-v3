package com.benzourry.leap.repository;

import com.benzourry.leap.model.Cogna;
import com.benzourry.leap.model.CognaPromptHistory;
import com.benzourry.leap.model.CognaSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

//import java.awt.print.Pageable;


@Repository
public interface CognaPromptHistoryRepository extends JpaRepository<CognaPromptHistory, Long> {

//    @Query("select s from Screen s where s.form.id = :formId")
//    Page<Screen> findByFormId(@Param("formId") long formId, Pageable pageable);


//    @Query("select s from Screen s where s.dataset.id = :dsId")
//    List<Screen> findByDatasetId(@Param("dsId") long dsId);

    @Query("select s from CognaPromptHistory s where " +
            " (:email IS NULL OR s.email = :email) " +
            " AND s.cognaId = :cognaId")
    Page<CognaPromptHistory> findByCognaId(@Param("cognaId") long cognaId,
                                           @Param("email") String email,
                                           Pageable pageable);


//    @Query("select s from CognaSource s where s.scheduled=TRUE and s.clock = :clock")
//    List<CognaSource> findScheduledByClock(@Param("clock") String clock);

}
