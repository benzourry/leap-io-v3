package com.benzourry.leap.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class DynamicOauth2Provider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String registrationId; // e.g. "custom-okta"
    private String clientName;
    private String clientId;
    private String clientSecret;

    // Comma-separated scopes e.g., "openid,profile,email"
    private String scopes;
    private String icon;

    private String authorizationUri;
    private String tokenUri;
    private String userInfoUri;
    private String jwkSetUri;
    private String userNameAttributeName; // e.g. "sub", "email", or "id"
    private Long appId;

    // Getters and Setters...
}