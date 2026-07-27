package com.benzourry.leap.repository;

import com.benzourry.leap.model.Entry;
import com.benzourry.leap.model.EntryApproval;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.stream.Stream;

import static org.hibernate.jpa.QueryHints.HINT_CACHEABLE;
import static org.hibernate.jpa.QueryHints.HINT_READONLY;

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



    @Query(value = "select ea from EntryApproval ea " +
            "join fetch ea.entry e " +
            "left join fetch e.prevEntry " +
            "where ea.tierId = :tierId and ea.deleted = false " +
            " and e.deleted = false" +
            " and (e.live = :live)")    @QueryHints(value = {
//            @QueryHint(name = HINT_FETCH_SIZE, value = "" + Integer.MIN_VALUE),
            @QueryHint(name = HINT_CACHEABLE, value = "false"),
            @QueryHint(name = HINT_READONLY, value = "true"),
//            @QueryHint(name = HINT_PASS_DISTINCT_THROUGH, value = "false")
    })
    Stream<EntryApproval> findByTierId(@Param("tierId") Long tierId, @Param("live") Boolean live);

}
