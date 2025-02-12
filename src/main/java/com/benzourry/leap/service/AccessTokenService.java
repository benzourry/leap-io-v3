package com.benzourry.leap.service;

import com.benzourry.leap.model.AccessToken;
import com.benzourry.leap.repository.AccessTokenRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
//import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AccessTokenService {

//    Map<String, AccessToken> accessToken = new HashMap<>();


    private final AccessTokenRepository accessTokenRepository;

    public AccessTokenService(AccessTokenRepository accessTokenRepository){
        this.accessTokenRepository = accessTokenRepository;
    }

    public List<AccessToken> getAllAccessToken(){
        return this.accessTokenRepository.findAll();
    }

    public void clearAccessToken(String pair){
        try {
            this.accessTokenRepository.deleteById(pair);
        }catch(Exception e){
            System.out.println("Error clear access token:" + e.getMessage());
        }
//        accessToken.remove(pair);
    }

    /**
     * THE PROBLEM IF MULTIPLE ACCESS_TOKEN CRETAED AT THE SAME TIME WILL BE DUPLICATE TOKEN ERROR FROM OAUTH SERVER
     */
    @Transactional
    public String getAccessToken(String tokenEndpoint, String clientId, String clientSecret){
        String pair = clientId+":"+clientSecret;
        Optional<AccessToken> t = this.accessTokenRepository.findById(pair); //this.accessToken.get(pair);
        // if expiry in 3 sec, always request for new token ! No need to request before 30sec. Just request when expired
        if (t.isPresent() && ((System.currentTimeMillis()/1000)+3)<t.get().getExpiry_time()){
            AccessToken at = t.get();
            return at.getAccess_token();
        }else{
            RestTemplate tokenRt = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(clientId, clientSecret);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            LinkedMultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
            params.add("grant_type","client_credentials");

            HttpEntity<LinkedMultiValueMap<String, Object>> entity = new HttpEntity<>(params,headers);

            //&client_id=" + clientId + "&client_secret=" + clientSecret
            ResponseEntity<AccessToken> re = tokenRt.exchange(tokenEndpoint,
                    HttpMethod.POST, entity, AccessToken.class);

            AccessToken at = re.getBody();

            at.setExpiry_time((System.currentTimeMillis()/1000) + at.getExpires_in());
            at.setPair(pair);

            accessTokenRepository.save(at);

            return  at.getAccess_token();
        }

    }

}
