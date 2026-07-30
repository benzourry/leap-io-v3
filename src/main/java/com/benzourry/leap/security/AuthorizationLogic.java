package com.benzourry.leap.security;

import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.service.KeyValueService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component("authz")
public class AuthorizationLogic {
    private final KeyValueService keyValueService;

    // Inject KeyValueService via constructor
    public AuthorizationLogic(KeyValueService keyValueService) {
        this.keyValueService = keyValueService;
    }
    public boolean isDesigner() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            return principal.getAppId() == -1;
        }
        return false;
    }

    public boolean isManager() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            return false;
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        // Fetch the managers string, default to empty string if not found
        String managers = keyValueService.getValue("platform", "managers").orElse("");

        // Parse and check if current user's email is in the list
        return Arrays.stream(managers.split(","))
                .map(String::trim)
                .anyMatch(email -> email.equalsIgnoreCase(principal.getEmail()));
    }
}