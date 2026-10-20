package com.benzourry.leap.repository;

import com.benzourry.leap.model.DynamicOauth2Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DynamicOauth2ProviderRepository extends JpaRepository<DynamicOauth2Provider, Long> {
    Optional<DynamicOauth2Provider> findByRegistrationId(String registrationId);

    List<DynamicOauth2Provider> findByAppId(Long appId);
}