package com.benzourry.leap.utility.audit;///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package my.unimas.iris.core.utility.audit;
//
//import my.unimas.iris.core.utility.Constant;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.oauth2.provider.OAuth2Authentication;
//
//import jakarta.persistence.PrePersist;
//import jakarta.persistence.PreUpdate;
//import java.util.Date;
//import java.util.Map;
//
///**
// * JPA EntityListener perform actions on callback
// * @author MohdRazif
// */
//public class AuditableLookupEntityListener {
//
////    public String getPrincipal() {
////        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
////
////        return authentication.getName();
////    }
//
//    public String getPrincipal(){
//        return getDetails().get("email");
//    }
//
//    public Map<String, String> getDetails(){
//        OAuth2Authentication auth = (OAuth2Authentication)SecurityContextHolder.getContext().getAuthentication();
//        Map<String, String> details = (Map<String, String>)auth.getUserAuthentication().getDetails();
////        System.out.println(details);
//        return details;
//    }
//
//    @PrePersist
//    public void prePersist(AuditableLookupEntity e) {
//        e.setAsOfDate(new Date());
//        e.setCreatedBy(getPrincipal());
//        e.setUpdatedBy(getPrincipal());
//        e.setActiveFlag(Constant.ACTIVE_FLAG);
//    }
//
//    @PreUpdate
//    public void preUpdate(AuditableLookupEntity e) {
//        e.setAsOfDate(new Date());
//        e.setActiveFlag(Constant.ACTIVE_FLAG);
//        e.setUpdatedBy(getPrincipal());
//    }
//
//}
