package com.benzourry.leap.security.oauth2.user;

import java.util.Map;

public class LinkedInOAuth2UserInfo extends OAuth2UserInfo {

    String email;
    public LinkedInOAuth2UserInfo(Map<String, Object> attributes, String email) {
        super(attributes);
        this.email = email;
    }

    @Override
    public String getId() {
        return (String) attributes.get("id");
    }

    @Override
    public String getName() {
        return (String) attributes.get("localizedFirstName") +" "+(String) attributes.get("localizedLastName");
    }

    @Override
    public String getEmail() {
        return this.email;
    }


    @Override
    public String getImageUrl() {
//        String url =
        return (String) attributes.get("imageUrl");
    }

}
