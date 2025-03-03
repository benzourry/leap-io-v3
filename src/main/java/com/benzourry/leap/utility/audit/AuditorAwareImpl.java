package com.benzourry.leap.utility.audit;

import com.benzourry.leap.security.UserPrincipal;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class AuditorAwareImpl implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        String email = "";
//        SecurityContextHolder.getContext().getAuthentication() is null when invoked by cogna tool
        if (SecurityContextHolder.getContext().getAuthentication()!=null) {
            if ("anonymousUser".equals(SecurityContextHolder.getContext().getAuthentication().getPrincipal())) {
                email = "ANONYMOUS";
            } else {
                email = ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getEmail();
            }
        }else{
            email = "ANONYMOUS";
        }
        return Optional.of(email);
    }
}
