package com.benzourry.leap.repository;

import com.benzourry.leap.model.MetaTrail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Repository
public interface MetaTrailRepository extends JpaRepository<MetaTrail, String> {

    @Query("SELECT m FROM MetaTrail m WHERE m.scope = :scope " +
            "AND (:searchText IS NULL OR LOWER(m.action) LIKE LOWER(CONCAT('%', :searchText, '%')) OR LOWER(m.email) LIKE LOWER(CONCAT('%', :searchText, '%'))) " +
            "AND (:dateFrom IS NULL OR m.timestamp >= :dateFrom) " +
            "AND (:dateTo IS NULL OR m.timestamp <= :dateTo)")
    Page<MetaTrail> findByScopeWithFilters(@Param("scope") String scope,
                                           @Param("searchText") String searchText,
                                           @Param("dateFrom") Date dateFrom,
                                           @Param("dateTo") Date dateTo,
                                           Pageable pageable);

    @Query("SELECT m FROM MetaTrail m WHERE m.appId = :appId " +
            "AND (:module IS NULL OR m.module = :module) " +
            "AND (:moduleId IS NULL OR m.moduleId = :moduleId) " +
            "AND (:dateFrom IS NULL OR m.timestamp >= :dateFrom) " +
            "AND (:dateTo IS NULL OR m.timestamp <= :dateTo)")
    Page<MetaTrail> findByAppIdWithFilters(@Param("appId") Long appId,
                                           @Param("module") String module,
                                           @Param("moduleId") Long moduleId,
                                           @Param("dateFrom") Date dateFrom,
                                           @Param("dateTo") Date dateTo,
                                           Pageable pageable);
}