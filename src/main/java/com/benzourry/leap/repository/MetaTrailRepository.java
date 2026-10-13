package com.benzourry.leap.repository;

import com.benzourry.leap.model.MetaTrail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetaTrailRepository  extends JpaRepository<MetaTrail, String> {
}
