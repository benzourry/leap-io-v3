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

    @Query("select u from UserGroup u" +
            " where u.app.id = :appId OR" +
            " (:userFromApp is not null AND u.app.id = :userFromApp)")

//    @Query(value = "select u.* from user_group u " +
//            " left join app on u.app = app.id " +
//            " where u.app = :appId OR json_value(app.x,'$.userFromApp') = u.app", nativeQuery = true)
    Page<UserGroup> findByAppIdAll(@Param("appId") Long appId,@Param("userFromApp") Long userFromApp, Pageable pageable);


//    @Query(value = """
//            select s from Screen s
//             left JOIN AppUser au on function('find_in_set',au.group.id,s.accessList)<>0
//             left join au.user u
//             where s.id in (:ids)
//             and (s.accessList is null or s.accessList = '' or (u.email = :email AND au.status = 'approved'))
//            """)
////            " and (access is null or (lower(concat(',',REGEXP_REPLACE(access.users,'[\r\n ]',''),',')) like concat('%',lower(concat(',',:email,',')),'%')))", nativeQuery = true)
//    List<UserGroup> findByIdsAndEmail(@Param("ids") List<Long> ids, @Param("email") String email);


    @Query(value = "select * from screen s " +

            " left JOIN app_user au on find_in_set(au.user_group,s.access_list) " +
//            " left join app_user au on f.access = au.user_group " +
            " left join users u on au.user = u.id " +
            " where s.app = :appId " +
//            " and (g.access is null or (u.email = :email AND au.status = 'approved')) " +
            " and (s.access_list is null or s.access_list = '' or (u.email = :email AND au.status = 'approved'))"
//            " and (f.access is null or (u.email = :email AND au.status = 'approved'))"
            , nativeQuery = true)
    Page<UserGroup> findByAppIdAndEmail(@Param("appId") Long appId,
                                   @Param("email") String email,
                                   Pageable pageable);



    @Modifying
    @Query("delete from UserGroup s where s.app.id = :appId")
    void deleteByAppId(@Param("appId") Long appId);

    @Query("select u from UserGroup u where u.app.id = :appId")
    List<UserGroup> findAllListByAppId(@Param("appId") Long appId);

    @Query("select u from UserGroup u where u.app.id = :appId and u.allowReg = TRUE")
    List<UserGroup> findRegListByAppId(@Param("appId") Long appId);
}
