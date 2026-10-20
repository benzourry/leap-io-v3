package com.benzourry.leap.security.oauth2;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.DynamicOauth2Provider;
import com.benzourry.leap.repository.DynamicOauth2ProviderRepository;
import com.benzourry.leap.repository.SecretRepository;
import com.benzourry.leap.utility.Helper;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
@EnableConfigurationProperties(OAuth2ClientProperties.class)
public class DynamicClientRegistrationRepository implements ClientRegistrationRepository, Iterable<ClientRegistration> {

    private final Map<String, ClientRegistration> propertiesRegistrations;
    private final DynamicOauth2ProviderRepository providerRepository;

    // 1. Add the SecretRepository
    private final SecretRepository secretRepository;

    public DynamicClientRegistrationRepository(
            OAuth2ClientProperties properties,
            DynamicOauth2ProviderRepository providerRepository,
            SecretRepository secretRepository) {
        this.providerRepository = providerRepository;
        this.secretRepository = secretRepository;

        // Map all hardcoded clients from application.properties into memory
        this.propertiesRegistrations = new OAuth2ClientPropertiesMapper(properties).asClientRegistrations();
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        ClientRegistration memoryRegistration = propertiesRegistrations.get(registrationId);
        if (memoryRegistration != null) {
            return memoryRegistration;
        }

        return providerRepository.findByRegistrationId(registrationId)
                .map(this::buildClientRegistration)
                .orElse(null);
    }

    private ClientRegistration buildClientRegistration(DynamicOauth2Provider dbProvider) {
        String clientSecret = dbProvider.getClientSecret();
        Long appId = dbProvider.getAppId();

        // 2. Resolve the dynamic {{_secret.*}} tag before passing it to Spring Security
        if (clientSecret != null && clientSecret.contains("{{_secret.")) {
            String key = Helper.extractTemplateKey(clientSecret, "{{_secret.", "}}").orElseThrow(() -> {
                return new RuntimeException("Cannot extract secret key from template");
            });

            clientSecret = secretRepository.findByKeyAndAppId(key, appId)
                    .orElseThrow(() -> new ResourceNotFoundException("Secret", "key+appId", key + "+" + appId))
                    .getValue();
        }

        return ClientRegistration.withRegistrationId(dbProvider.getRegistrationId())
                .clientName(dbProvider.getClientName())
                .clientId(dbProvider.getClientId())
                .clientSecret(clientSecret) // Inject resolved secret here
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/{action}/oauth2/code/{registrationId}")
                .scope(dbProvider.getScopes() != null ? dbProvider.getScopes().split(",") : new String[]{"openid", "profile", "email"})
                .authorizationUri(dbProvider.getAuthorizationUri())
                .tokenUri(dbProvider.getTokenUri())
                .userInfoUri(dbProvider.getUserInfoUri())
                .jwkSetUri(dbProvider.getJwkSetUri())
                .userNameAttributeName(dbProvider.getUserNameAttributeName())
                .build();
    }

    @Override
    public Iterator<ClientRegistration> iterator() {
        List<ClientRegistration> allRegistrations = new ArrayList<>(propertiesRegistrations.values());

        providerRepository.findAll().forEach(dbProvider -> {
            allRegistrations.add(buildClientRegistration(dbProvider));
        });

        return allRegistrations.iterator();
    }
}