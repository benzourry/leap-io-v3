package com.benzourry.leap.repository;

import com.benzourry.leap.model.DatasetFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DatasetFilterRepository extends JpaRepository<DatasetFilter, Long> {

    @Modifying
    @Query("delete from DatasetFilter s where s.code = :code and s.dataset.id = :dsId")
    void deleteByDatasetIdAndCode(@Param("code") String code,
                                  @Param("dsId") Long dsId);

//    @Query("select s from Dataset s where s.form.id = :formId")
//    Page<DatasetItem> findByFormId(@Param("formId") long formId, Pageable pageable);
}
