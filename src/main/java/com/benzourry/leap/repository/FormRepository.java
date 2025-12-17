package com.benzourry.leap.repository;

import com.benzourry.leap.model.Form;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface FormRepository extends JpaRepository<Form, Long> {

    @Query(value = "select f from Form f where f.app.id = :appId")
    Page<Form> findByAppId(@Param("appId") Long appId,
                           Pageable pageable);


    @Query(value = "select * from form f " +
            " left JOIN app_user au on find_in_set(au.user_group,f.access_list) " +
//            " left join app_user au on f.access = au.user_group " +
            " left join users u on au.user = u.id " +
            " where f.app = :appId " +
//            " and (g.access is null or (u.email = :email AND au.status = 'approved')) " +
            " and (f.access_list is null or f.access_list = '' or (u.email = :email AND au.status = 'approved'))"
//            " and (f.access is null or (u.email = :email AND au.status = 'approved'))"
            , nativeQuery = true)
    Page<Form> findByAppIdAndEmail(@Param("appId") Long appId,
                                   @Param("email") String email,
                                   Pageable pageable);

    @Query(value = "select f " +
            " from Form f " +
            " left JOIN AppUser au on function('find_in_set',au.group.id,f.accessList)<>0 " +
//            " left join app_user au on f.access = au.user_group " +
            " left join au.user u " +
            " where f.id in (:ids) " +
//            " and (g.access is null or (u.email = :email AND au.status = 'approved')) " +
            " and (f.accessList is null or f.accessList = '' or (u.email = :email AND au.status = 'approved'))")
    Page<Form> findByIdsAndEmail(@Param("ids") List<Long> ids,
                                 @Param("email") String email,
                                 Pageable pageable);

    @Query("select count(f.id) from Form f where f.app.id = :appId")
    long countByAppId(@Param("appId") Long appId);

    @Query("select d.form from Dataset d where d.id=:id")
    Form getByDatasetId(@Param("id") long id);

    @Modifying
    @Query(value = "UPDATE form SET counter = LAST_INSERT_ID(counter + 1) WHERE id = :formId", nativeQuery = true)
    void incrementCounter(@Param("formId") Long formId);

    @Query(value = "SELECT LAST_INSERT_ID()", nativeQuery = true)
    int getLatestCounter();

    @Modifying
    @Query( " UPDATE Form r " +
            " SET r.prev = NULL " +
            " WHERE r.id=:formId")
    void unlinkPrev(@Param("formId") Long formId);

    @Modifying
    @Query("delete from Form s where s.app.id = :appId")
    void deleteByAppId(@Param("appId") Long dsId);


}
