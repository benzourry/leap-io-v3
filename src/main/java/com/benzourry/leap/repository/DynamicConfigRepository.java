package com.benzourry.leap.repository;

import com.benzourry.leap.model.DynamicConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DynamicConfigRepository extends JpaRepository<DynamicConfig,Long> {

    DynamicConfig findByProp(String prop);
}
