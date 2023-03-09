package com.benzourry.leap.utility.audit;

import com.benzourry.leap.security.UserPrincipal;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class AuditorAwareImpl implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        String email = "";
        if ("anonymousUser".equals(SecurityContextHolder.getContext().getAuthentication().getPrincipal())){
            email = "ANONYMOUS";
        }else {
            email = ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getEmail();
        }
        return Optional.of(email);
    }
}
