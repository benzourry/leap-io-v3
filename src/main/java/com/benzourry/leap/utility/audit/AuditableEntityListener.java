package com.benzourry.leap.utility.audit;///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package com.benzourry.leap.utility.audit;
//
//import my.unimas.iris.core.utility.Constant;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.oauth2.provider.OAuth2Authentication;
//
//import jakarta.persistence.PrePersist;
//import jakarta.persistence.PreUpdate;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * JPA EntityListener perform actions on callback
// * @author MohdRazif
// */
//public class AuditableEntityListener {
//
////    public String getPrincipal() {
////        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
////        return authentication.getName();
////    }
//
//    public String getPrincipal(){
//        return getDetails().get("email");
//    }
//
//    public Map<String, String> getDetails(){
//        Map<String, String> details = new HashMap<>();
//        System.out.println("principal: "+ SecurityContextHolder.getContext().getAuthentication().getPrincipal());
//        if ("anonymousUser".equals(SecurityContextHolder.getContext().getAuthentication().getPrincipal())){
//            details.put("email","ANONYMOUS");
//        }else{
//            OAuth2Authentication auth = (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication();
//            details = (Map<String, String>) auth.getUserAuthentication().getDetails();
//        }
////        OAuth2Authentication auth = (OAuth2Authentication)SecurityContextHolder.getContext().getAuthentication();
////
////        if (auth!=null) {
////            details = (Map<String, String>) auth.getUserAuthentication().getDetails();
////        }else{
////            details.put("email","ANONYMOUS");
////        }
////        System.out.println(details);
//        return details;
//    }
//
//    @PrePersist
//    public void prePersist(AuditableEntity e) {
//        getDetails();
//        e.setDateCreated(new Date());
//        e.setDateUpdated(new Date());
//        e.setCreatedBy(getPrincipal());
//        e.setUpdatedBy(getPrincipal());
//        e.setActiveFlag(Constant.ACTIVE_FLAG);
//
//    }
//
//    @PreUpdate
//    public void preUpdate(AuditableEntity e) {
//        e.setDateUpdated(new Date());
//        e.setActiveFlag(Constant.ACTIVE_FLAG);
////        System.out.println("Date Created:"+ e.getDateCreated());
//        e.setUpdatedBy(getPrincipal());
//    }
//
//}
