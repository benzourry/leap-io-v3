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

    List<PushSub> findPushSubsByAppId(Long appId);

    @Modifying
    @Query("delete from PushSub s where s.appId = :appId")
    void deleteByAppId(@Param("appId") Long appId);
}
