package com.benzourry.leap.service;

import com.benzourry.leap.model.AccessToken;
import com.benzourry.leap.repository.AccessTokenRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Base64Utils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AccessTokenService {

    Map<String, AccessToken> accessToken = new HashMap<>();


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
//            AccessToken t = accessToken.get(pair);
//            System.out.println("AccessToken exist! expiry_time:"+at.getExpiry_time()+", current:"+System.currentTimeMillis()/1000 + ", expires_in:"+ at.getExpires_in());
//            System.out.println("< ACCESS TOKEN EXIST ==============");
//            System.out.println("current_time:"+(System.currentTimeMillis()/1000));
//            System.out.println("access_token:"+at.getAccess_token());
//            System.out.println("expiry_time:"+at.getExpiry_time());
//            System.out.println("expires_in:"+at.getExpires_in());
//            System.out.println("token_type:"+at.getToken_type());
//            System.out.println("scope:"+at.getScope());
//            System.out.println("=====================>");
            return at.getAccess_token();
        }else{
//            System.out.println("tokenEndpoint:"+tokenEndpoint+",clientId="+clientId+",clientSecret="+clientSecret);
            RestTemplate tokenRt = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            String basic = Base64Utils.encodeToString(pair.getBytes());
            headers.set("Authorization", "Basic " + basic);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<AccessToken> re = tokenRt.exchange(tokenEndpoint + "?grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret,
                    HttpMethod.POST, entity, AccessToken.class);

            AccessToken at = re.getBody();
//            System.out.println("< NEW ACCESS TOKEN REQUESTED =================");
//            System.out.println("access_token:"+at.getAccess_token());
//            System.out.println("expiry_time:"+at.getExpiry_time());
//            System.out.println("expires_in:"+at.getExpires_in());
//            System.out.println("token_type:"+at.getToken_type());
//            System.out.println("scope:"+at.getScope());
//            System.out.println("=====================>");
            at.setExpiry_time((System.currentTimeMillis()/1000) + at.getExpires_in());
//            this.accessToken.put(pair, at);

            at.setPair(pair);

            accessTokenRepository.save(at);

            return  at.getAccess_token();
        }

    }

}
