package com.benzourry.leap.security.oauth2.user;

import java.util.Map;

public class UnimasIdOAuth2UserInfo extends OAuth2UserInfo {

    public UnimasIdOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        if (attributes.get("universityId")!=null){
            return (String) attributes.get("universityId");
        }else{
            return (String) attributes.get("sub");
        }

    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getImageUrl() {
        return (String) attributes.get("picture");
    }
}
