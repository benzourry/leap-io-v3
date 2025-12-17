package com.benzourry.leap.repository;

import com.benzourry.leap.model.NaviGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NaviGroupRepository extends JpaRepository<NaviGroup, Long> {

    @Query(value = "select g " +
            " from NaviGroup g " +
            " left join AppUser au on function('find_in_set',au.group.id,g.accessList)<>0 " +
            " left join au.user u " +
            " where g.app.id=:appId and " +
            " (g.accessList is null or g.accessList = '' or (u.email = :email AND au.status = 'approved')) " +
            " order by g.sortOrder ASC")
    List<NaviGroup> findByAppIdAndEMail(@Param("appId") Long appId, @Param("email") String email);

    @Query("select g from NaviGroup g " +
            " where g.app.id=:appId " +
            " order by g.sortOrder ASC")
    List<NaviGroup> findByAppId(@Param("appId") Long appId);

    @Query("select count(s.id) from NaviGroup s where s.app.id = :appId")
    long countByAppId(@Param("appId") long appId);

}
