package com.benzourry.leap.repository;

import com.benzourry.leap.model.Entry;
import com.benzourry.leap.model.EntryApproval;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.hibernate.jpa.QueryHints.HINT_CACHEABLE;
import static org.hibernate.jpa.QueryHints.HINT_READONLY;

@Repository
public interface EntryRepository extends JpaRepository<Entry, Long>, JpaSpecificationExecutor<Entry> {

    @Query(value = "select * from entry where form = :formId and deleted = false and live = :live", nativeQuery = true)
    Page<Entry> findByFormId(@Param("formId") Long formId, @Param("live") Boolean live, Pageable pageable);


    @Query(value = "select e from Entry e where e.form.id = :formId and e.deleted = false and (e.live = :live)")
    @QueryHints(value = {
//            @QueryHint(name = HINT_FETCH_SIZE, value = "" + Integer.MIN_VALUE),
            @QueryHint(name = HINT_CACHEABLE, value = "false"),
            @QueryHint(name = HINT_READONLY, value = "true"),
//            @QueryHint(name = HINT_PASS_DISTINCT_THROUGH, value = "false")
    })
    Stream<Entry> findByFormId(@Param("formId") Long formId, @Param("live") Boolean live);

    @Query(value = "select e from Entry e where e.form.id = :formId and json_value(e.data,:path) = :value and e.deleted = false and (e.live = :live)")
    @QueryHints(value = {
//            @QueryHint(name = HINT_FETCH_SIZE, value = "" + Integer.MIN_VALUE),
            @QueryHint(name = HINT_CACHEABLE, value = "false"),
            @QueryHint(name = HINT_READONLY, value = "true"),
//            @QueryHint(name = HINT_PASS_DISTINCT_THROUGH, value = "false")
    })
    Stream<Entry> findByFormIdAndDataPathWithId(@Param("formId") Long formId,@Param("path") String path,@Param("value") Object value, @Param("live") Boolean live);

    @Query(value = "select e from Entry e where e.form.id = :formId and JSON_SEARCH(e.data, 'one', :value, NULL, :path) IS NOT NULL and e.deleted = false and (e.live = :live)")
    @QueryHints(value = {
//            @QueryHint(name = HINT_FETCH_SIZE, value = "" + Integer.MIN_VALUE),
            @QueryHint(name = HINT_CACHEABLE, value = "false"),
            @QueryHint(name = HINT_READONLY, value = "true"),
//            @QueryHint(name = HINT_PASS_DISTINCT_THROUGH, value = "false")
    })
    Stream<Entry> findByFormIdAndDataPathMultiWithId(@Param("formId") Long formId,@Param("path") String path,@Param("value") Object value, @Param("live") Boolean live);

    // update entry_approval set data = json_set(data,:path,json_query(:value,'$[0]')) where entry_approval.entry = :entryId and entry_approval.tier = :tierId

    @Query(value = "select e from EntryApproval e where e.tierId = :tierId and json_value(e.data,:path) = :value")
    @QueryHints(value = {
//            @QueryHint(name = HINT_FETCH_SIZE, value = "" + Integer.MIN_VALUE),
            @QueryHint(name = HINT_CACHEABLE, value = "false"),
            @QueryHint(name = HINT_READONLY, value = "true"),
//            @QueryHint(name = HINT_PASS_DISTINCT_THROUGH, value = "false")
    })
    Stream<EntryApproval> findByTierIdAndApprovalDataPathWithId(@Param("tierId") Long tierId, @Param("path") String path, @Param("value") Object value);

    @Query(value = "select e from EntryApproval e where e.tierId = :tierId and JSON_SEARCH(e.data, 'one', :value, NULL, :path) IS NOT NULL")
    @QueryHints(value = {
//            @QueryHint(name = HINT_FETCH_SIZE, value = "" + Integer.MIN_VALUE),
            @QueryHint(name = HINT_CACHEABLE, value = "false"),
            @QueryHint(name = HINT_READONLY, value = "true"),
//            @QueryHint(name = HINT_PASS_DISTINCT_THROUGH, value = "false")
    })
    Stream<EntryApproval> findByTierIdAndApprovalDataPathMultiWithId(@Param("tierId") Long tierId, @Param("path") String path, @Param("value") Object value);


    @Query(value = "select count(*) as total from entry e" +
            " left join form f on e.form = f.id where f.app = :appId", nativeQuery = true)
    Long statTotalCount(@Param("appId") Long appId);


    @Query(value = "select sum(length(e.data)) as total from entry e" +
            " left join form f on e.form = f.id where f.app = :appId", nativeQuery = true)
    Long statTotalSize(@Param("appId") Long appId);

    @Query(value = "select concat(f.title,':',f.id) as name, count(e.id) as `value` from entry e " +
            " left join form f on e.form = f.id " +
            " where f.app = :appId" +
            " group by concat(f.title,':',f.id)", nativeQuery = true)
    List<Map> statCountByForm(@Param("appId") Long appId);

    @Query(value = "select a.title as name, count(e.id) as `value` from entry e " +
            " left join form f on e.form = f.id " +
            " left join app a on f.app = a.id " +
            " group by a.title" +
            " order by count(e.id) desc " +
            " limit 10", nativeQuery = true)
    List<Map> statCountByApp();


    @Query(value = "select * from (select date_format(e.created_date,'%Y-%m') as name, count(e.id) as `value` from entry e " +
            " left join form f on e.form = f.id " +
            " where f.app = :appId" +
            " group by date_format(e.created_date,'%Y-%m') " +
            " order by date_format(e.created_date,'%Y-%m') desc limit 10 ) as sub order by name asc", nativeQuery = true)
    List<Map> statCountByYearMonth(@Param("appId") Long appId);


    /**
     * Perlu sort asc lok supaya dpt roll over total. Lepas ya baruk desc + limit 10 n then finally baruk asc lagik.
     * @param appId
     * @return
     */
    @Query(value = "select * from (select sub.name, sum(sub.value) over (order by sub.name) as `value` from (" +
            " select date_format(e.created_date,'%Y-%m') as name, count(e.id) as `value` from entry e " +
            " left join form f on e.form = f.id " +
            " where f.app = :appId" +
            " group by date_format(e.created_date,'%Y-%m') " +
            " order by date_format(e.created_date,'%Y-%m') asc " +
            " ) as sub order by sub.name desc limit 10) as sub2 order by sub2.name asc", nativeQuery = true)
    List<Map> statCountByYearMonthCumulative(@Param("appId") Long appId);


    @Query(value = "select * from (select date_format(e.created_date,'%Y-%m') as name, count(e.id) as `value` from entry e " +
            " where date_format(e.created_date,'%Y-%m') is not null " +
            " group by date_format(e.created_date,'%Y-%m') " +
            " order by date_format(e.created_date,'%Y-%m') desc " +
            " limit 10 ) as sub order by name asc", nativeQuery = true)
    List<Map> statCountByYearMonth();

    @Query(value = "select * from (select sub.name, sum(sub.value) over (order by sub.name) as `value` from (" +
            " select date_format(e.created_date,'%Y-%m') as name, count(e.id) as `value` from entry e " +
            " where date_format(e.created_date,'%Y-%m') is not null " +
            " group by date_format(e.created_date,'%Y-%m') " +
            " order by date_format(e.created_date,'%Y-%m') asc " +
            " ) as sub order by sub.name desc limit 10) as sub2 order by sub2.name asc", nativeQuery = true)
    List<Map> statCountByYearMonthCumulative();

    @Modifying
    @Query(value = "delete from entry_approver where entry_id in (select id from entry where form = :formId)", nativeQuery = true)
    int deleteApproverByFormId(@Param("formId") Long formId);

    @Modifying
    @Query(value = "delete from entry_approval where entry in (select id from entry where form = :formId)", nativeQuery = true)
    int deleteApprovalByFormId(@Param("formId") Long formId);

    @Modifying
    @Query(value = "delete from entry_trail where form_id = :formId", nativeQuery = true)
    int deleteTrailByFormId(@Param("formId") Long formId);

    @Modifying
    @Query(value = "delete from entry_approval_trail where tier in (select id from tier where form = :formId)", nativeQuery = true)
    int deleteApprovalTrailByFormId(@Param("formId") Long formId);

    @Modifying
    @Query(value = "delete from entry where form = :formId", nativeQuery = true)
    int deleteByFormId(@Param("formId") Long formId);

    @Modifying
    @Query(value = "delete from entry_approval_trail where tier=:tierId", nativeQuery = true)
    int deleteApprovalTrailByTierId(@Param("tierId") Long tierId);

    // update entry set data = json_set(data,'$.field',value);
//    @Query(value = "update entry set data = json_set(data,:field,:value) where entry.id = :entryId", nativeQuery = true)
//    void updateField(@Param("entryId") Long entryId, @Param("field") String field, @Param("value") String value);
    // update entry set data = json_set(data,'$.field',value);


    @Modifying(clearAutomatically = true)
    @Query(value = "update entry set data = :value where entry.id = :entryId", nativeQuery = true)
    void updateDataField(@Param("entryId") Long entryId, @Param("value") String value);

    // pass in [{json}] so can use json_query with $[0], or else not work with '$' only
    @Modifying(clearAutomatically = true)
    @Query(value = "update entry set data = json_set(data,:path,json_query(:value,'$[0]')) where entry.id = :entryId", nativeQuery = true)
    void updateDataFieldScope(@Param("entryId") Long entryId, @Param("path") String path, @Param("value") String value);

    //    @Query(value="update entry set data = json_set(data,:path,json_query(:value,'$[0]')) where entry.id = :entryId", nativeQuery = true)
//    void updateApprovalFieldScope(@Param("entryId") Long entryId,@Param("path") String path,@Param("value") String value);
    @Modifying(clearAutomatically = true)
    @Query(value = "update entry_approval set data = json_set(data,:path,json_query(:value,'$[0]')) where entry_approval.entry = :entryId and entry_approval.tier_id = :tierId", nativeQuery = true)
    void updateApprovalDataFieldScope(@Param("entryId") Long entryId,
                              @Param("tierId") Long tierId,
                              @Param("path") String path,
                              @Param("value") String value);

    @Modifying(clearAutomatically = true)
    @Query(value = "update entry_approval set data = json_set(data,:path,json_query(:value,'$[0]')) where entry_approval.id = :entryApprovalId", nativeQuery = true)
    void updateApprovalDataFieldScope2(@Param("entryApprovalId") Long entryApprovalId,
                              @Param("path") String path,
                              @Param("value") String value);


    @Modifying
    @Query(value = "update entry set deleted=false where id=:entryId", nativeQuery = true)
    int undeleteEntry(@Param("entryId") long entryId);


//    @Query(value = """
//             select entry.id,length(entry.data) as size,entry.email, entry.created_date, entry.created_by,
//             entry.modified_date, entry.modified_by, entry.form as form_id, form.title as form_title,
//             app.id as app_id, app.title as app_title
//             from entry
//             left join form on entry.form = form.id
//             left join app on form.app = app.id
//             where length(entry.data)> :size""", nativeQuery = true)
//    Page<Map> findLargeEntry(@Param("size") Long size,
//                        Pageable pageable);


}
