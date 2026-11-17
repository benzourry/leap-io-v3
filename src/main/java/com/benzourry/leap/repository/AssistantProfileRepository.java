package com.benzourry.leap.repository;

import com.benzourry.leap.model.AssistantProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssistantProfileRepository extends JpaRepository<AssistantProfile, Long> {
    List<AssistantProfile> findByUserEmailOrderByIdAsc(String userEmail);

    Optional<AssistantProfile> findByIdAndUserEmail(Long id, String userEmail);

    Optional<AssistantProfile> findFirstByUserEmailAndSelectedTrue(String userEmail);
}
