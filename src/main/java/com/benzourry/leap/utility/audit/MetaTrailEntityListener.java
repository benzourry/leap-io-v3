package com.benzourry.leap.utility.audit;

import com.benzourry.leap.security.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class MetaTrailEntityListener {

    @Lazy
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Lazy
    @Autowired
    private ObjectMapper mapper;

    @PostPersist
    public void onPostPersist(Object entity) {
        publishEvent(entity, "create");
    }

    @PostUpdate
    public void onPostUpdate(Object entity) {
        publishEvent(entity, "update");
    }

    @PostRemove
    public void onPostRemove(Object entity) {
        publishEvent(entity, "delete");
    }

    private void publishEvent(Object entity, String action) {
        if (entity instanceof Trailable trailable) {
            try {
                // Safely serialize. If Jackson fails, we catch it.
                JsonNode data = mapper.convertValue(entity, JsonNode.class);

                MetaTrailEvent event = new MetaTrailEvent(
                        trailable.getTrailId(),
                        trailable.getTrailModule(),
                        trailable.getTrailScope(),
                        action,
                        trailable.getTrailAppId(),
                        data,
                        getPrincipalEmail()
                );

                eventPublisher.publishEvent(event);
            } catch (Exception e) {
                // Log the error, but DO NOT rethrow it. The main transaction must succeed.
                LoggerFactory.getLogger(MetaTrailEntityListener.class)
                        .error("Failed to serialize audit trail for {} ID {}", trailable.getTrailModule(), trailable.getTrailId(), e);
            }
        }
    }
    private String getPrincipalEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserPrincipal userPrincipal) {
                return userPrincipal.getEmail();
            }
            return authentication.getName();
        }
        return "ANONYMOUS";
    }
}