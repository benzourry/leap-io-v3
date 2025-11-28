package com.benzourry.leap.repository;

import com.benzourry.leap.model.Bucket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BucketRepository extends JpaRepository<Bucket, Long> {

    @Query("select u from Bucket u where u.appId = :appId")
    Page<Bucket> findByAppId(@Param("appId") Long appId, Pageable pageable);

    @Modifying
    @Query("delete from Bucket s where s.appId = :appId")
    void deleteByAppId(@Param("appId") Long appId);

    @Query("select ea.fileUrl from EntryAttachment ea where ea.bucketId = :bucketId")
    List<String> findPathByBucketId(@Param("bucketId") Long bucketId);

//    @Query("select u from UserGroup u where u.app.id = :appId and u.allowReg = TRUE")
//    List<UserGroup> findRegListByAppId(@Param("appId") Long appId);


    @Query("select s from Bucket s where s.scheduled=TRUE and s.clock = :clock")
    List<Bucket> findScheduledByClock(@Param("clock") String clock);

    @Query("select count(l.id) from Bucket l where l.appId = :appId")
    long countByAppId(@Param("appId") Long appId);

}
