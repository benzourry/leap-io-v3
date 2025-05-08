package com.benzourry.leap.repository;

import com.benzourry.leap.model.App;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface AppRepository extends JpaRepository<App, Long>, JpaSpecificationExecutor<App> {

//    @Query("select a from App a where " +
//            "(concat(',',a.email,',') like concat('%',concat(',',:email,','),'%')) " +
//            " and (upper(a.title) like concat('%',upper(:searchText),'%') " +
//            " or upper(a.description) like concat('%',upper(:searchText),'%'))")
//    Page<App> findByEmail(@Param("email") String email, @Param("searchText") String searchText, Pageable pageable);
//
//    @Query("select a from App a where a.status = 'published' " +
//            " and (upper(a.title) like concat('%',upper(:searchText),'%') " +
//            " or upper(a.description) like concat('%',upper(:searchText),'%'))")
//    Page<App> findPublished(@Param("searchText") String searchText, Pageable pageable);
//
//    @Query("select a from App a where a.status = :status " +
//            " and (upper(a.title) like concat('%',upper(:searchText),'%') " +
//            " or upper(a.description) like concat('%',upper(:searchText),'%'))")
//    Page<App> findByStatus(@Param("status") String status, @Param("searchText") String searchText, Pageable pageable);

    @Query(value = "select count(a.id) from App a where upper(a.appPath) LIKE upper(:appPath)")
    long checkByPath(@Param("appPath") String appPath);

    @Query(value = "select count(a.id) from App a where upper(a.appDomain) LIKE upper(:appDomain)")
    long checkByDomain(@Param("appDomain") String appDomain);

    @Query(value = "select a from App a where a.appPath=:appPath")
    App findByAppPath(@Param("appPath") String appPath);

//    @Query("select a from App a " +
//            " left join a.navis g " +
//            " where a.appPath=:appPath and lower(g.access.users) like :email")
//    App findByAppPathAndEmail(@Param("appPath") String appPath,
//                              @Param("email") String email);
//
//    @Query("select a from App a " +
//            " left join a.navis g on lower(g.access.users) like :email " +
//            " where a.id=:appId ")
//    App findByIdAndEmail(@Param("appId") Long appId,
//                         @Param("email") String email);

    @Query(value = "select a from App a where a.appDomain=:appDomain")
    App findByAppDomain(@Param("appDomain") String appDomain);

    @Query(value = "select count(id) as `value`, case when live then 'Live' else 'Dev' end  as `name` from app where status in ('local','published') group by live", nativeQuery = true)
    List<Map> statCountByLive();
}
