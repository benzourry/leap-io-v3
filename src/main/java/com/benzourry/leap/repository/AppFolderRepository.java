package com.benzourry.leap.repository;

import com.benzourry.leap.model.AppFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppFolderRepository extends JpaRepository<AppFolder, Long> {

    // 1. Basic query to get all folders for a specific creator
    List<AppFolder> findByEmail(String email);

    // 2. Searchable query to filter by email AND a search keyword (checks both name and description)
    @Query("SELECT f FROM AppFolder f WHERE f.email = :email AND " +
            "(LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(f.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<AppFolder> searchByEmailAndKeyword(@Param("email") String email, @Param("keyword") String keyword);

}