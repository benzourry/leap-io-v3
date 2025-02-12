package com.benzourry.leap.security;

import com.benzourry.leap.model.User;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class ApiKeyAuth extends AbstractAuthenticationToken {

    private final String apiKey;

    public ApiKeyAuth(String apiKey, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.apiKey = apiKey;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
//        UserPrincipal principal = new UserPrincipal(0l,"anonymous","",0l,"",null);
        return apiKey;
    }

    @Override
    public Object getPrincipal() {
        return UserPrincipal.create(User.anonymous());
    }

}