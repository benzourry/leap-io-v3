package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import java.util.Date;

@Setter
@Getter
@Entity
@Table(name="PUSH_SUB")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PushSub extends BaseEntity {
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    Long id;

//    @Column(name="CODE")
//    String code;

    @Id
    @Column(name="ENDPOINT",length=700)
    String endpoint;

    //    @JsonIgnore
    @Column(name = "P256DH")
    String p256dh;

    //    @JsonIgnore
    @Column(name = "AUTH")
    String auth;

    @Column(name = "USER_AGENT")
    String userAgent;

    @Column(name = "APP_ID")
    Long appId;

    @Column(name = "TIMESTAMP")
    Date timestamp;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json", name = "CLIENT")
    private JsonNode client;

    @JoinColumn(name = "USER", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
    User user;
}
