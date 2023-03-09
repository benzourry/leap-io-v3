package com.benzourry.leap.security.oauth2.user;

import java.util.Map;

public class TwitterOAuth2UserInfo extends OAuth2UserInfo {

    public TwitterOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return ((Map<String,String>) attributes.get("data")).get("id");
    }

    @Override
    public String getName() {
        return ((Map<String,String>) attributes.get("data")).get("name");
    }

    @Override
    public String getEmail() {
        return ((Map<String,String>) attributes.get("data")).get("username")+"@twitter.com";
    }

    @Override
    public String getImageUrl() {
        return ((Map<String,String>) attributes.get("data")).get("default_profile_image");
//        return (String) attributes.get("picture");
    }
}
