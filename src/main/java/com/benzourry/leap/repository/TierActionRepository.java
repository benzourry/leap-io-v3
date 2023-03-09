package com.benzourry.leap.repository;

import com.benzourry.leap.model.TierAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TierActionRepository extends JpaRepository<TierAction, Long> {
}
