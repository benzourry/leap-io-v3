package com.benzourry.leap.security.oauth2.user;

import java.util.Map;

public class GenericOAuth2UserInfo extends OAuth2UserInfo {

    public GenericOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return attributes.containsKey("sub") ? (String) attributes.get("sub") : (String) attributes.get("id");
    }

    @Override
    public String getName() {
        return attributes.containsKey("name") ? (String) attributes.get("name") : "User";
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getImageUrl() {
        return attributes.containsKey("picture") ? (String) attributes.get("picture") : (String) attributes.get("avatar_url");
    }
}