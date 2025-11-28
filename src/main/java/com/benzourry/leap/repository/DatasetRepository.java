package com.benzourry.leap.repository;

import com.benzourry.leap.model.Dataset;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DatasetRepository extends JpaRepository<Dataset, Long> {

    @Query(value = "select d from Dataset d " +
            " left join d.form form " +
            " where form.id = :formId")
    List<Dataset> findByFormId(@Param("formId") long formId, Pageable pageable);

   @Query(value = "select d.id from Dataset d " +
            " left join d.form form " +
            " where form.id = :formId")
    List<Long> findIdsByFormId(@Param("formId") long formId);

    @Query(value = "select s from Dataset s where s.app.id = :appId")
    List<Dataset> findByAppId(@Param("appId") long appId, Pageable pageable);

    @Query(value = "select d from Dataset d where d.id in (:ids)")
    List<Dataset> findByIds(@Param("ids") List<Long> ids);

//
//    @Query("select s from Dataset s left join s.access access where s.app.id = :appId " +
//            " and (access is null or (lower(concat(',',REGEXP_REPLACE(access.users,'[\r\n]',''),',')) like concat('%',lower(concat(',',:email,',')),'%')))")
//    List<Dataset> findByAppIdAndEmail(@Param("appId") long appId, @Param("email") String email);

//    @Query(value = "select * from dataset s " +
//            " left join app_user au on s.access = au.user_group " +
//            " left join users u on au.user = u.id " +
////            " left join user_group access on s.access = access.id " +
//            " where s.app = :appId " +
//            " and (s.access is null or (u.email = :email AND au.status = 'approved'))", nativeQuery = true)
////            " and (access is null or (lower(concat(',',REGEXP_REPLACE(access.users,'[\r\n ]',''),',')) like concat('%',lower(concat(',',:email,',')),'%')))", nativeQuery = true)
//    List<Dataset> findByAppIdAndEmail(@Param("appId") long appId, @Param("email") String email);

//    @Query(value = "select * from dataset s " +
//            " left join app_user au on s.access = au.user_group " +
//            " left join users u on au.user = u.id " +
////            " left join user_group access on s.access = access.id " +
//            " where s.id in (:ids) " +
//            " and (s.access is null or (u.email = :email AND au.status = 'approved'))", nativeQuery = true)
////            " and (access is null or (lower(concat(',',REGEXP_REPLACE(access.users,'[\r\n ]',''),',')) like concat('%',lower(concat(',',:email,',')),'%')))", nativeQuery = true)
//    @Query(value = "select d from Dataset d " +
//            " left join d.access access " +
//            " left join AppUser au on access.id = au.group.id " +
//            " left join au.user u " +
//            " where d.id in (:ids) " +
//            " and (access.id is null or (u.email = :email AND au.status = 'approved'))")
//    List<Dataset> findByIdsAndEmail(@Param("ids") List<Long> ids, @Param("email") String email);

//    @Query(value = "select d from Dataset d " +
//            " left JOIN AppUser au on find_in_set(au.group.id,d.accessList)<>0 " +
//            " left join au.user u " +
//            " where d.id in (:ids) " +
//            " and (d.accessList is null or d.accessList = '' or (u.email = :email AND au.status = 'approved'))")
//    List<Dataset> findByIdsAndEmail(@Param("ids") List<Long> ids, @Param("email") String email);

    @Query(value = "select d from Dataset d " +
            " left JOIN AppUser au on function('find_in_set',au.group.id,d.accessList)<>0 " +
            " left join au.user u " +
            " where d.id in (:ids) " +
            " and (d.accessList is null or d.accessList = '' or (u.email = :email AND au.status = 'approved'))")
    List<Dataset> findByIdsAndEmail(@Param("ids") List<Long> ids, @Param("email") String email);


    @Query("select count(s.id) from Dataset s where s.app.id = :appId")
    long countByAppId(@Param("appId") long appId);

    @Modifying
    @Query("delete from Dataset s where s.app.id = :appId")
    void deleteByAppId(@Param("appId") Long dsId);


}
