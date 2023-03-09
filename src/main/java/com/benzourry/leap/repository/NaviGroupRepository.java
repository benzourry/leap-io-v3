package com.benzourry.leap.repository;

import com.benzourry.leap.model.NaviGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NaviGroupRepository extends JpaRepository<NaviGroup, Long> {

//    @Query("select g from NaviGroup g " +
//            " left join g.access a" +
//            " where g.app.id=:appId AND " +
//            " ( a is null OR (concat(',',REGEXP_REPLACE(a.users,'[\r\n ]',''),',') like concat('%',concat(',',:email,','),'%')) ) " +
//            " order by g.sortOrder ASC")
//    List<NaviGroup> findByAppIdAndEMail(@Param("appId") Long appId, @Param("email") String email);

//    @Query("select g from NaviGroup g " +
////            " left join g.access a " +
////            " left join g.access a " +
//            " left JOIN AppUser au on g.access.id = au.group.id  " +
//            " where g.app.id=:appId AND " +
//            " (a is null or au.user.email = :email) " +
//            " order by g.sortOrder ASC")
//    @Query(value = "select g.id, g.title, g.access_list, g.sort_order, g.pre, g.app " +
//            " from navi_group g " +
//            " left JOIN app_user au on find_in_set(au.user_group,g.access_list) " +
//            " left join users u on au.user = u.id " +
//            " where g.app=:appId AND " +
//            " (g.access_list is null or g.access_list = '' or (u.email = :email AND au.status = 'approved')) " +
//            " order by g.sort_order ASC",nativeQuery = true)
    @Query(value = "select g " +
            " from NaviGroup g " +
            " left JOIN AppUser au on find_in_set(au.group,g.accessList)<>0 " +
            " left join au.user u " +
            " where g.app.id=:appId AND " +
            " (g.accessList is null or g.accessList = '' or (u.email = :email AND au.status = 'approved')) " +
            " order by g.sortOrder ASC")
    List<NaviGroup> findByAppIdAndEMail(@Param("appId") Long appId, @Param("email") String email);

//    @Query("select g from NaviGroup g " +
//            " left join g.access a " +
//            " JOIN AppUser au on g.access.id = au.group.id" +
//            " where g.app.id=:appId AND " +
//            " au.user.id = :userId " +
//            " order by g.sortOrder ASC")
//    List<NaviGroup> findByAppIdAndUserId(@Param("appId") Long appId, @Param("userId") Long userId);

    @Query("select g from NaviGroup g " +
            " where g.app.id=:appId " +
            " order by g.sortOrder ASC")
    List<NaviGroup> findByAppId(@Param("appId") Long appId);


    @Query("select count(s.id) from NaviGroup s where s.app.id = :appId")
    long countByAppId(@Param("appId") long appId);

}
