package com.benzourry.leap.repository;

import com.benzourry.leap.model.KryptaWallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface KryptaWalletRepository extends JpaRepository<KryptaWallet, Long> {
    @Query("select s from KryptaWallet s where s.app.id = :appId")
    Page<KryptaWallet> findByAppId(@Param("appId") Long appId, Pageable pageable);
}
