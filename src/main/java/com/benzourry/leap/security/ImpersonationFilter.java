package com.benzourry.leap.security;

import com.benzourry.leap.model.App;
import com.benzourry.leap.model.User;
import com.benzourry.leap.repository.AppRepository;
import com.benzourry.leap.repository.UserRepository;
import com.benzourry.leap.utility.Helper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@Component
public class ImpersonationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final AppRepository appRepository;

    public ImpersonationFilter(UserRepository userRepository, AppRepository appRepository) {
        this.userRepository = userRepository;
        this.appRepository = appRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String impersonateEmail = request.getHeader("X-Impersonate-User");
        String impersonateAppIdStr = request.getHeader("X-Impersonate-App");
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();

        // 1. Check if headers exist and the current user is fully authenticated
        if (impersonateEmail != null && impersonateAppIdStr != null && currentAuth != null && currentAuth.isAuthenticated()) {
            try {
                Long appId = Long.parseLong(impersonateAppIdStr);

                // Get the real logged-in Creator's email
                UserPrincipal currentPrincipal = (UserPrincipal) currentAuth.getPrincipal();
                String currentUserEmail = currentPrincipal.getEmail();

                Optional<App> appOpt = appRepository.findById(appId);

                if (appOpt.isPresent()) {
                    App app = appOpt.get();
                    String creatorEmails = app.getEmail(); // Comma-separated string of creator emails

                    // 2. SECURITY CHECK: Verify if the logged-in user is a creator of this app
                    boolean isCreator = false;
                    if (creatorEmails != null) {
                        isCreator = Arrays.stream(creatorEmails.split(","))
                                .map(String::trim)
                                .anyMatch(email -> email.equalsIgnoreCase(currentUserEmail));
                    }

                    if (!isCreator) {
                        // Unauthorized! Reject the request immediately.
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have creator access to impersonate users on this app.");
                        return;
                    }

                    // 3. Find the target user in the database
                    Optional<User> targetUserOpt = userRepository.findFirstByEmailAndAppId(impersonateEmail, appId);
                    UserPrincipal targetPrincipal;

                    if (targetUserOpt.isPresent()) {
                        // User exists -> Create principal from database record
                        targetPrincipal = UserPrincipal.create(targetUserOpt.get());
                    } else {
                        // User DOES NOT exist -> Spin up the Ghost User for debugging
                        Map<String, Object> attributes = Map.of(
                                "email", impersonateEmail,
                                "name", Helper.capitalize(impersonateEmail.split("@")[0]),
                                "debug", true
                        );

                        targetPrincipal = new UserPrincipal(
                                -1L, impersonateEmail, "", appId, null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                        );
                        targetPrincipal.setAttributes(attributes);
                    }

                    // 4. Overwrite the SecurityContext for this specific request
                    UsernamePasswordAuthenticationToken impersonatedAuth = new UsernamePasswordAuthenticationToken(
                            targetPrincipal, null, targetPrincipal.getAuthorities()
                    );

                    // --- NEW: Add the original creator to the authentication details ---
                    Map<String, Object> authDetails = new HashMap<>();
                    authDetails.put("isImpersonation", true);
                    authDetails.put("impersonatorEmail", currentPrincipal.getEmail());
                    authDetails.put("impersonatorId", currentPrincipal.getId());
                    authDetails.put("remoteAddress", request.getRemoteAddr()); // Keep IP address for logging

                    impersonatedAuth.setDetails(authDetails);

                    SecurityContextHolder.getContext().setAuthentication(impersonatedAuth);

                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "App not found.");
                    return;
                }

            } catch (NumberFormatException e) {
                logger.warn("Invalid App ID in impersonation header");
            }
        }

        // Continue the filter chain
        filterChain.doFilter(request, response);
    }
}