package com.benzourry.leap.repository;

import com.benzourry.leap.model.PushSub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PushSubRepository extends JpaRepository<PushSub, String> {

    List<PushSub> findPushSubsByUser_Id(Long userId);
//    List<PushSub> findPushSubsByUser_IdAndActiveIsTrue(Long userId);

//    boolean existsByUser_IdAndUserAgent(Long userId, String userAgent);

//    @Query("select count(e)>0 from PushSub e where e.endpoint = :endpoint OR (e.user.id = :userId and e.userAgent = :userAgent)")
//    boolean existsCheck(@Param("endpoint") String endpoint,
//                                         @Param("userId") Long userId,
//                                         @Param("userAgent") String userAgent);
//
//    @Query("select e from PushSub e where e.endpoint = :endpoint OR (e.user.id = :userId and e.userAgent = :userAgent)")
//    PushSub findCheck(@Param("endpoint") String endpoint,
//                                         @Param("userId") Long userId,
//                                         @Param("userAgent") String userAgent);

    List<PushSub> findPushSubsByAppId(Long appId);

    @Modifying
    @Query("delete from PushSub s where s.appId = :appId")
    void deleteByAppId(@Param("appId") Long appId);
}
