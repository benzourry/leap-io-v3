package com.benzourry.leap.repository;

import com.benzourry.leap.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

//    Optional<User> findByEmail(String email);

    Optional<User> findFirstByEmailAndAppId(String email, Long appId);

//    Optional<User> findByEmailAndProvider(String email, AuthProvider provider);

//    List<User> findAllByAppId(Long appId);

//    List<User> findAllByAppIdAndPushSubNotNull(Long appId);

    Boolean existsByEmailAndAppId(String email, Long appId);

    long countByAppId(Long appId);



    @Query(value = "select * from (select date_format(e.first_login,'%Y-%m') as name, count(e.id) as `value` from users e " +
            " where :appId is null OR e.app_id = :appId" +
            " group by date_format(e.first_login,'%Y-%m') " +
            " order by date_format(e.first_login,'%Y-%m') desc " +
            " limit 10) as sub order by name asc ", nativeQuery = true)
    List<Map> statCountByYearMonth(@Param("appId") Long appId);

    @Query(value = "select * from (select sub.name, sum(sub.value) over (order by sub.name) as `value` from " +
            " ( " +
            "select date_format(e.first_login,'%Y-%m') as name, count(e.id) as `value` from users e " +
            " where :appId is null OR e.app_id = :appId" +
            " group by date_format(e.first_login,'%Y-%m') " +
            " order by date_format(e.first_login,'%Y-%m') asc " +
            " ) as sub order by sub.name desc limit 10) as sub2 order by sub2.name asc", nativeQuery = true)
    List<Map> statCountByYearMonthCumulative(@Param("appId") Long appId);


//    @Query(value = "select * from (select date_format(e.first_login,'%Y-%m') as name, count(e.id) as `value` from users e " +
//            " group by date_format(e.first_login,'%Y-%m') " +
//            " order by date_format(e.first_login,'%Y-%m') desc" +
//            " limit 10) as sub order by name asc", nativeQuery = true)
//    List<Map> statCountByYearMonth();

//    @Query(value = "select * from (select sub.name, sum(sub.value) over (order by sub.name) as `value` from " +
//            " (select date_format(e.first_login,'%Y-%m') as name, count(e.id) as `value` from users e " +
//            " group by date_format(e.first_login,'%Y-%m') " +
//            " order by date_format(e.first_login,'%Y-%m') asc" +
//            " ) as sub order by sub.name desc limit 10) as sub2 order by sub2.name asc", nativeQuery = true)
//    List<Map> statCountByYearMonthCumulative();

    @Query(value = "select e.provider as name, count(e.id) as `value` from users e " +
            " where e.app_id = :appId" +
            " group by e.provider " +
            " order by e.provider ", nativeQuery = true)
    List<Map> statCountByType(@Param("appId") Long appId);


    @Modifying
    @Query("delete from User s where s.appId = :appId")
    void deleteByAppId(@Param("appId") Long appId);

    @Query(value = "select a.title as name, " +
            " count(u.id) as `value` " +
            " from users u " +
            " left join app a on u.app_id = a.id  " +
            " where a.title is not null " +
            " group by a.title, a.app_path " +
            " order by count(u.id) desc  " +
            " limit 10", nativeQuery = true)
    List<Map> statCountByApp();


//    @Modifying
//    @Query("delete from User s where s.provider = :provider and s.providerId = :providerId")
//    void deleteByProviderId(@Param("provider") String provider, @Param("providerId") String providerId);

}
