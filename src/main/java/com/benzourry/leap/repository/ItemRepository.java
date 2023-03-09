package com.benzourry.leap.repository;

import com.benzourry.leap.model.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    @Query(value = "select * from item i" +
//            " left join i.section s " +
//            " left join i.form f " +
            " where i.form = :formId", nativeQuery = true)
    Page<Item> findByFormId(@Param("formId") long formId, Pageable pageable);

    @Query(value = "select * from item i" +
            " where i.datasource = :lookupId", nativeQuery = true)
    List<Item> findByDatasourceId(@Param("lookupId") long lookupId);

    @Query(value = "select * from item i" +
            " where json_value(i.x,'$.bucket') = :id", nativeQuery = true)
    List<Item> findByBucketId(@Param("id") Long id);
}
