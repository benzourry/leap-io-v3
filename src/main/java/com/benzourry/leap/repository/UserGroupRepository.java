package com.benzourry.leap.repository;

import com.benzourry.leap.model.UserGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {

    @Query("select u from UserGroup u where u.app.id = :appId")
    Page<UserGroup> findByAppId(@Param("appId") Long appId, Pageable pageable);

    @Modifying
    @Query("delete from UserGroup s where s.app.id = :appId")
    void deleteByAppId(@Param("appId") Long appId);

    @Query("select u from UserGroup u where u.app.id = :appId")
    List<UserGroup> findAllListByAppId(@Param("appId") Long appId);

    @Query("select u from UserGroup u where u.app.id = :appId and u.allowReg = TRUE")
    List<UserGroup> findRegListByAppId(@Param("appId") Long appId);
}
