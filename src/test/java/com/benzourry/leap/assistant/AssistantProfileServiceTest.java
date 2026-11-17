package com.benzourry.leap.assistant;

import com.benzourry.leap.model.AssistantProfile;
import com.benzourry.leap.repository.AssistantProfileRepository;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssistantProfileServiceTest {

    @Test
    void storesEncryptedKeyAndReturnsOnlyConfiguredStatus() {
        AssistantProfileRepository repository = mock(AssistantProfileRepository.class);
        AssistantCredentialCipher cipher = new AssistantCredentialCipher("dGVzdC1zZWNyZXQ=");
        AssistantProfileService service = new AssistantProfileService(repository, cipher, new AssistantModelFactory());
        when(repository.findByUserEmailOrderByIdAsc("creator@example.com")).thenReturn(List.of());
        when(repository.save(any(AssistantProfile.class))).thenAnswer(invocation -> {
            AssistantProfile profile = invocation.getArgument(0);
            profile.setId(12L);
            return profile;
        });

        AssistantDtos.ProfileResponse response = service.create("Creator@example.com",
                new AssistantDtos.ProfileRequest("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini",
                        "private-api-key", "Keep answers concise.", 12));

        ArgumentCaptor<AssistantProfile> saved = ArgumentCaptor.forClass(AssistantProfile.class);
        verify(repository).save(saved.capture());
        assertEquals("creator@example.com", saved.getValue().getUserEmail());
        assertFalse(saved.getValue().getEncryptedApiKey().contains("private-api-key"));
        assertEquals("private-api-key", cipher.decrypt(saved.getValue().getEncryptedApiKey()));
        assertTrue(response.apiKeyConfigured());
        assertFalse(response.toString().contains("private-api-key"));
        assertEquals(Integer.valueOf(12), response.maxActions());
    }

    @Test
    void onlyResolvesProfilesOwnedByTheAuthenticatedEmail() {
        AssistantProfileRepository repository = mock(AssistantProfileRepository.class);
        AssistantProfileService service = new AssistantProfileService(repository,
                new AssistantCredentialCipher("dGVzdC1zZWNyZXQ="), new AssistantModelFactory());
        when(repository.findByIdAndUserEmail(12L, "other@example.com")).thenReturn(java.util.Optional.empty());
        AssistantDtos.PlanRequest request = new AssistantDtos.PlanRequest(
                "make a form", null, null, null, 12L, null, null);

        org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
                () -> service.withProfile(request, "Other@example.com"));

        verify(repository).findByIdAndUserEmail(12L, "other@example.com");
    }
}
