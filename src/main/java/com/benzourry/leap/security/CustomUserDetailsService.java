package com.benzourry.leap.security;


import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.User;
import com.benzourry.leap.repository.KeyValueRepository;
import com.benzourry.leap.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Created by rajeevkumarsingh on 02/08/17.
 */

//This service is used for manual account user signin (using email & password)
@Service
public class CustomUserDetailsService implements UserDetailsService {

    final UserRepository userRepository;

    final KeyValueRepository keyValueRepository;

    public CustomUserDetailsService(UserRepository userRepository, KeyValueRepository keyValueRepository) {
        this.userRepository = userRepository;
        this.keyValueRepository = keyValueRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        String[] usernameAndAppId = StringUtils.split(
                username, "\n");
        if (usernameAndAppId == null || usernameAndAppId.length != 2) {
            throw new UsernameNotFoundException("Username and AppId must be provided");
        }
        String email = usernameAndAppId[0];
        Long appId = Long.parseLong(usernameAndAppId[1]);
        User user = userRepository.findFirstByEmailAndAppId(email,appId)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User for AppId ["+appId+"] not found with email : " + email)
                );

        if (Long.valueOf(-1).equals(appId)){

            Optional<String> allowedCreatorEmail = keyValueRepository.getValue("platform", "allowed-creator-email");
            AntPathMatcher am = new AntPathMatcher();

            if (allowedCreatorEmail.isPresent()){
                String[] patterns = allowedCreatorEmail.get().split(",");
                boolean match = false;
                for (String pattern: patterns){
                    if (am.match(pattern.trim(), email)){
                        match = true;
                        break;
                    }
                }
                if (!match){
                    throw new AuthenticationServiceException("User not allowed to login as Creator : " + email);
                }
            }
        }

        return UserPrincipal.create(user);
    }

    @Transactional
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(
            () -> new ResourceNotFoundException("User", "id", id)
        );

        return UserPrincipal.create(user);
    }
}