/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.benzourry.leap.utility.audit;

import com.benzourry.leap.model.BaseEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.util.Date;
//import org.hibernate.annotations.SQLDelete;
//import org.hibernate.annotations.Where;

/**
 *
 * @author MohdRazif
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
//@Customizer(value=AuditCustomizer.class)
//@SQLDelete(sql = "UPDATE #{#entityName} SET active_flag = 0 WHERE id = ?") //hibernate specific
//@AdditionalCriteria("this.activeFlag <> 0") //eclipselink specific  AND (:ownBy IS NULL OR this.ownBy=:ownBy)
//@Where(clause = "active_flag <> 0") //hibernate specific
public abstract class AuditableEntity extends BaseEntity {


    @Column(name = "CREATED_DATE", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @CreatedDate
    private Date createdDate;

    @Column(name = "MODIFIED_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    @LastModifiedDate
    private Date modifiedDate;

    @Column(name = "CREATED_BY", updatable = false)
    @CreatedBy
    private String createdBy;

    @Column(name = "MODIFIED_BY")
    @LastModifiedBy
    private String modifiedBy;

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Column(name = "DELETED")
    private boolean deleted;


    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

//    public Integer getActiveFlag() {
//        return activeFlag;
//    }
//
//    public void setActiveFlag(Integer activeFlag) {
//        this.activeFlag = activeFlag;
//    }

//    @PostPersist
//    public void postPersist() {
//        this.activeFlag = 1;
//    }
//
//    @PrePersist
//    public void prePersist() {
//        this.activeFlag = 1;
//    }

}
