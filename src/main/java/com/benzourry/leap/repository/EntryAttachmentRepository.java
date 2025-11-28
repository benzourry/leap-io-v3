package com.benzourry.leap.repository;

import com.benzourry.leap.model.EntryAttachment;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.hibernate.jpa.QueryHints.HINT_CACHEABLE;
import static org.hibernate.jpa.QueryHints.HINT_READONLY;

@Repository
public interface EntryAttachmentRepository extends JpaRepository<EntryAttachment, Long> {

    @Query(value = "select * from entry_attachment i" +
//            " left join i.section s " +
//            " left join i.form f " +
            " where i.item_id = :itemId", nativeQuery = true)
    Page<EntryAttachment> findByItemId(@Param("itemId") long itemId,
                                       Pageable pageable);

    EntryAttachment findFirstByFileUrl(String fileUrl);

    @Query(value = "select * from entry_attachment " +
            " where bucket_id = :bucketId" +
            " and (lower(file_url) like :searchText " +
            " or lower(file_type) like :searchText " +
            " or lower(file_name) like :searchText" +
            " or lower(email) like :searchText " +
            " or entry_id like :searchText " +
            " or lower(item_label) like :searchText)", nativeQuery = true)
    Page<EntryAttachment> findByBucketId(@Param("bucketId") long bucketId,
                                         @Param("searchText") String searchText,
                                         Pageable pageable);

    @Query(value = "select * from entry_attachment " +
            " where bucket_id = :bucketId " +
            " and ((lower(file_url) like :searchText " +
            " or lower(file_type) like :searchText " +
            " or lower(file_name) like :searchText" +
            " or lower(email) like :searchText " +
            " or entry_id like :searchText " +
            " or lower(item_label) like :searchText))" +
            " and (:email is null or email=:email) " +
            " and (:fileType is null or file_type=:fileType) " +
            " and (:entryId is null or entry_id=:entryId) " +
            " and (:sStatus is null or s_status=:sStatus) " +
            " and (:itemId is null or item_id=:itemId) " +
            "", nativeQuery = true)
    Page<EntryAttachment> findByBucketIdAndParams(@Param("bucketId") long bucketId,
                                         @Param("searchText") String searchText,
                                         @Param("email") String email,
                                         @Param("fileType") String fileType,
                                         @Param("entryId") Long entryId,
                                         @Param("sStatus") String sStatus,
                                         @Param("itemId") Long itemId,
                                         Pageable pageable);

    @Query(value = "select * from entry_attachment " +
            " where bucket_id = :bucketId" +
            " and (lower(file_url) like :searchText " +
            " or lower(file_type) like :searchText " +
            " or lower(file_name) like :searchText" +
            " or lower(email) like :searchText " +
            " or entry_id like :searchText " +
            " or lower(item_label) like :searchText)", nativeQuery = true)
    @QueryHints(value = {
//            @QueryHint(name = HINT_FETCH_SIZE, value = "" + Integer.MIN_VALUE),
            @QueryHint(name = HINT_CACHEABLE, value = "false"),
            @QueryHint(name = HINT_READONLY, value = "true"),
//            @QueryHint(name = HINT_PASS_DISTINCT_THROUGH, value = "false")
    })
    Stream<EntryAttachment> findByBucketId(@Param("bucketId") long bucketId,
                                           @Param("searchText") String searchText);

    @Modifying
//    @Query("delete from EntryAttachment s where s.appId = :appId")
    @Query(value = "delete from entry_attachment where app_id = :appId", nativeQuery = true)
    void deleteByAppId(@Param("appId") Long appId);

    @Query(value = "select * from entry_attachment where app_id = :appId", nativeQuery = true)
    List<EntryAttachment> findByAppId(@Param("appId") Long appId);

    @Modifying
//    @Query("delete from EntryAttachment s where s.appId = :appId")
    @Query(value = "delete from entry_attachment where item_id in (select id from item where form = :formId)", nativeQuery = true)
    void deleteByFormId(@Param("formId") Long formId);

//    @Query("delete from EntryAttachment s where s.appId = :appId")
    @Query(value = "select * from entry_attachment where item_id in (select id from item where form = :formId)", nativeQuery = true)
    List<EntryAttachment> findByFormId(@Param("formId") Long formId);

    List<EntryAttachment> findByEntryId(Long entryId);

    Page<EntryAttachment> findByEntryId(Long entryId, Pageable pageable);

    @Query(value = "select file_type as name, count(*) as `value` from entry_attachment where bucket_id = :bucketId" +
            " group by file_type", nativeQuery = true)
    List<Map> statCountByFileType(@Param("bucketId") Long bucketId);

    @Query(value = "select file_type as name, sum(file_size) as `value` from entry_attachment where bucket_id = :bucketId" +
            " group by file_type", nativeQuery = true)
    List<Map> statSizeByFileType(@Param("bucketId") Long bucketId);

    @Query(value = "select item_label as name, count(*) as `value` from entry_attachment where bucket_id = :bucketId" +
            " group by item_label", nativeQuery = true)
    List<Map> statCountByItemLabel(@Param("bucketId") Long bucketId);

    @Query(value = "select item_label as name, sum(file_size) as `value` from entry_attachment where bucket_id = :bucketId" +
            " group by item_label", nativeQuery = true)
    List<Map> statSizeByItemLabel(@Param("bucketId") Long bucketId);

    @Query(value = "select count(*) as total from entry_attachment where bucket_id = :bucketId", nativeQuery = true)
    Long statTotalCount(@Param("bucketId") Long bucketId);

    @Query(value = "select sum(file_size) as total from entry_attachment where bucket_id = :bucketId", nativeQuery = true)
    Long statTotalSize(@Param("bucketId") Long bucketId);

    @Query(value = "select file_type as name, count(*) as `value` from entry_attachment where app_id = :appId" +
            " group by file_type", nativeQuery = true)
    List<Map> statCountByFileTypeByAppId(@Param("appId") Long appId);

    @Query(value = "select file_type as name, sum(file_size) as `value` from entry_attachment where app_id = :appId" +
            " group by file_type", nativeQuery = true)
    List<Map> statSizeByFileTypeByAppId(@Param("appId") Long appId);

    @Query(value = "select item_label as name, count(*) as `value` from entry_attachment where app_id = :appId" +
            " group by item_label", nativeQuery = true)
    List<Map> statCountByItemLabelByAppId(@Param("appId") Long appId);

    @Query(value = "select item_label as name, sum(file_size) as `value` from entry_attachment where app_id = :appId" +
            " group by item_label", nativeQuery = true)
    List<Map> statSizeByItemLabelByAppId(@Param("appId") Long appId);

    @Query(value = "select count(*) as total from entry_attachment where app_id = :appId", nativeQuery = true)
    Long statTotalCountByAppId(@Param("appId") Long appId);



    @Query(value = "select date_format(e.timestamp,'%Y-%m') as name, count(e.id) as `value` from entry_attachment e " +
            " where :appId is null OR e.app_id = :appId" +
            " group by date_format(e.timestamp,'%Y-%m') " +
            " order by date_format(e.timestamp,'%Y-%m') ", nativeQuery = true)
    List<Map> statCountByYearMonth(@Param("appId") Long appId);

    @Query(value = "select * from (" +
            " select sub.name, sum(sub.value) over (order by sub.name) as `value` from " +
            " (" +
            "select date_format(e.timestamp,'%Y-%m') as name, count(e.id) as `value` from entry_attachment e " +
            " where :appId is null OR e.app_id = :appId" +
            " group by date_format(e.timestamp,'%Y-%m') " +
            " order by date_format(e.timestamp,'%Y-%m') " +
            " ) as sub order by sub.name desc limit 10) as sub2 order by sub2.name asc", nativeQuery = true)
    List<Map> statCountByYearMonthCumulative(@Param("appId") Long appId);

    @Query(value = "select sum(file_size) as total from entry_attachment where app_id = :appId", nativeQuery = true)
    Long statTotalSizeByAppId(@Param("appId") Long appId);

    @Query(value = "select * from entry_attachment " +
            " where item_id = :itemId", nativeQuery = true)
    Stream<EntryAttachment> findByItemId(@Param("itemId") Long itemId);


    @Modifying
    @Query(value = "update entry_attachment set success=:success, message=:message, s_status=:s_status, s_message=:s_message where id=:entryId", nativeQuery = true)
    int updateSMessage(
            @Param("success") boolean success,
                      @Param("message") String message,
                      @Param("s_status") String sStatus,
                      @Param("s_message") String sMessage,
                      @Param("entryId") long entryId);

    @Query(value = "select a.title as name, sum(e.file_size) as `value` from entry_attachment e " +
            " left join app a on e.app_id = a.id " +
            " group by a.title" +
            " order by sum(e.file_size) desc " +
            " limit 10", nativeQuery = true)
    List<Map> statSizeByApp();

    @Query(value = "select * from (select date_format(e.timestamp,'%Y-%m') as name, " +
            " sum(e.file_size) as `value` from entry_attachment e " +
            " where date_format(e.timestamp,'%Y-%m') is not null AND " +
            " (:appId is null OR e.app_id = :appId) " +
            " group by date_format(e.timestamp,'%Y-%m') " +
            " order by date_format(e.timestamp,'%Y-%m') desc " +
            " limit 10 ) as sub order by name asc", nativeQuery = true)
    List<Map> statSizeByYearMonth(@Param("appId") Long appId);

    @Query(value = "select * from (" +
            " select sub.name, sum(sub.value) over (order by sub.name) as `value` from " +
            " (select date_format(e.timestamp,'%Y-%m') as name, " +
            " sum(e.file_size) as `value` from entry_attachment e " +
            " where date_format(e.timestamp,'%Y-%m') is not null AND" +
            " (:appId is null OR e.app_id = :appId) " +
            " group by date_format(e.timestamp,'%Y-%m') " +
            " order by date_format(e.timestamp,'%Y-%m') asc " +
            " ) as sub order by sub.name desc limit 10) as sub2 order by sub2.name asc", nativeQuery = true)
    List<Map> statSizeByYearMonthCumulative(@Param("appId") Long appId);

}
