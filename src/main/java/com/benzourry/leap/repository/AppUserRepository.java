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
import java.util.Map;
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
    Page<AppUser> findByAppIdAndParam(@Param("appId") Long appId,
                                      @Param("searchText") String searchText,
                                      @Param("status") List<String> status,
                                      @Param("groupId") Long group,
                                      Pageable pageable);


//    @Query("select new AppUser(au.id,usr, grp, au.status) " +
//            " from AppUser au " +
//            " right join au.user usr "  +
//            " left join au.group grp " +
//            " where usr.appId = :appId " +
//            " and (:status is null or au.status in :status) " +
//            " and usr.email like :searchText")
//    @Query("select new AppUser(au.id,usr, grp, au.status) " +
//            " from AppUser au " +
//            " left join au.group grp " +
//            " right join au.user usr "  +
//            " where usr.appId = :appId " +
//            " and (:status is null or au.status in :status) " +
//            " and usr.email like :searchText")
    @Query("select new AppUser(au.id,usr, grp, au.status)" +
            " from User usr " +
            " left join AppUser au on usr.id = au.userId " +
            " left join au.group grp " +
            " where usr.appId = :appId " +
//            " and (:#{null eq #status} is true or au.status in :status) " +
//            " and (:status is null or au.status in :status) " +
            " and (:emptyStatus=true or au.status in :status) " +
            " and (upper(usr.email) like :searchText OR upper(usr.name) like :searchText OR upper(usr.providerId) like :searchText )")
    Page<AppUser> findAllByAppId(@Param("appId") Long appId,
                                 @Param("searchText") String searchText,
                                 @Param("status") List<String> status,
                                 @Param("emptyStatus") boolean emptyStatus,
                                 Pageable pageable);


    @Query("select au from AppUser au where au.user.appId = :appId and au.user.email = :email")
    List<AppUser> findByAppIdAndEmail(@Param("appId") Long appId,
                                      @Param("email") String email);

    @Query("select u from AppUser u where u.group.id = :groupId")
    Page<AppUser> findByGroupId(@Param("groupId") Long groupId,
                                Pageable pageable);

    //    @Query("select new AppUser(au.id,usr, grp, au.status)" +
//            " from User usr " +
//            " left join AppUser au on usr.id = au.userId " +
//            " left join au.group grp " +
//            " where (:emptyStatus=true or au.status in :status) " +
//            " and grp.id = :groupId " +
//            " and usr.email like :searchText")
    @Query("select au from AppUser au " +
            " left join au.user usr " +
            " left join au.group grp " +
            " where (:emptyStatus=true or au.status in :status) " +
            " and grp.id = :groupId " +
            " and (upper(usr.email) like :searchText OR upper(usr.name) like :searchText OR upper(usr.providerId) like :searchText )")
    Page<AppUser> findByGroupIdAndParams(@Param("groupId") Long groupId,
                                         @Param("searchText") String searchText,
                                         @Param("status") List<String> status,
                                         @Param("emptyStatus") boolean emptyStatus,
                                Pageable pageable);

    List<AppUser> findByUserIdAndStatus(Long userId,String status);

    @Query("select a from AppUser a where a.user.email = :email and a.user.appId = :appId and a.status = :status")
    List<AppUser> findByAppIdAndEmailAndStatus(@Param("appId") Long appId, @Param("email") String email,@Param("status") String status);

    @Query("select a.group.id from AppUser a where a.user.email = :email and a.user.appId = :appId and a.status = :status")
    List<Long> findIdsByAppIdAndEmailAndStatus(@Param("appId") Long appId, @Param("email") String email,@Param("status") String status);

    @Query("select a.group.id from AppUser a where a.user.id = :userId and a.status = :status")
    List<Long> findIdsByUserIdAndStatus(@Param("userId") Long userId,@Param("status") String status);

    List<AppUser> findByUserId(Long userId);


//    @Modifying
//    @Query(value = "delete from app_user where `user` in (select id from users where app_id = :appId)", nativeQuery = true)
//    void deleteByAppId(@Param("appId") Long appId);

    @Modifying
    @Query(value = "delete from app_user where `user` in (select id from users where app_id = :appId)", nativeQuery = true)
//    @Query(value = "delete from AppUser au where au.user.appId = :appId")
    void deleteByAppId(@Param("appId") Long appId);


    Optional<AppUser> findByUserIdAndGroupId(Long id, Long id1);

    @Modifying
    @Query("delete from AppUser s where s.group.id = :groupId")
    void deleteByUserGroup(@Param("groupId") Long groupId);

//    @Query(value = "select u.email from app_user au left join `users` u on au.user = u.id where au.`user_group` = :groupId", nativeQuery = true)
//    List<String> findEmailsByGroupId(Long groupId);

    @Query(value = "select u.email from AppUser au " +
            " left join au.user u where au.group.id = :groupId")
    List<String> findEmailsByGroupId(@Param("groupId") Long groupId);

//    @Query(value = "select u from AppUser au " +
//            " left join au.user u where au.group.id = :groupId")
//    List<AppUser> findByGroupIdAndCriteria(Long groupId, Map<String, Object> criteria);

}
