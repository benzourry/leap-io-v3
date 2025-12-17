package com.benzourry.leap.repository;

import com.benzourry.leap.model.Action;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScreenActionRepository extends JpaRepository<Action, Long> {

}
