package com.benzourry.leap.repository;

import com.benzourry.leap.model.AuthProvider;
import com.benzourry.leap.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

//    Optional<User> findByEmail(String email);

    Optional<User> findFirstByEmailAndAppId(String email, Long appId);

//    Optional<User> findByEmailAndProvider(String email, AuthProvider provider);

//    List<User> findAllByAppId(Long appId);

//    List<User> findAllByAppIdAndPushSubNotNull(Long appId);

    Boolean existsByEmailAndAppId(String email, Long appId);

    long countByAppId(Long appId);


    @Modifying
    @Query("delete from User s where s.appId = :appId")
    void deleteByAppId(@Param("appId") Long appId);

//    @Modifying
//    @Query("delete from User s where s.provider = :provider and s.providerId = :providerId")
//    void deleteByProviderId(@Param("provider") String provider, @Param("providerId") String providerId);

}
