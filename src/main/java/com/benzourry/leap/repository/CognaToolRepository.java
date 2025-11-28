package com.benzourry.leap.repository;

import com.benzourry.leap.model.CognaTool;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

//import java.awt.print.Pageable;


@Repository
public interface CognaToolRepository extends JpaRepository<CognaTool, Long> {

//    @Query("select s from Screen s where s.form.id = :formId")
//    Page<Screen> findByFormId(@Param("formId") long formId, Pageable pageable);


//    @Query("select s from Screen s where s.dataset.id = :dsId")
//    List<Screen> findByDatasetId(@Param("dsId") long dsId);

    @Query("select s from CognaTool s where s.cogna.id = :cognaId")
    Page<CognaTool> findByCognaId(@Param("cognaId") long cognaId,
                              Pageable pageable);


//    @Query("select s from CognaSource s where s.scheduled=TRUE and s.clock = :clock")
//    List<CognaTool> findScheduledByClock(@Param("clock") String clock);

}
