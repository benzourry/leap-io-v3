package com.benzourry.leap.assistant;

import com.benzourry.leap.model.AssistantProfile;
import com.benzourry.leap.repository.AssistantProfileRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class AssistantProfileService {

    private final AssistantProfileRepository repository;
    private final AssistantCredentialCipher credentialCipher;
    private final AssistantModelFactory modelFactory;

    public AssistantProfileService(AssistantProfileRepository repository,
                                   AssistantCredentialCipher credentialCipher,
                                   AssistantModelFactory modelFactory) {
        this.repository = repository;
        this.credentialCipher = credentialCipher;
        this.modelFactory = modelFactory;
    }

    public List<AssistantDtos.ProfileResponse> list(String email) {
        return repository.findByUserEmailOrderByIdAsc(normalizeEmail(email)).stream()
                .map(this::response)
                .toList();
    }

    @Transactional
    public AssistantDtos.ProfileResponse create(String email, AssistantDtos.ProfileRequest request) {
        AssistantProfile profile = new AssistantProfile();
        profile.setUserEmail(normalizeEmail(email));
        apply(profile, request, true);
        List<AssistantProfile> existing = repository.findByUserEmailOrderByIdAsc(profile.getUserEmail());
        profile.setSelected(existing.isEmpty());
        return response(repository.save(profile));
    }

    @Transactional
    public AssistantDtos.ProfileResponse update(String email, long id, AssistantDtos.ProfileRequest request) {
        AssistantProfile profile = ownedProfile(email, id);
        apply(profile, request, false);
        return response(repository.save(profile));
    }

    @Transactional
    public void delete(String email, long id) {
        String owner = normalizeEmail(email);
        AssistantProfile profile = ownedProfile(owner, id);
        boolean wasSelected = profile.isSelected();
        repository.delete(profile);
        if (wasSelected) {
            repository.findByUserEmailOrderByIdAsc(owner).stream().findFirst().ifPresent(next -> {
                next.setSelected(true);
                repository.save(next);
            });
        }
    }

    @Transactional
    public AssistantDtos.ProfileResponse select(String email, long id) {
        String owner = normalizeEmail(email);
        AssistantProfile selected = ownedProfile(owner, id);
        repository.findByUserEmailOrderByIdAsc(owner).forEach(profile -> {
            profile.setSelected(profile.getId().equals(id));
            repository.save(profile);
        });
        return response(selected);
    }

    public AssistantDtos.PlanRequest withProfile(AssistantDtos.PlanRequest request, String email) {
        AssistantProfile profile = request.assistantId() == null
                ? repository.findFirstByUserEmailAndSelectedTrue(normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Select an assistant in your developer profile first."))
                : ownedProfile(email, request.assistantId());
        AssistantDtos.ModelSettings settings = new AssistantDtos.ModelSettings(
                profile.getApiBaseUrl(), profile.getModel(), credentialCipher.decrypt(profile.getEncryptedApiKey()));
        return new AssistantDtos.PlanRequest(request.prompt(), request.history(), request.context(), settings,
                profile.getId(), profile.getAdditionalInstructions(), profile.getMaxActions());
    }

    private void apply(AssistantProfile profile, AssistantDtos.ProfileRequest request, boolean creating) {
        if (request == null || request.name() == null || request.name().isBlank()
                || request.name().trim().length() > 100) {
            throw new IllegalArgumentException("Assistant name is required and must be 100 characters or fewer.");
        }
        if (request.model() == null || request.model().isBlank()
                || request.apiBaseUrl() == null || request.apiBaseUrl().isBlank()) {
            throw new IllegalArgumentException("Enter an API base URL and model.");
        }
        if (request.maxActions() == null || request.maxActions() < 1 || request.maxActions() > 60) {
            throw new IllegalArgumentException("Maximum actions must be between 1 and 60.");
        }
        if (request.additionalInstructions() != null && request.additionalInstructions().length() > 10000) {
            throw new IllegalArgumentException("Additional instructions must be 10,000 characters or fewer.");
        }

        AssistantDtos.ModelSettings validated = modelFactory.normalize(
                new AssistantDtos.ModelSettings(request.apiBaseUrl(), request.model(), "validation-key"));
        profile.setName(request.name().trim());
        profile.setApiBaseUrl(validated.apiBaseUrl());
        profile.setModel(validated.model());
        profile.setAdditionalInstructions(request.additionalInstructions() == null
                ? "" : request.additionalInstructions().trim());
        profile.setMaxActions(request.maxActions());

        String apiKey = request.apiKey() == null ? "" : request.apiKey().trim();
        if (!apiKey.isEmpty()) {
            profile.setEncryptedApiKey(credentialCipher.encrypt(apiKey));
        } else if (creating || profile.getEncryptedApiKey() == null || profile.getEncryptedApiKey().isBlank()) {
            throw new IllegalArgumentException("Enter an API key for this assistant.");
        }
    }

    private AssistantProfile ownedProfile(String email, long id) {
        return repository.findByIdAndUserEmail(id, normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assistant was not found."));
    }

    private AssistantDtos.ProfileResponse response(AssistantProfile profile) {
        return new AssistantDtos.ProfileResponse(profile.getId(), profile.getName(), profile.getApiBaseUrl(),
                profile.getModel(), profile.getAdditionalInstructions(), profile.getMaxActions(),
                profile.isSelected(), profile.getEncryptedApiKey() != null
                && !profile.getEncryptedApiKey().isBlank());
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("An authenticated user email is required.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
