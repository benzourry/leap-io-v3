package com.benzourry.leap.repository;


import com.benzourry.leap.model.AppGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppGroupRepository extends JpaRepository<AppGroup, Long> {

    @Query("select a from AppGroup a where (:email is null OR (concat(',',REPLACE(a.managers,' ',''),',') like concat('%',concat(',',:email,','),'%'))) ")
    Page<AppGroup> findByParams(@Param("email") String email, Pageable unpaged);
}
