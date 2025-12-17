package com.benzourry.leap.repository;


import com.benzourry.leap.model.RestorePoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RestorePointRepository extends JpaRepository<RestorePoint, Long> {

    @Query(value = "select * from restore_point f where f.app_id = :appId", nativeQuery = true)
    Page<RestorePoint> findByAppId(@Param("appId") Long appId, Pageable pageable);
    @Query(value = "select count(*) as total from restore_point n where n.app_id = :appId", nativeQuery = true)
    long countByAppId(@Param("appId") Long appId);

}
