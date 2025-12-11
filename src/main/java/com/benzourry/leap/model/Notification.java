package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
@Entity
@Table(name="NOTIFICATION")
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Notification implements Serializable{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "EMAIL", length = 5000, columnDefinition = "text")
    String email;

    @Column(name = "SENDER")
    String sender;

    @Column(name = "INIT_BY")
    String initBy;

    @Column(name = "SUBJECT", length = 5000, columnDefinition = "text")
    String subject;

    @Column(name = "CONTENT", length = 5000, columnDefinition = "text")
    String content;

    @Column(name = "URL")
    String url;

    @Column(name = "APP_ID")
    Long appId;

    @Column(name = "TPL_ID")
    Long emailTemplateId;

    @Column(name = "TIMESTAMP")
    @Temporal(TemporalType.TIMESTAMP)
    Date timestamp;

    @Column(name = "STATUS")
    String status;


    @Column(name = "ENTRY_ID")
    private Long entryId;

//    @Column(name = "READ")
//    String read;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String,Object> receipt= new HashMap<>(); // {"facet":"mode"} xjd pake, dlm x jk



}
