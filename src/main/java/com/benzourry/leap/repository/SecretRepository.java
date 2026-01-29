package com.benzourry.leap.repository;


import com.benzourry.leap.model.KeyValue;
import com.benzourry.leap.model.Secret;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Created by MohdRazif on 1/8/2016.
 */
@Repository
public interface SecretRepository extends JpaRepository<Secret, Long> {

    Secret findByKey(String key);
    Optional<Secret> findByKeyAndAppId(String key, Long appId);

    List<Secret> findByAppId(Long appId);
    List<Secret> findByAppIdAndEnabled(Long appId, Integer enabled);
    @Cacheable(value = "platformSecretStr", key = "#appId + ':' + #key")
    @Query("select k.value from Secret k where k.key=:key and k.appId=:appId and k.enabled=1")
    Optional<String> getValue(@Param("appId") Long appId, @Param("key") String key);

    @Modifying
    @Query("delete from Secret s where s.appId = :appId")
    void deleteByAppId(@Param("appId") Long appId);

}
