package com.benzourry.leap.repository;

import com.benzourry.leap.model.Tier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TierRepository extends JpaRepository<Tier, Long> {
    List<Tier> findBySectionId(Long sectionId);
}
