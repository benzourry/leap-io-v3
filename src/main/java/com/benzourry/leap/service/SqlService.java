package com.benzourry.leap.service;

import com.benzourry.leap.repository.DynamicSQLRepository;
import com.benzourry.leap.repository.KeyValueRepository;
import com.benzourry.leap.security.UserPrincipal;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class SqlService {

    private final DynamicSQLRepository dynamicSQLRepository;
    private final KeyValueRepository keyValueRepository;

    public SqlService(DynamicSQLRepository dynamicSQLRepository,
                      KeyValueRepository keyValueRepository) {
        this.dynamicSQLRepository = dynamicSQLRepository;
        this.keyValueRepository = keyValueRepository;
    }

    // --- HELPER METHOD FOR AUTHORIZATION ---
    private void checkAuthorization(String authKey) {
        String currentUser = getPrincipalEmail();

        if (currentUser == null) {
            throw new AccessDeniedException("Unauthenticated users cannot execute SQL operations.");
        }

        boolean isAllowed = keyValueRepository.getValue("platform", authKey)
                .map(allowedEmails -> Arrays.stream(allowedEmails.split(","))
                        .map(String::trim)
                        .anyMatch(email -> email.equalsIgnoreCase(currentUser)))
                .orElse(false);

        if (!isAllowed) {
            throw new AccessDeniedException("User unauthorized to perform SQL operation required by key: " + authKey);
        }
    }

    // --- SELECT METHODS ---
    public List<Object[]> select(String query, boolean nativeQuery) throws Exception {
        // Forward to the main select method to prevent duplicating the auth check
        return this.select(query, Collections.emptyMap(), nativeQuery);
    }

    public List<Object[]> select(String query, Map<String, Object> params, boolean nativeQuery) throws Exception {
        checkAuthorization("lambda_sql_select");
        return this.dynamicSQLRepository.runQueryAsMap(query, params, nativeQuery);
    }

    public List<Map> select(String query, boolean nativeQuery, Pageable pageable) throws Exception {
        // Forward to the main paged select method
        return this.select(query, Collections.emptyMap(), nativeQuery, pageable);
    }

    public List<Map> select(String query, Map<String, Object> params, boolean nativeQuery, Pageable pageable) throws Exception {
        checkAuthorization("lambda_sql_select");
        return this.dynamicSQLRepository.runPagedQueryAsMap(query, params, nativeQuery, pageable);
    }

    // --- COUNT METHODS ---
    public long count(String query, boolean nativeQuery) throws Exception {
        // Forward to the main count method
        return this.count(query, Collections.emptyMap(), nativeQuery);
    }

    public long count(String query, Map<String, Object> params, boolean nativeQuery) throws Exception {
        checkAuthorization("lambda_sql_select");
        return this.dynamicSQLRepository.getQueryCount(query, params, nativeQuery);
    }

    // --- EXEC METHOD ---
    @Transactional
    public int exec(String sql, Map<String, Object> params) throws Exception {
        checkAuthorization("lambda_sql_exec");
        return this.dynamicSQLRepository.executeQuery(sql, params);
    }

    // --- UTILITY ---
    public String getPrincipalEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserPrincipal) {
                return ((UserPrincipal) principal).getEmail();
            } else {
                return authentication.getName(); // fallback
            }
        }
        return null;
    }
}