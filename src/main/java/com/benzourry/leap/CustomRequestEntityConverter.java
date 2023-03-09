//package com.benzourry.leap;
//
//import org.springframework.core.convert.converter.Converter;
//import org.springframework.http.RequestEntity;
//import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
//import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter;
//import org.springframework.util.MultiValueMap;
//
////import com.fasterxml.jackson.databind.util.Converter;
//
//public class CustomRequestEntityConverter implements Converter<OAuth2AuthorizationCodeGrantRequest, RequestEntity<?>> {
//
//    private OAuth2AuthorizationCodeGrantRequestEntityConverter defaultConverter;
//
//    public CustomRequestEntityConverter() {
//        defaultConverter = new OAuth2AuthorizationCodeGrantRequestEntityConverter();
//    }
//
//    @Override
//    public RequestEntity<?> convert(OAuth2AuthorizationCodeGrantRequest req) {
//        System.out.println("%%%$$$:DLM REQUEST CONVERTER");
//        RequestEntity<?> entity = defaultConverter.convert(req);
//        MultiValueMap<String, String> params =  (MultiValueMap<String, String>) entity.getBody();
//        String url = params.getFirst("redirect_uri");
//        System.out.println("%%%$$$:REDIRECT_URI:"+url);
//        if(url.contains("facebook")||url.contains("twitter")||url.contains("azuread")){
//            url = url.replace("http://", "https://");
//        }
//        System.out.println("%%%$$$:REDIRECT_URI_CONVERTED:"+url);
//        params.set("redirect_uri", url);
//        System.out.println("%%%$$$:ALL_PARAMS:"+params);
//        //log.info("Callback Request Parameters: "+params.toSingleValueMap().toString());
//        return new RequestEntity<>(params, entity.getHeaders(),
//                entity.getMethod(), entity.getUrl());
//    }
//
//}