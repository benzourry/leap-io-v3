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


//    @Query("select f from Form f left join f.access access where f.app.id = :appId " +
//            " and (access is null or (lower(concat(',',REGEXP_REPLACE(access.users,'[\r\n]',''),',')) like concat('%',lower(concat(',',:email,',')),'%')))")
//    Page<Form> findByAppIdAndEmail(@Param("appId") Long appId, @Param("email") String email, Pageable pageable);

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

//    id, f.title, f.description, f.admin, f.access_list, f.nav, f.icon, f.align, " +
//            " f.code_format, f.f, f.on_save, " +
//            " f.app, f.add_mailer, f.update_mailer, f.retract_mailer
//    @Query(value = "select f.id, f.align, f.code_format, f.description, f.end_date, f.f, f.icon, f.inactive, f.nav," +
//            " f.public_access, f.start_date, f.title, f.access, f.app, f.prev, f.counter, f.can_edit, f.can_retract," +
//            " f.can_save, f.can_submit, f.validate_save, f.admin, f.show_status, f.hide_status, f.single, f.add_mailer," +
//            " f.update_mailer, f.retract_mailer, f.on_save, f.on_submit, f.update_appr_mailer, f.on_view, f.single_q," +
//            " f.show_index, f.x, f.public_ep, f.access_list," +
//            " ug_admin.allow_reg, ug_admin.name, ug_admin.description, " +
//            " ug_admin.need_approval " +
//            " from form f " +
//            " left join section sect on sect.form = f.id " +
//            " left join tier tier on tier.form = f.id " +
//            " left join tab tab on tab.form = f.id " +
//            " left join item item on item.form = f.id " +
//            " left join user_group ug_admin on f.admin = ug_admin.id " +
////            " left join user_group access on f.access = access.id " +
//            " left JOIN app_user au on find_in_set(au.user_group,f.access_list) " +
////            " left join app_user au on f.access = au.user_group " +
//            " left join users u on au.user = u.id " +
//            " where f.id in (:ids) " +
////            " and (g.access is null or (u.email = :email AND au.status = 'approved')) " +
//            " and (f.access_list is null or f.access_list = '' or (u.email = :email AND au.status = 'approved'))", nativeQuery = true)
//            " and (f.access is null or (u.email = :email AND au.status = 'approved'))", nativeQuery = true)


// @Query(value = "select f " +
//            " from Form f " +
//            " left JOIN AppUser au on find_in_set(au.group.id,f.accessList)<>0 " +
////            " left join app_user au on f.access = au.user_group " +
//            " left join au.user u " +
//            " where f.id in (:ids) " +
////            " and (g.access is null or (u.email = :email AND au.status = 'approved')) " +
//            " and (f.accessList is null or f.accessList = '' or (u.email = :email AND au.status = 'approved'))")
////            " and (f.access is null or (u.email = :email AND au.status = 'approved'))", nativeQuery = true)
//    Page<Form> findByIdsAndEmail(@Param("ids") List<Long> ids, @Param("email") String email, Pageable pageable);

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
    @Query( " UPDATE Form r " +
            " SET r.inactive = TRUE " +
            " WHERE (:now NOT BETWEEN r.startDate AND r.endDate)" +
            " and (r.inactive = FALSE )")
    void updateInactive(@Param("now") Date now);


    @Modifying
    @Query("UPDATE Form f SET f.counter = f.counter + 1 WHERE f.id = :formId")
    void incrementCounter(@Param("formId") Long formId);

    @Query("SELECT f.counter FROM Form f WHERE f.id = :formId")
    int findCounter(@Param("formId") Long formId);

    @Modifying
    @Query( " UPDATE Form r " +
            " SET r.prev = NULL " +
            " WHERE r.id=:formId")
    void unlinkPrev(@Param("formId") Long formId);

    @Modifying
    @Query("delete from Form s where s.app.id = :appId")
    void deleteByAppId(@Param("appId") Long dsId);


}
