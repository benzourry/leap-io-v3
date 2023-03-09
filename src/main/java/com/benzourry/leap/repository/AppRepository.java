package com.benzourry.leap.repository;

import com.benzourry.leap.model.App;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    @Query(value = "select count(*) from app where upper(app_path) LIKE upper(:appPath)", nativeQuery = true)
    long checkByPath(@Param("appPath") String appPath);

    @Query(value = "select count(*) from app where upper(app_domain) LIKE upper(:appDomain)",nativeQuery = true)
    long checkByDomain(@Param("appDomain") String appDomain);

    @Query(value = "select * from app where app_path=:appPath", nativeQuery = true)
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

    @Query(value = "select * from app where app_domain=:appDomain", nativeQuery = true)
    App findByAppDomain(@Param("appDomain") String appDomain);
}
