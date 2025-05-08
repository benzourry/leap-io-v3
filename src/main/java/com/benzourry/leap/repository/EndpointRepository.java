package com.benzourry.leap.repository;

import com.benzourry.leap.model.Endpoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EndpointRepository extends JpaRepository<Endpoint, Long> {

    @Query(value = "select e from Endpoint e where e.app.id = :appId")
    Page<Endpoint> findByAppId(@Param("appId") Long appId, Pageable pageable);

    @Query(value = "select e from Endpoint e where e.shared = TRUE")
    Page<Endpoint> findShared(Pageable pageable);

    Endpoint findFirstByCodeAndApp_Id(String code,Long appId);

    @Modifying
    @Query("delete from Endpoint s where s.app.id = :appId")
    void deleteByAppId(@Param("appId") Long appId);

    @Query("select count(l.id) from Endpoint l where l.app.id = :appId")
    long countByAppId(@Param("appId") Long appId);


//    @Query("select u from Endpoint u where u.app.id = :appId and u.allowReg = TRUE")
//    Page<UserGroup> findRegListByAppId(@Param("appId") Long appId, Pageable pageable);
}
