package com.benzourry.leap.repository;

import com.benzourry.leap.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("select n from Notification n where n.appId = :appId and n.email = :email")
    Page<Notification> findByAppIdAndEmail(@Param("appId") long appId,@Param("email") String email, Pageable pageable);
}
