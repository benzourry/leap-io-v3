package com.benzourry.leap.repository;

import com.benzourry.leap.model.EntryAttachment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.hibernate.jpa.QueryHints.*;

@Repository
public interface EntryAttachmentRepository extends JpaRepository<EntryAttachment, Long> {

    @Query(value = "select * from entry_attachment i" +
//            " left join i.section s " +
//            " left join i.form f " +
            " where i.item_id = :itemId", nativeQuery = true)
    Page<EntryAttachment> findByItemId(@Param("itemId") long itemId, Pageable pageable);

    EntryAttachment findByFileUrl(String fileUrl);

    @Query(value = "select * from entry_attachment " +
            " where bucket_id = :bucketId" +
            " and (lower(file_url) like :searchText or lower(file_type) like :searchText " +
            " or lower(file_name) like :searchText" +
            " or lower(email) like :searchText " +
            " or lower(item_label) like :searchText)", nativeQuery = true)
    Page<EntryAttachment> findByBucketId(@Param("bucketId") long bucketId, @Param("searchText") String searchText, Pageable pageable);

    @Query(value = "select * from entry_attachment " +
            " where bucket_id = :bucketId" +
            " and (lower(file_url) like :searchText or lower(file_type) like :searchText " +
            " or lower(file_name) like :searchText" +
            " or lower(email) like :searchText " +
            " or lower(item_label) like :searchText)", nativeQuery = true)
    @QueryHints(value = {
//            @QueryHint(name = HINT_FETCH_SIZE, value = "" + Integer.MIN_VALUE),
            @QueryHint(name = HINT_CACHEABLE, value = "false"),
            @QueryHint(name = HINT_READONLY, value = "true"),
//            @QueryHint(name = HINT_PASS_DISTINCT_THROUGH, value = "false")
    })
    Stream<EntryAttachment> findByBucketId(@Param("bucketId") long bucketId, @Param("searchText") String searchText);

    @Modifying
//    @Query("delete from EntryAttachment s where s.appId = :appId")
    @Query(value = "delete from entry_attachment where app_id = :appId", nativeQuery = true)
    void deleteByAppId(@Param("appId") Long appId);

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

    @Query(value = "select * from entry_attachment " +
            " where item_id = :itemId", nativeQuery = true)
    Stream<EntryAttachment> findByItemId(@Param("itemId") Long itemId);
}
