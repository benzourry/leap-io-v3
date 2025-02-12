package com.benzourry.leap.repository;

import com.benzourry.leap.model.App;
import com.benzourry.leap.model.CloneRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CloneRequestRepository extends JpaRepository<CloneRequest, Long>, JpaSpecificationExecutor<App> {

    @Query("select u.status from CloneRequest u where " +
            " u.app.id = :appId" +
            " and u.email = :email" +
            " and u.type = :type")
    String findStatusByAppIdAndEmail(@Param("appId") Long appId,
                                     @Param("email") String requesterEmail,
                                     @Param("type") String type);

    @Query("select u from CloneRequest u where u.app.id = :appId and u.status in :status")
    Page<CloneRequest> findByAppId(@Param("appId") Long appId,
                                   @Param("status") List<String> status,
                                   Pageable pageable);
}
