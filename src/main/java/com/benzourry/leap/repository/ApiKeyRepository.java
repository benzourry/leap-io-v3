package com.benzourry.leap.repository;

import com.benzourry.leap.model.ApiKey;
import com.benzourry.leap.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    @Query(value = "select s from ApiKey s where s.appId = :appId")
    List<ApiKey> findByAppId(@Param("appId") Long appId);

    @Query(value = "select s from ApiKey s where s.apiKey = :apiKey")
    ApiKey findFirstByApiKey(@Param("apiKey") String apiKey);

    long countByApiKey(String apiKey);

    @Modifying
    @Query("delete from ApiKey s where s.appId = :appId")
    void deleteByAppId(@Param("appId") Long appId);

}
