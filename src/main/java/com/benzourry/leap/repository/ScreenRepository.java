package com.benzourry.leap.repository;

import com.benzourry.leap.model.Dataset;
import com.benzourry.leap.model.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, Long> {

//    @Query("select s from Screen s where s.form.id = :formId")
//    Page<Screen> findByFormId(@Param("formId") long formId, Pageable pageable);


    @Query("select s from Screen s where s.dataset.id = :dsId")
    List<Screen> findByDatasetId(@Param("dsId") long dsId);

    @Query("select s from Screen s where s.app.id = :appId")
    List<Screen> findByAppId(@Param("appId") long appId);

    @Query(value = "select * from screen s where s.id in (:ids)", nativeQuery = true)
    List<Screen> findByIds(@Param("ids") List<Long> ids);


//    @Query("select s from Screen s left join s.access access where s.app.id = :appId " +
//            " and (access is null or (lower(concat(',',REGEXP_REPLACE(access.users,'[\r\n]',''),',')) like concat('%',lower(concat(',',:email,',')),'%')))")
//    List<Screen> findByAppIdAndEmail(@Param("appId") long appId,@Param("email") String email);

    @Query(value = "select * from screen s " +
            " left join app_user au on s.access = au.user_group " +
            " left join users u on au.user = u.id " +
//            " left join user_group access on s.access = access.id " +
            " where s.app = :appId " +
            " and (s.access is null or (u.email = :email AND au.status = 'approved'))", nativeQuery = true)
//            " and (access is null or (lower(concat(',',REGEXP_REPLACE(access.users,'[\r\n ]',''),',')) like concat('%',lower(concat(',',:email,',')),'%')))", nativeQuery = true)
    List<Screen> findByAppIdAndEmail(@Param("appId") long appId, @Param("email") String email);

    @Query(value = """
            select s from Screen s 
             left join s.access access 
             left join AppUser au on access.id = au.group.id 
             left join au.user u 
             where s.id in (:ids) 
             and (access.id is null or (u.email = :email AND au.status = 'approved'))
            """)
//            " and (access is null or (lower(concat(',',REGEXP_REPLACE(access.users,'[\r\n ]',''),',')) like concat('%',lower(concat(',',:email,',')),'%')))", nativeQuery = true)
    List<Screen> findByIdsAndEmail(@Param("ids") List<Long> ids, @Param("email") String email);


    @Query("select count(s.id) from Screen s where s.app.id = :appId")
    long countByAppId(@Param("appId") long appId);

    @Modifying
    @Query("delete from Screen s where s.app.id = :appId")
    void deleteByAppId(@Param("appId") Long dsId);


}
