package com.benzourry.leap.assistant;

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

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    /** Availability probe used to hide the drawer when the deployment has no assistant. */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return assistantService.health();
    }

    @PostMapping("/{appId}/plan")
    @PreAuthorize("@authz.isDesigner()")
    public Object plan(@PathVariable("appId") long appId,
                       @RequestBody AssistantDtos.PlanRequest request) {
        assistantService.checkRequest(request);
        return assistantService.plan(appId, request);
    }

    /**
     * Streams the explanation as it is written and finishes with {@code <!--PLAN-->} followed by the
     * JSON plan, which is what the drawer expects.
     */
    @PostMapping(value = "/{appId}/plan-stream", produces = "text/plain;charset=UTF-8")
    @PreAuthorize("@authz.isDesigner()")
    public ResponseEntity<StreamingResponseBody> planStream(@PathVariable("appId") long appId,
                                                            @RequestBody AssistantDtos.PlanRequest request) {
        assistantService.checkRequest(request);
        // Fail before the response is committed so a missing configuration is a real 503.
        assistantService.requireReady();

        StreamingResponseBody body = out -> assistantService.streamPlan(appId, request, out);
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

    @ExceptionHandler(AssistantUnavailableException.class)
    public ResponseEntity<String> unavailable(AssistantUnavailableException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).contentType(TEXT).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().contentType(TEXT).body(e.getMessage());
    }
}
