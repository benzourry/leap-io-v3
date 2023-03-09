package com.benzourry.leap.repository;

import com.benzourry.leap.model.DatasetItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DatasetItemRepository extends JpaRepository<DatasetItem, Long> {

//    @Query("select s from Dataset s where s.form.id = :formId")
//    Page<DatasetItem> findByFormId(@Param("formId") long formId, Pageable pageable);

//    @Transactional
//    @Modifying
//    @Query("delete from DatasetItem di where di.code = :code AND di.dataset.form.id = :formId")
//    void deleteByCodeAndFormId(@Param("code") String code,@Param("formId") Long formId);


    @Query("select di from DatasetItem di " +
            " left join di.dataset dataset " +
            " left join dataset.form form " +
            " where di.code = :code AND form.id = :formId")
    List<DatasetItem> findByCodeAndFormId(@Param("code") String code, @Param("formId") Long formId);

}
