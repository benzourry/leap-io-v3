package com.benzourry.leap.repository;

import com.benzourry.leap.model.AppFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppFolderRepository extends JpaRepository<AppFolder, Long> {

    // Replace findByEmail with this:
    List<AppFolder> findByEmailOrderBySortOrderAsc(String email);

    // If using @Query for the keyword search, add "ORDER BY f.sortOrder ASC" to the end:
    @Query("SELECT f FROM AppFolder f WHERE f.email = :email AND " +
            " (LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(f.description) LIKE LOWER(CONCAT('%', :keyword, '%')))" +
            " ORDER BY f.sortOrder ASC")
    List<AppFolder> searchByEmailAndKeyword(@Param("email") String email, @Param("keyword") String keyword);
}