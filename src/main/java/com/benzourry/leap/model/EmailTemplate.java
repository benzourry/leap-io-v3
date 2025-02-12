/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Map;

/**
 *
 * @author MohdRazif
 */
@Entity
@Table(name = "EMAIL_TEMPLATE")
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

//    @Basic(optional = false)
//    @Column(name = "CODE", length=25)
//    private String code;

    @Basic(optional = false)
    @Column(name = "CONTENT", length=4000)
    private String content;

    @Basic(optional = false)
    @Column(name = "ENABLED")
    private Integer enabled;


    @Column(name = "DESCRIPTION", length=1000)
    private String description;

    @Basic(optional = false)
    @Column(name = "SUBJECT", length=1000)
    private String subject;

    @Basic(optional = false)
    @Column(name = "NAME", length=255)
    private String name;

    @Column(name = "CREATOR")
    private String creator;


    @Column(name = "SHARED")
    boolean shared;

    @Column(name = "PICKABLE")
    boolean pickable;

    @Column(name = "PUSHABLE")
    boolean pushable;

    @Column(name = "PUSH_URL")
    private String pushUrl;

    @Column(name = "TO_USER")
    boolean toUser;

    @Column(name = "TO_APPROVER")
    boolean toApprover;

    @Column(name = "TO_ADMIN")
    boolean toAdmin;

    @Column(name = "LOG")
    boolean log;

    @Column(name = "TO_EXTRA")
    String toExtra;

    @Column(name = "CC_USER")
    boolean ccUser;

    @Column(name = "CC_APPROVER")
    boolean ccApprover;

    @Column(name = "CC_ADMIN")
    boolean ccAdmin;

    @Column(name = "CC_EXTRA")
    String ccExtra;


    @JoinColumn(name = "APP", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    App app;


    public EmailTemplate() {
    }

    public EmailTemplate(Map params){
        this.setContent(params.get("content")+"");
        this.setSubject(params.get("subject")+"");
    }

    public EmailTemplate(Long id) {
        this.id = id;
    }

    public EmailTemplate(Long id, String content, String subject, String name) {
        this.id = id;
//        this.activeFlag = activeFlag;
        this.content = content;
//        this.dateCreated = dateCreated;
        this.subject = subject;
        this.name = name;
       // this.irisOprid = irisOprid;
    }

}
