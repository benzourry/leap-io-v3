package com.benzourry.leap.utility.audit;

import com.benzourry.leap.security.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import java.util.ArrayList;
import java.util.List;

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

                // Strip the computed getters (e.g. _f, _onSave, _pre) before saving
                stripComputedFields(data);

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

    // 1. Define the exact computed properties we want to ignore
    private static final java.util.Set<String> COMPUTED_FIELDS = java.util.Set.of(
            "_f", "_onSave", "_onSubmit", "_onView",
            "_pre", "_post", "_placeholder", "_data"
    );

    /**
     * Recursively removes specific computed fields from the snapshot.
     * Checks that the value is Textual to ensure we don't accidentally
     * delete Map entries (like an Item) that happen to share the same name.
     */
    private void stripComputedFields(JsonNode node) {
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;

            List<String> keysToRemove = new ArrayList<>();
            obj.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();

                // Only remove if it's an exact match AND the value is a String
                if (COMPUTED_FIELDS.contains(key) && value.isTextual()) {
                    keysToRemove.add(key);
                }
            });

            // Remove the matched computed properties
            obj.remove(keysToRemove);

            // Recurse into children (like items, sections)
            obj.elements().forEachRemaining(this::stripComputedFields);

        } else if (node.isArray()) {
            node.elements().forEachRemaining(this::stripComputedFields);
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