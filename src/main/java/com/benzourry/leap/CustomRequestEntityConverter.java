package com.benzourry.leap;

import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter;
import org.springframework.util.MultiValueMap;

//import com.fasterxml.jackson.databind.util.Converter;

public class CustomRequestEntityConverter implements Converter<OAuth2AuthorizationCodeGrantRequest, RequestEntity<?>> {

    private OAuth2AuthorizationCodeGrantRequestEntityConverter defaultConverter;

    public CustomRequestEntityConverter() {
        defaultConverter = new OAuth2AuthorizationCodeGrantRequestEntityConverter();
    }

    @Override
    public RequestEntity<?> convert(OAuth2AuthorizationCodeGrantRequest req) {
        RequestEntity<?> entity = defaultConverter.convert(req);
        MultiValueMap<String, String> params =  (MultiValueMap<String, String>) entity.getBody();
        String url = params.getFirst("redirect_uri");
        if(url.contains("facebook")||url.contains("twitter")||url.contains("azuread")){
            url = url.replace("http://", "https://");
        }
        params.set("redirect_uri", url);

        HttpMethod method = url.contains("sarawakid")?HttpMethod.POST: entity.getMethod();
        //log.info("Callback Request Parameters: "+params.toSingleValueMap().toString());
        return new RequestEntity<>(params, entity.getHeaders(),
                method, entity.getUrl());
    }

}