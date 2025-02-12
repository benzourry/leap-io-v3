package com.benzourry.leap.utility.audit;///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package my.unimas.iris.core.utility.audit;
//
//import my.unimas.iris.core.model.GenericEntity;
//
//import jakarta.persistence.*;
//import java.util.Date;
//
///**
// *
// * @author MohdRazif
// */
//@MappedSuperclass
//@EntityListeners(AuditableLookupEntityListener.class)
//public abstract class AuditableLookupEntity extends GenericEntity {
//
//    @Column(name = "AS_OF_DATE")
//    @Temporal(TemporalType.TIMESTAMP)
//    private Date asOfDate; // = new Date();
//
//    @Column(name = "CREATED_BY", length=50)
//    private String createdBy;
//
//    @Column(name = "UPDATED_BY", length=50)
//    private String updatedBy;
//
// //   //@JsonView(LookupView.Admin.class)
//    @Column(name = "ACTIVE_FLAG")
//    private Integer activeFlag;
//
//
//    @Column(name="EXTRA", length=150)
//    private String extra;
//
//    @Column(name="EXTRA_2", length=150)
//    private String extra2;
//    /**
//     * @return the createdBy
//     */
//    public String getCreatedBy() {
//        return createdBy;
//    }
//
//    /**
//     * @param createdBy the createdBy to set
//     */
//    public void setCreatedBy(String createdBy) {
//        this.createdBy = createdBy;
//    }
//
//    /**
//     * @return the updatedBy
//     */
//    public String getUpdatedBy() {
//        return updatedBy;
//    }
//
//    /**
//     * @param updatedBy the updatedBy to set
//     */
//    public void setUpdatedBy(String updatedBy) {
//        this.updatedBy = updatedBy;
//    }
//
//    /**
//     * @return the asOfDate
//     */
//    public Date getAsOfDate() {
//        return asOfDate;
//    }
//
//    /**
//     * @param asOfDate the asOfDate to set
//     */
//    public void setAsOfDate(Date asOfDate) {
//        this.asOfDate = asOfDate;
//    }
//
//    public Integer getActiveFlag() {
//        return activeFlag;
//    }
//
//    public void setActiveFlag(Integer activeFlag) {
//        this.activeFlag = activeFlag;
//    }
//
//
//    public String getExtra() {
//        return extra;
//    }
//
//    public void setExtra(String extra) {
//        this.extra = extra;
//    }
//
//    public String getExtra2() {
//        return extra2;
//    }
//
//    public void setExtra2(String extra2) {
//        this.extra2 = extra2;
//    }
//}
