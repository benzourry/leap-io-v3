package com.benzourry.leap.repository;

import com.benzourry.leap.model.CodeAuto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeAutoRepository extends JpaRepository<CodeAuto, Long> {
    @Query("select c from CodeAuto c where c.type like :type")
    List<CodeAuto> findByType(@Param("type") String type);
}
