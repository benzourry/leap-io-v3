package com.benzourry.leap.security;


import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.User;
import com.benzourry.leap.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Created by rajeevkumarsingh on 02/08/17.
 */

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

//        System.out.println("username::"+username);

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