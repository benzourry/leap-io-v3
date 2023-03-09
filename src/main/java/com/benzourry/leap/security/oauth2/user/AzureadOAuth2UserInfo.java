package com.benzourry.leap.security.oauth2.user;

import java.util.Map;

public class AzureadOAuth2UserInfo extends OAuth2UserInfo {

    public AzureadOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return (String) attributes.get("id");
    }

    @Override
    public String getName() {
        return (String) attributes.get("displayName");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("userPrincipalName");
    }

    @Override
    public String getImageUrl() {
        return "https://graph.microsoft.com/v1.0/me/photo/$value";
//        return (String) attributes.get("picture");
    }
}
