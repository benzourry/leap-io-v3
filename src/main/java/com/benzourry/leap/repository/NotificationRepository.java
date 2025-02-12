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

    @Query("select n from Notification n where n.appId = :appId and concat(',',lower(n.email),',') like concat(',',:email,',')")
    Page<Notification> findByAppIdAndEmail(@Param("appId") long appId,@Param("email") String email, Pageable pageable);

    @Query("select n from Notification n where n.appId = :appId " +
            " and (:searchText is null OR (lower(n.content) LIKE :searchText OR lower(n.subject) LIKE :searchText OR lower(n.sender) LIKE :searchText)) " +
            " and (:email is null OR concat(',',lower(n.email),',') like concat('%,',:email,',%')) " +
            " and (:emailTemplateId is null OR n.emailTemplateId = :emailTemplateId) ")
    Page<Notification> findByAppIdAndParam(@Param("appId") long appId,
                                           @Param("searchText") String searchText,
                                           @Param("email") String email,
                                           @Param("emailTemplateId") Long emailTemplateId,
                                           Pageable pageable);
}
