package com.benzourry.leap.repository;

import com.benzourry.leap.model.Dashboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DashboardRepository extends JpaRepository<Dashboard, Long> {

//    @Query("select s from Dashboard s where s.form.id = :formId")
//    Page<Dashboard> findByFormId(@Param("formId") long formId, Pageable pageable);

    @Query(value = "select s from Dashboard s where s.app.id = :appId")
    List<Dashboard> findByAppId(@Param("appId") long appId);


//    @Query("select s from Dashboard s left join s.access access where s.app.id = :appId " +
//            " and (access is null or (lower(concat(',',REGEXP_REPLACE(access.users,'[\r\n]',''),',')) like concat('%',lower(concat(',',:email,',')),'%')))")
//    List<Dashboard> findByAppIdAndEmail(@Param("appId") long appId, @Param("email") String email);

    @Query(value = "select * from dashboard s" +
            " left join app_user au on s.access = au.user_group " +
            " left join users u on au.user = u.id " +
//            " left join user_group access on s.access = access.id " +
            " where s.app = :appId " +
            " and (s.access is null or (u.email = :email AND au.status = 'approved'))", nativeQuery = true)
//            " and (access is null or (lower(concat(',',REGEXP_REPLACE(access.users,'[\r\n ]',''),',')) like concat('%',lower(concat(',',:email,',')),'%')))", nativeQuery = true)
    List<Dashboard> findByAppIdAndEmail(@Param("appId") long appId, @Param("email") String email);

    @Query(value = "select s from Dashboard s " +
            " left join s.access access" +
            " left join AppUser au on access.id = au.group.id " +
            " left join au.user u " +
//            " left join user_group access on s.access = access.id " +
            " where s.id in (:ids) " +
            " and (access.id is null or (u.email = :email AND au.status = 'approved'))")
//            " and (access is null or (lower(concat(',',REGEXP_REPLACE(access.users,'[\r\n ]',''),',')) like concat('%',lower(concat(',',:email,',')),'%')))", nativeQuery = true)
    List<Dashboard> findByIdsAndEmail(@Param("ids") List<Long> ids, @Param("email") String email);


    @Query(value = "select count(s.id) from dashboard s where s.app = :appId", nativeQuery = true)
    long countByAppId(@Param("appId") long appId);

    @Modifying
    @Query("delete from Dashboard s where s.app.id = :appId")
    void deleteByAppId(@Param("appId") Long dsId);


}
