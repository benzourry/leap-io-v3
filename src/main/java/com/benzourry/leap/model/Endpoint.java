package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;

@Setter
@Getter
@Entity
@Table(name="ENDPOINT")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Endpoint implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "NAME")
    String name;

    @Column(name = "CODE")
    String code;

    @Column(name = "DESCRIPTION", length = 4000)
    String description;

    @Column(name = "EMAIL")
    String email;

    @Column(name = "RESPONSE_TYPE")
    String responseType; // json|jsonp

    @Column(name = "HEADERS")
    String headers; // json|jsonp

    @Column(name = "METHOD")
    String method; // json|jsonp

    @Column(name="URL")
    String url;

    @Column(name = "JSON_ROOT", length = 2500)
    String jsonRoot;

    @JoinColumn(name = "APP", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    App app;

    @Column(name = "AUTH")
    boolean auth;

    @Column(name = "AUTH_FLOW")
    String authFlow; //authorization,client_credential

    @Column(name = "SHARED")
    boolean shared;

    @Column(name = "CLIENT_ID")
    String clientId;

    @Column(name = "CLIENT_SECRET")
    String clientSecret;

    @Column(name = "TOKEN_ENDPOINT")
    String tokenEndpoint;

    @Column(name = "TOKEN_TO")
    String tokenTo; //header,url

    @Column(name = "APP",insertable=false, updatable=false)
    Long appId;

}
