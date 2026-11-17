package com.benzourry.leap.assistant;

import com.benzourry.leap.security.CurrentUser;
import com.benzourry.leap.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assistant/profiles")
@PreAuthorize("@authz.isDesigner()")
public class AssistantProfileController {

    private final AssistantProfileService profileService;

    public AssistantProfileController(AssistantProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public List<AssistantDtos.ProfileResponse> list(@CurrentUser UserPrincipal principal) {
        return profileService.list(principal.getEmail());
    }

    @PostMapping
    public AssistantDtos.ProfileResponse create(@CurrentUser UserPrincipal principal,
                                               @RequestBody AssistantDtos.ProfileRequest request) {
        return profileService.create(principal.getEmail(), request);
    }

    @PutMapping("/{id}")
    public AssistantDtos.ProfileResponse update(@CurrentUser UserPrincipal principal,
                                               @PathVariable("id") long id,
                                               @RequestBody AssistantDtos.ProfileRequest request) {
        return profileService.update(principal.getEmail(), id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@CurrentUser UserPrincipal principal, @PathVariable("id") long id) {
        profileService.delete(principal.getEmail(), id);
    }

    @PutMapping("/{id}/select")
    public AssistantDtos.ProfileResponse select(@CurrentUser UserPrincipal principal, @PathVariable("id") long id) {
        return profileService.select(principal.getEmail(), id);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> badRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_PLAIN).body(e.getMessage());
    }
}
