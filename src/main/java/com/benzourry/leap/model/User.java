package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import java.util.Date;
import java.util.Map;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
//@Table(name = "users", uniqueConstraints = {
//        @UniqueConstraint(columnNames = "email")
//})
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Email
    @Column(nullable = false)
    private String email;

    @Column(name = "IMAGE_URL", length = 5000, columnDefinition = "text")
    private String imageUrl;

    @Column(nullable = false)
    private Boolean emailVerified = false;

    @JsonIgnore
    private String password;

    private Long appId;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json", name="ATTRIBUTES")
    private JsonNode attributes;

    @Column(name = "FIRST_LOGIN")
    @Temporal(TemporalType.TIMESTAMP)
    private Date firstLogin;

    @Column(name = "LAST_LOGIN")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastLogin;

//    @NotNull
    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    private String providerId;

    @Column(name = "STATUS", length = 50)
    private String status; // pending, activated, rejected

    @Column(name = "PROVIDER_TOKEN", length = 5000, columnDefinition = "text")
    private String providerToken; // access_token dari provider

    @Column(name = "ONCE")
    private Boolean once;

//    @Type(type = "json")
//    @JsonIgnore
//    @Column(columnDefinition = "json", name="PUSH_SUB")
//    private JsonNode pushSub;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public AuthProvider getProvider() {
        return provider;
    }

    public void setProvider(AuthProvider provider) {
        this.provider = provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public JsonNode getAttributes() {
        return attributes;
    }

    public void setAttributes(JsonNode attributes) {
        this.attributes = attributes;
    }

    public Date getFirstLogin() {
        return firstLogin;
    }

    public void setFirstLogin(Date firstLogin) {
        this.firstLogin = firstLogin;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getOnce() {
        return once;
    }

    public void setOnce(Boolean once) {
        this.once = once;
    }

    public String getProviderToken() {
        return providerToken;
    }

    public void setProviderToken(String providerToken) {
        this.providerToken = providerToken;
    }

    public static User anonymous(){
        String random = RandomStringUtils.randomAlphanumeric(6);
        ObjectMapper mapper = new ObjectMapper();
        return new User(0l,"Guest","anonymous-"+random,"assets/img/avatar-big.png",true,null,0l,mapper.valueToTree(Map.of("name","Anonymous")),new Date(), new Date(),AuthProvider.local,"anonymous","approved",null, true);
    }
//    public JsonNode getPushSub() {
//        return pushSub;
//    }
//
//    public void setPushSub(JsonNode pushSub) {
//        this.pushSub = pushSub;
//    }
}
