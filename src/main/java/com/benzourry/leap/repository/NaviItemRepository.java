package com.benzourry.leap.repository;

import com.benzourry.leap.model.NaviItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NaviItemRepository extends JpaRepository<NaviItem, Long> {
}
