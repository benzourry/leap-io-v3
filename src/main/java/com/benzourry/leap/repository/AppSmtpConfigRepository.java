package com.benzourry.leap.repository;

import com.benzourry.leap.model.AppSmtpConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AppSmtpConfigRepository extends JpaRepository<AppSmtpConfig, Long> {
    Optional<AppSmtpConfig> findByAppId(Long appId);
}