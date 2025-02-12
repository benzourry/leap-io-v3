package com.benzourry.leap.security.oauth2.user;

import java.util.Map;

public class SarawakidOAuth2UserInfo extends OAuth2UserInfo {

    public SarawakidOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return (String) attributes.get("usr_id");
    }

    @Override
    public String getName() {
        return (String) attributes.get("usr_full_name");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("usr_email");
    }

    @Override
    public String getImageUrl() {
        return (String) attributes.get("picture");
    }
}
