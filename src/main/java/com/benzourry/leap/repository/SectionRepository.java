package com.benzourry.leap.repository;

import com.benzourry.leap.model.Section;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {

    @Query("select s from Section s where s.form.id = :formId")
    Page<Section> findByFormId(@Param("formId") long formId, Pageable pageable);

    // Add this to ALL your repositories (FormRepo, ItemRepo, ScreenRepo, etc.)
    @Query("SELECT e.id FROM Section e")
    List<Long> findAllIds();
}
