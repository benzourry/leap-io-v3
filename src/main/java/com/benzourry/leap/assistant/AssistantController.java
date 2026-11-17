package com.benzourry.leap.assistant;

import com.benzourry.leap.security.CurrentUser;
import com.benzourry.leap.security.UserPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Endpoints used by the Angular assistant drawer.
 *
 * <p>Plan construction is read only: the browser applies the accepted steps through the regular
 * design services, so nothing here can change an app.</p>
 */
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private static final MediaType TEXT = new MediaType("text", "plain", StandardCharsets.UTF_8);

    private final AssistantService assistantService;
    private final AssistantProfileService profileService;

    public AssistantController(AssistantService assistantService, AssistantProfileService profileService) {
        this.assistantService = assistantService;
        this.profileService = profileService;
    }

    /** Availability probe used to hide the drawer when the deployment has no assistant. */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return assistantService.health();
    }

    @PostMapping("/{appId}/plan")
    @PreAuthorize("@authz.isDesigner()")
    public Object plan(@PathVariable("appId") long appId,
                       @RequestBody AssistantDtos.PlanRequest request,
                       @CurrentUser UserPrincipal principal) {
        AssistantDtos.PlanRequest resolved = resolveProfile(request, principal);
        assistantService.checkRequest(resolved);
        return assistantService.plan(appId, resolved);
    }

    /**
     * Streams the explanation as it is written and finishes with {@code <!--PLAN-->} followed by the
     * JSON plan, which is what the drawer expects.
     */
    @PostMapping(value = "/{appId}/plan-stream", produces = "text/plain;charset=UTF-8")
    @PreAuthorize("@authz.isDesigner()")
    public ResponseEntity<StreamingResponseBody> planStream(@PathVariable("appId") long appId,
                                                            @RequestBody AssistantDtos.PlanRequest request,
                                                            @CurrentUser UserPrincipal principal) {
        AssistantDtos.PlanRequest resolved = resolveProfile(request, principal);
        assistantService.checkRequest(resolved);
        // Validate the model client before committing the streaming response.
        assistantService.requireReady(resolved);

        StreamingResponseBody body = out -> assistantService.streamPlan(appId, resolved, out);
        return ResponseEntity.ok()
                .contentType(TEXT)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform")
                // Server compression is on for text/plain, and Tomcat buffers the response until
                // compressionMinSize is reached. Declaring the body as unencoded keeps the prose
                // arriving token by token.
                .header(HttpHeaders.CONTENT_ENCODING, "identity")
                .header("X-Accel-Buffering", "no")
                .body(body);
    }

    public ResponseEntity<StreamingResponseBody> planStream(long appId, AssistantDtos.PlanRequest request) {
        assistantService.checkRequest(request);
        assistantService.requireReady(request);
        StreamingResponseBody body = out -> assistantService.streamPlan(appId, request, out);
        return ResponseEntity.ok()
                .contentType(TEXT)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform")
                .header(HttpHeaders.CONTENT_ENCODING, "identity")
                .header("X-Accel-Buffering", "no")
                .body(body);
    }

    private AssistantDtos.PlanRequest resolveProfile(AssistantDtos.PlanRequest request, UserPrincipal principal) {
        if (request == null) {
            throw new IllegalArgumentException("Describe what you want the assistant to build.");
        }
        if (request.assistantId() == null && request.settings() != null) {
            return new AssistantDtos.PlanRequest(request.prompt(), request.history(), request.context(),
                    request.settings());
        }
        if (profileService == null || principal == null) {
            throw new IllegalArgumentException("An authenticated user is required to select an assistant.");
        }
        return profileService.withProfile(request, principal.getEmail());
    }

    @ExceptionHandler(AssistantUnavailableException.class)
    public ResponseEntity<String> unavailable(AssistantUnavailableException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).contentType(TEXT).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().contentType(TEXT).body(e.getMessage());
    }
}
