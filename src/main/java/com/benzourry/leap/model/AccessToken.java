package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

@Setter
@Getter
@Entity
@Table(name="ACCESS_TOKEN")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccessToken {
    @Id
    @Column(name = "PAIR", length = 700)
    String pair;
    @Column(name = "ACCESS_TOKEN")
    String access_token;
    @Column(name = "TOKEN_TYPE")
    String token_type;
    @Column(name = "SCOPE")
    String scope;
    @Column(name = "EXPIRES_IN")
    Long expires_in;
    @Column(name = "EXPIRY_TIME")
    Long expiry_time;
}
