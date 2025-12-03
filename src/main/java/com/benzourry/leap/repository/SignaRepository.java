package com.benzourry.leap.repository;

import com.benzourry.leap.model.Signa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SignaRepository extends JpaRepository<Signa, Long> {
    @Query("select s from Signa s where s.app.id = :appId")
    Page<Signa> findByAppId(@Param("appId") Long appId, Pageable pageable);
}
