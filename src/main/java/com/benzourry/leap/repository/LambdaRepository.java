package com.benzourry.leap.repository;

import com.benzourry.leap.model.Lambda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

//import java.awt.print.Pageable;


@Repository
public interface LambdaRepository extends JpaRepository<Lambda, Long> {

//    @Query("select s from Screen s where s.form.id = :formId")
//    Page<Screen> findByFormId(@Param("formId") long formId, Pageable pageable);


//    @Query("select s from Screen s where s.dataset.id = :dsId")
//    List<Screen> findByDatasetId(@Param("dsId") long dsId);

    @Query("select s from Lambda s where s.app.id = :appId")
    Page<Lambda> findByAppId(@Param("appId") long appId, Pageable pageable);

    @Query("select s from Lambda s where s.scheduled=TRUE and s.clock = :clock and s.app.live=TRUE")
    List<Lambda> findScheduledByClock(@Param("clock") String clock);


//    @Query("select s from Screen s left join s.access access where s.app.id = :appId " +
//            " and (access is null or (lower(concat(',',REGEXP_REPLACE(access.users,'[\r\n]',''),',')) like concat('%',lower(concat(',',:email,',')),'%')))")
//    List<Screen> findByAppIdAndEmail(@Param("appId") long appId,@Param("email") String email);

    @Query(value = "select * from lambda s " +
            " left join app_user au on s.access = au.user_group " +
            " left join users u on au.user = u.id " +
//            " left join user_group access on s.access = access.id " +
            " where s.app = :appId " +
            " and (s.access is null or (u.email = :email AND au.status = 'approved'))", nativeQuery = true)
//            " and (access is null or (lower(concat(',',REGEXP_REPLACE(access.users,'[\r\n ]',''),',')) like concat('%',lower(concat(',',:email,',')),'%')))", nativeQuery = true)
    Page<Lambda> findByAppIdAndEmail(@Param("appId") long appId,
                                     @Param("email") String email, Pageable pageable);


    @Query("select count(s.id) from Lambda s where s.app.id = :appId")
    long countByAppId(@Param("appId") long appId);

    @Modifying
    @Query("delete from Lambda s where s.app.id = :appId")
    void deleteByAppId(@Param("appId") Long dsId);


    Optional<Lambda> findFirstByCode(String code);

//    Optional<Lambda> findFirstByCodeOpt(String code);

    @Query(value = "select count(*) from lambda a where upper(a.code) LIKE upper(:code)", nativeQuery = true)
    long checkByCode(@Param("code") String code);

}
