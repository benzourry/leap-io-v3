package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Setter
@Getter
@Entity
@Table(name="NOTIFICATION")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Notification implements Serializable{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "EMAIL")
    String email;

    @Column(name = "SENDER")
    String sender;

    @Column(name = "CONTENT", length = 2000)
    String content;

    @Column(name = "URL")
    String url;

    @Column(name = "APP_ID")
    Long appId;

    @Column(name = "TIMESTAMP")
    @Temporal(TemporalType.TIMESTAMP)
    Date timestamp;

    @Column(name = "STATUS")
    String status;
}
