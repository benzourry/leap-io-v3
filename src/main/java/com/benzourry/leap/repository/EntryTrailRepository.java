package com.benzourry.leap.repository;

import com.benzourry.leap.model.Entry;
import com.benzourry.leap.model.EntryApprovalTrail;
import com.benzourry.leap.model.EntryTrail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface EntryTrailRepository extends JpaRepository<EntryTrail, Long>{

    @Query(value = "select eat from EntryApprovalTrail eat where " +
            " eat.entryId = :entryId")
    Page<EntryApprovalTrail> findTrailByEntryId(@Param("entryId") Long entryId,
                                Pageable pageable);

    @Query(value = "select eat from EntryTrail eat " +
//            " left join Entry e on e.id = eat.entryId " +
//            " left join e.form form " +
//            " left join form.app app " +
            "  where " +
            " eat.formId = :formId AND " +
            " (CONCAT(eat.entryId,'') like :searchText OR " +
            " UPPER(eat.remark) like :searchText OR " +
            " UPPER(eat.email) like :searchText OR  " +
            " UPPER(eat.action) like :searchText) " +
            " AND eat.action in :actions " +
            " AND (:dateFrom is null OR :dateFrom < eat.timestamp) " +
            " AND (:dateTo is null OR :dateTo > eat.timestamp) ")
    Page<EntryTrail> findTrailByFormId(@Param("formId") Long formId,
                                       @Param("searchText") String searchText,
                                       @Param("actions") List<String> actions,
                                       @Param("dateFrom") Date dateFrom,
                                       @Param("dateTo") Date dateTo,
                                Pageable pageable);

    @Query(value = "select eat from EntryTrail eat " +
            " left join Form form on form.id = eat.formId " +
//            " left join e.form form " +
            " left join form.app app " +
            "  where " +
            " app.id = :appId")
    Page<EntryApprovalTrail> findTrailByAppId(@Param("appId") Long appId,
                                Pageable pageable);
}
