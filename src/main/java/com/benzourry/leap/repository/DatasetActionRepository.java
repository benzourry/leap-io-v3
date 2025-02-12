package com.benzourry.leap.repository;

import com.benzourry.leap.model.DatasetAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DatasetActionRepository extends JpaRepository<DatasetAction, Long> {

}
