/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.benzourry.leap.repository;

import com.benzourry.leap.model.EmailTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author MohdRazif
 */
@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

//    @Query("select l from EmailTemplate l where " +
//            " (l.shared=TRUE OR l.creator = :email) " +
//            " AND (upper(l.name) like :searchText or upper(l.description) like :searchText)")
//    Page<EmailTemplate> findByCreator(@Param("email") String email, @Param("searchText") String searchText, Pageable pageable);

    @Query("select l from EmailTemplate l where" +
            " l.app.id = :appId " +
            " AND (upper(l.name) like :searchText or upper(l.description) like :searchText)")
    Page<EmailTemplate> findByAppId(@Param("appId") Long appId, @Param("searchText") String searchText, Pageable pageable);

    @Query("select l from EmailTemplate l where" +
            " l.app.id = :appId " +
            " AND (upper(l.name) like :searchText or upper(l.description) like :searchText) AND l.pickable = TRUE")
    Page<EmailTemplate> findPickableByAppId(@Param("appId") Long appId, @Param("searchText") String searchText, Pageable pageable);

//    EmailTemplate findByCode(@Param("code") String code);

    EmailTemplate findByIdAndEnabled(@Param("id") Long id, @Param("enabled") Integer enabled);

    @Modifying
    @Query("delete from EmailTemplate s where s.app.id = :appId")
    void deleteByAppId(@Param("appId") Long dsId);


    //  public EmailTemplate findOne(Long id);
    
//    public List<EmailTemplate> findByActiveFlag(@Param("activeFlag") Integer activeFlag);
    
}
