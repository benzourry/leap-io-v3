package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.*;
import java.io.Serializable;

@Setter
@Getter
@Entity
@Table(name="LOOKUP")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Lookup implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "NAME")
    String name;

    @Column(name = "DESCRIPTION", length = 4000)
    String description;


    @JoinColumn(name = "ACCESS", referencedColumnName = "ID")
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    UserGroup access;

    @Column(name = "SHARED")
    boolean shared;

    @Column(name = "DATA_ENABLED")
    boolean dataEnabled;

    @Column(name = "EMAIL")
    String email;

    @Column(name = "SOURCE_TYPE")
    String sourceType; // db|rest

    @Column(name = "PROXY_ID")
    Long proxyId;

    @Column(name = "RESPONSE_TYPE")
    String responseType; // json|jsonp

    @Column(name = "HEADERS")
    String headers; // json|jsonp

    @Column(name = "METHOD")
    String method; // get|post

    @Column(name="ENDPOINT")
    String endpoint;

    @Column(name = "CODE_PROP")
    String codeProp;

    @Column(name = "DESC_PROP")
    String descProp;

    @Column(name = "EXTRA_PROP")
    String extraProp;

    @Column(name = "DATA_FIELDS")
    String dataFields;

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

    @Column(name = "CLIENT_ID")
    String clientId;

    @Column(name = "CLIENT_SECRET")
    String clientSecret;

    @Column(name = "TOKEN_ENDPOINT")
    String tokenEndpoint;

    @Column(name = "TOKEN_TO")
    String tokenTo;

}
