package com.benzourry.leap.repository;

import com.benzourry.leap.model.KryptaWalletInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface KryptaWalletInfoRepository extends JpaRepository<KryptaWalletInfo, Long> {
    @Query("select s from KryptaWalletInfo s where s.app.id = :appId")
    Page<KryptaWalletInfo> findByAppId(@Param("appId") Long appId, Pageable pageable);
}
