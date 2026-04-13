package com.benzourry.leap.repository;

import com.benzourry.leap.model.AppLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AppLogRepository extends JpaRepository<AppLog, String> {

    @Query(value = "select al from AppLog al " +
            "  where " +
            " al.appId = :appId AND " +
            " (UPPER(al.data) like :searchText) " +
            " AND (:status IS NULL OR al.status in :status) " +
            " AND (:module IS NULL OR al.module in :module) " +
            " AND (:moduleId is NULL OR al.moduleId = :moduleId) " +
            " AND (:dateFrom is null OR :dateFrom < al.timestamp) " +
            " AND (:dateTo is null OR :dateTo > al.timestamp) ")

    Page<AppLog> findByQuery(Long appId,
                             String searchText,
                             String status,
                             String module,
                             String moduleId,
                             Long dateFrom,
                             Long dateTo,
                             String email,
                             Pageable pageable);
}
