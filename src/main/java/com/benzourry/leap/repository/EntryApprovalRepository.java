package com.benzourry.leap.repository;

import com.benzourry.leap.model.Entry;
import com.benzourry.leap.model.EntryApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EntryApprovalRepository extends JpaRepository<EntryApproval, Long>, JpaSpecificationExecutor<Entry> {

    // pass in [{json}] so can use json_query with $[0], or else not work with '$' only
//    @Query(value="update entry_approval set data = json_set(data,:path,json_query(:value,'$[0]')) where entry_approval.id = :entryId and entry_approval.tier = :tierId", nativeQuery = true)
//    void updateDataFieldScope(@Param("entryId") Long entryId,
//                              @Param("tierId") Long tierId,
//                              @Param("path") String path,
//                              @Param("value") String value);

    @Modifying
    @Query(value = "update entry_approval set deleted = false where entry=:entryId", nativeQuery = true)
    int undeleteEntry(@Param("entryId") long entryId);
}
