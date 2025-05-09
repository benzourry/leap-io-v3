package com.benzourry.leap.repository;

import com.benzourry.leap.model.Entry;
import com.benzourry.leap.model.EntryApprovalTrail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EntryApprovalTrailRepository extends JpaRepository<EntryApprovalTrail, Long>, JpaSpecificationExecutor<Entry> {

    @Query(value = "select eat from EntryApprovalTrail eat where " +
            " eat.entryId = :entryId")
    Page<EntryApprovalTrail> findTrailByEntryId(@Param("entryId") Long entryId,
                                Pageable pageable);

    @Query(value = "select eat from EntryApprovalTrail eat " +
            " left join Entry e on e.id = eat.entryId " +
            " left join e.form form " +
//            " left join form.app app " +
            "  where " +
            " form.id = :formId AND " +
            " (CONCAT(eat.entryId,'') like :searchText OR " +
            " UPPER(eat.remark) like :searchText OR " +
            " UPPER(eat.email) like :searchText OR  " +
            " UPPER(eat.status) like :searchText) ")
    Page<EntryApprovalTrail> findTrailByFormId(@Param("formId") Long formId,
                                               @Param("searchText") String searchText,
                                Pageable pageable);

    @Query(value = "select eat from EntryApprovalTrail eat " +
            " left join Entry e on e.id = eat.entryId " +
            " left join e.form form " +
            " left join form.app app " +
            "  where " +
            " app.id = :appId")
    Page<EntryApprovalTrail> findTrailByAppId(@Param("appId") Long appId,
                                Pageable pageable);


//    @Query(value = "select count(*) as total from entry_approval_trail n left join form f on n.form_id = f.id where f.app = :appId", nativeQuery = true)
//    long countByAppId(@Param("appId") Long appId);

}
