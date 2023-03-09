package com.benzourry.leap.repository;

import com.benzourry.leap.model.App;
import com.benzourry.leap.model.AppUser;
import com.benzourry.leap.model.User;
import com.benzourry.leap.model.UserGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long>, JpaSpecificationExecutor<App> {

//    @Query("select u.status from AppUser u where " +
//            " u.user.appId = :appId" +
//            " and u.user.email = :email")
//    String findStatusByAppIdAndEmail(@Param("appId") Long appId, @Param("email") String requesterEmail);

    @Query("select u from AppUser u " +
            " where u.user.appId = :appId " +
            " and (:status is null or u.status in :status) " +
            " and (:groupId is null or u.group.id = :groupId) " +
            " and u.user.email like :searchText")
    Page<AppUser> findByAppIdAndParam(@Param("appId") Long appId, @Param("searchText") String searchText, @Param("status") List<String> status, @Param("groupId") Long group, Pageable pageable);


    @Query("select new AppUser(au.id,usr, grp, au.status) " +
            " from AppUser au " +
            " left join au.group grp " +
            " right join au.user usr "  +
            " where usr.appId = :appId " +
            " and (:status is null or au.status in :status) " +
            " and usr.email like :searchText")
    Page<AppUser> findAllByAppId(@Param("appId") Long appId, @Param("searchText") String searchText, @Param("status") List<String> status, Pageable pageable);


    @Query("select au from AppUser au where au.user.appId = :appId and au.user.email = :email")
    List<AppUser> findByAppIdAndEmail(@Param("appId") Long appId, @Param("email") String email);

    @Query("select u from AppUser u where u.group.id = :groupId")
    Page<AppUser> findByGroupId(@Param("groupId") Long groupId, Pageable pageable);

    List<AppUser> findByUserIdAndStatus(Long userId,String status);

    List<AppUser> findByUserId(Long userId);


    @Modifying
    @Query(value = "delete from app_user where `user` in (select id from users where app_id = :appId)", nativeQuery = true)
    void deleteByAppId(@Param("appId") Long appId);


    Optional<AppUser> findByUserIdAndGroupId(Long id, Long id1);

    @Modifying
    @Query("delete from AppUser s where s.group.id = :groupId")
    void deleteByUserGroup(@Param("groupId") Long groupId);

    @Query(value = "select u.email from app_user au left join `users` u on au.user = u.id where au.`user_group` = :groupId", nativeQuery = true)
    List<String> findEmailsByGroupId(Long groupId);

}
