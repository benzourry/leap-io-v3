package com.benzourry.leap.service;

import com.benzourry.leap.repository.DynamicSQLRepository;
import com.benzourry.leap.security.UserPrincipal;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class SqlService {

    DynamicSQLRepository dynamicSQLRepository;

    public SqlService(DynamicSQLRepository dynamicSQLRepository){
        this.dynamicSQLRepository = dynamicSQLRepository;
    }

//    FOR LAMBDA
    public List select(String query, boolean nativeQuery) throws Exception {
        return this.dynamicSQLRepository.runQueryAsMap(query, Map.of(), nativeQuery);
    }

//    FOR LAMBDA
    public List select(String query, Map<String, Object> params, boolean nativeQuery) throws Exception {
        return this.dynamicSQLRepository.runQueryAsMap(query, params, nativeQuery);
    }

//    FOR LAMBDA
    public long count(String query, boolean nativeQuery) throws Exception {
        return this.dynamicSQLRepository.getQueryCount(query, Map.of(),nativeQuery);
    }

//    FOR LAMBDA
    public long count(String query, Map<String, Object> params, boolean nativeQuery) throws Exception {
        return this.dynamicSQLRepository.getQueryCount(query, params,nativeQuery);
    }

//    FOR LAMBDA
    public List select(String query, Map<String, Object> params,boolean nativeQuery, Pageable pageable) throws Exception {
        return this.dynamicSQLRepository.runPagedQueryAsMap(query, params, nativeQuery, pageable);
    }

//    FOR LAMBDA
    public List select(String query, boolean nativeQuery, Pageable pageable) throws Exception {
        return this.dynamicSQLRepository.runPagedQueryAsMap(query, Map.of(), nativeQuery, pageable);
    }

    @Transactional
    public int exec(String sql, Map<String, Object> params) throws Exception {
        if ("blmrazif@unimas.my".equals(getPrincipalEmail())||
            "benzourry@gmail.com".equals(getPrincipalEmail())) {
            return this.dynamicSQLRepository.executeQuery(sql, params);
        }else{
            throw new Exception("Unauthorized");
        }
    }


    public String getPrincipalEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserPrincipal) {
                return ((UserPrincipal) principal).getEmail();
            } else {
                // Optionally log or handle unexpected principal types
                return authentication.getName(); // fallback
            }
        } else {
            return "anonymous";
        }
    }

}
