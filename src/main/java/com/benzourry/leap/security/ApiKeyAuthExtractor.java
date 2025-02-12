package com.benzourry.leap.security;

import com.benzourry.leap.repository.ApiKeyRepository;
import com.benzourry.leap.utility.Helper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Component
public class ApiKeyAuthExtractor {

//    @Value("${application.security.api-key}")
//    private String apiKey;

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyAuthExtractor(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    public Optional<Authentication> extract(HttpServletRequest request) {
        String providedKey = Helper.getApiKey(request);
        if (providedKey == null || apiKeyRepository.countByApiKey(providedKey)==0)
            return Optional.empty();

        return Optional.of(new ApiKeyAuth(providedKey, AuthorityUtils.NO_AUTHORITIES));
    }


}