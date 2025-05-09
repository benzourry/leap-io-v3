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

    @Query(value = "select * from entry where form = :formId", nativeQuery = true)
    Page<Entry> findByFormId(@Param("formId") Long formId, Pageable pageable);


    @Query(value = "select e from Entry e where e.form.id = :formId")
    @QueryHints(value = {
//            @QueryHint(name = HINT_FETCH_SIZE, value = "" + Integer.MIN_VALUE),
            @QueryHint(name = HINT_CACHEABLE, value = "false"),
            @QueryHint(name = HINT_READONLY, value = "true"),
//            @QueryHint(name = HINT_PASS_DISTINCT_THROUGH, value = "false")
    })
    Stream<Entry> findByFormId(@Param("formId") Long formId);

    @Query(value = "select e from Entry e where e.form.id = :formId and json_value(e.data,:path) = :value")
    @QueryHints(value = {
//            @QueryHint(name = HINT_FETCH_SIZE, value = "" + Integer.MIN_VALUE),
            @QueryHint(name = HINT_CACHEABLE, value = "false"),
            @QueryHint(name = HINT_READONLY, value = "true"),
//            @QueryHint(name = HINT_PASS_DISTINCT_THROUGH, value = "false")
    })
    Stream<Entry> findByFormIdAndDataPathWithId(@Param("formId") Long formId,@Param("path") String path,@Param("value") Object value);

    // update entry_approval set data = json_set(data,:path,json_query(:value,'$[0]')) where entry_approval.entry = :entryId and entry_approval.tier = :tierId

    @Query(value = "select e from EntryApproval e where e.tierId = :tierId and json_value(e.data,:path) = :value")
    @QueryHints(value = {
//            @QueryHint(name = HINT_FETCH_SIZE, value = "" + Integer.MIN_VALUE),
            @QueryHint(name = HINT_CACHEABLE, value = "false"),
            @QueryHint(name = HINT_READONLY, value = "true"),
//            @QueryHint(name = HINT_PASS_DISTINCT_THROUGH, value = "false")
    })
    Stream<EntryApproval> findByTierIdAndApprovalDataPathWithId(@Param("tierId") Long tierId, @Param("path") String path, @Param("value") Object value);


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

//            "select date_format(e.created_date,'%Y-%m') as name, count(e.id) as `value` from entry e " +
//            " where date_format(e.created_date,'%Y-%m') is not null " +
//            " group by date_format(e.created_date,'%Y-%m') " +
//            " order by date_format(e.created_date,'%Y-%m') asc " +
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



    //distinct e.
//    @Query(value = "select * from entry where " +
//            " upper(data) like :searchText " +
//            " and current_status in :status " +
//            " and form = :formId", nativeQuery = true)
//    Page<Entry> findAll(@Param("formId") Long formId,
//                        @Param("searchText") String searchText,
//                        @Param("status") List<String> status,
//                        Pageable pageable);

//    @Query(value = "select count(*) from entry where " +
//            " upper(data) like :searchText " +
//            " and current_status in :status " +
//            " and form = :formId", nativeQuery = true)
//    long countAll(@Param("formId") Long formId,
//                  @Param("searchText") String searchText,
//                  @Param("status") List<String> status);
    //distinct e.
//    @Query(value = "select * from entry e where " +
//            " upper(e.data) like :searchText " +
//            " and e.current_status in :status " +
//            " and e.email = :email " +
//            " and e.form = :formId", nativeQuery = true)
//    Page<Entry> findUserByEmail(@Param("formId") Long formId,
//                                @Param("searchText") String searchText,
//                                @Param("email") String email,
//                                @Param("status") List<String> status,
//                                Pageable pageable);

//    @Query(value = "select count(*) from entry where " +
//            " upper(data) like :searchText " +
//            " and current_status in :status " +
//            " and email = :email " +
//            " and form = :formId", nativeQuery = true)
//    long countUserByEmail(@Param("formId") Long formId,
//                          @Param("searchText") String searchText,
//                          @Param("email") String email,
//                          @Param("status") List<String> status);

    //distinct e.
//    @Query(value = "SELECT * FROM entry e " +
//            " left join form form on form.id = e.form " +
//            " WHERE " +
//            " (e.data like :searchText) " +
//            " AND e.current_status IN :status " +
//            " AND (lower(concat(',',form.admin,',')) like concat('%',lower(concat(',',:email,',')),'%')) " +
//            " AND form.id = :formId", nativeQuery = true)
//    Page<Entry> findAdminByEmail(@Param("formId") Long formId,
//                                 @Param("searchText") String searchText,
//                                 @Param("email") String email,
//                                 @Param("status") List<String> status,
//                                 Pageable pageable);

//    @Query(value = "SELECT count(*) FROM entry e " +
//            " left join form form on form.id = e.form " +
//            " WHERE " +
//            " (e.data like :searchText) " +
//            " AND e.current_status IN :status " +
//            " AND (lower(concat(',',REGEXP_REPLACE(form.admin,'[\r\n ]',''),',')) like concat('%',lower(concat(',',:email,',')),'%')) " +
//            " AND form.id = :formId", nativeQuery = true)
//    long countAdminByEmail(@Param("formId") Long formId,
//                           @Param("searchText") String searchText,
//                           @Param("email") String email,
//                           @Param("status") List<String> status);

    //distinct e.
//    @Query(value = "SELECT * FROM entry e " +
//            " left join tier tier on tier.form=e.form " +
//            " left join entry_approver approver on approver.entry_id=e.id " +
//            " where (UPPER(e.data) like :searchText) " +
//            " and e.form = :formId " +
////            " and e.current_status in :status " +
//            " and e.current_tier = tier.sort_order " +
//            " and ((tier.type = 'ALL')" +
//            " OR (tier.type IN ('DYNAMIC','ASSIGN') AND approver.tier_id = tier.id AND concat(',',REGEXP_REPLACE(approver.approver,'[\r\n ]',''),',') like concat('%',concat(',',:email,','),'%'))" +
//            // REGEXP_REPLACE(a.users,'[\r\n ]','')
//            " OR (tier.type IN ('FIXED','GROUP') AND concat(',',REGEXP_REPLACE(tier.approver,'[\r\n ]',''),',') like concat('%',concat(',',:email,','),'%')))" +
//            " group by e.id", nativeQuery = true)
//    Page<Entry> findActionByEmail(@Param("formId") Long formId,
//                                  @Param("searchText") String searchText,
//                                  @Param("email") String email,
////                              @Param("tgroup") List<String> group,
////                                  @Param("status") List<String> status,
//                                  Pageable pageable);

//    @Query(value = "select count(*) from (SELECT e.id FROM entry e " +
//            " left join tier tier on tier.form=e.form " +
//            " left join entry_approver approver on approver.entry_id=e.id " +
//            " where (UPPER(e.data) like :searchText) " +
//            " and e.form = :formId " +
////            " and e.current_status in :status " +
//            " and e.current_tier = tier.sort_order " +
//            " and ((tier.type = 'ALL')" +
//            " OR (tier.type IN ('DYNAMIC','ASSIGN') AND approver.tier_id = tier.id AND concat(',',REGEXP_REPLACE(approver.approver,'[\r\n ]',''),',') like concat('%',concat(',',:email,','),'%'))" +
//            " OR (tier.type IN ('FIXED','GROUP') AND concat(',',REGEXP_REPLACE(tier.approver,'[\r\n ]',''),',') like concat('%',concat(',',:email,','),'%')))" +
//            " group by e.id) a ", nativeQuery = true)
//    long countActionByEmail(@Param("formId") Long formId,
//                            @Param("searchText") String searchText,
//                            @Param("email") String email
////                              @Param("tgroup") List<String> group,
////                            @Param("status") List<String> status
//    );
//


//    @Query(value = "select " +
////            " coalesce(json_value(e.data,:code),'n/a') as name," +
//            " case :codeRoot " +
//            "   when 'data' then coalesce(json_value(e.data,:code),'n/a') " +
//            "   when 'prev' then coalesce(json_value(e.prev,:code),'n/a') " +
//            " end as name," +
//
//            " case :valueRoot" +
//            "  when 'data' then " +
//            "    case :agg " +
//            "      when 'count' THEN count(json_value(e.data,:value)) " +
//            "      when 'sum' THEN sum(json_value(e.data,:value)) " +
//            "      when 'avg' THEN avg(json_value(e.data,:value)) " +
//            "      when 'max' THEN max(json_value(e.data,:value)) " +
//            "      when 'min' THEN min(json_value(e.data,:value)) " +
//            "    end " +
//            "  when 'prev' then " +
//            "    case :agg " +
//            "      when 'count' THEN count(json_value(e.prev,:value)) " +
//            "      when 'sum' THEN sum(json_value(e.prev,:value)) " +
//            "      when 'avg' THEN avg(json_value(e.prev,:value)) " +
//            "      when 'max' THEN max(json_value(e.prev,:value)) " +
//            "      when 'min' THEN min(json_value(e.prev,:value)) " +
//            "    end " +
//            " end as value " +
//            " from entry e " +
//            " where e.form=:formId " +
//            " and e.current_status in :status" +
//            " group by " +
//            "  case :codeRoot " +
//            "    when 'data' then json_value(e.data,:code) " +
//            "    when 'prev' then json_value(e.prev,:code) " +
//            "  end ", nativeQuery = true)
//    List<Map> getChartData(@Param("formId") Long formId,
//                           @Param("code") String code,
//                           @Param("codeRoot") String codeRoot,
//                           @Param("value") String value,
//                           @Param("valueRoot") String valueRoot,
//                           @Param("agg") String agg,
//                           @Param("status") List<String> status);

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


    @Query(value = "update entry set data = :value where entry.id = :entryId", nativeQuery = true)
    void updateDataField(@Param("entryId") Long entryId, @Param("value") String value);

    // pass in [{json}] so can use json_query with $[0], or else not work with '$' only
    @Query(value = "update entry set data = json_set(data,:path,json_query(:value,'$[0]')) where entry.id = :entryId", nativeQuery = true)
    void updateDataFieldScope(@Param("entryId") Long entryId, @Param("path") String path, @Param("value") String value);

    //    @Query(value="update entry set data = json_set(data,:path,json_query(:value,'$[0]')) where entry.id = :entryId", nativeQuery = true)
//    void updateApprovalFieldScope(@Param("entryId") Long entryId,@Param("path") String path,@Param("value") String value);
    @Query(value = "update entry_approval set data = json_set(data,:path,json_query(:value,'$[0]')) where entry_approval.entry = :entryId and entry_approval.tier_id = :tierId", nativeQuery = true)
    void updateApprovalDataFieldScope(@Param("entryId") Long entryId,
                              @Param("tierId") Long tierId,
                              @Param("path") String path,
                              @Param("value") String value);

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
