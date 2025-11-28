package com.benzourry.leap.model;

import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.annotations.Type;

import java.util.Date;
import java.util.Map;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
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


    public static User anonymous(){
        String random = RandomStringUtils.randomAlphanumeric(6);
        return new User(0l,"Guest","anonymous-"+random,"assets/img/avatar-big.png",true,null,0l, Helper.MAPPER.valueToTree(Map.of("name","Anonymous")),new Date(), new Date(),AuthProvider.local,"anonymous","approved",null, true);
    }
}
