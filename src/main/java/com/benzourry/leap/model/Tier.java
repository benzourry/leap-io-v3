package com.benzourry.leap.model;

import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;

import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Setter
@Getter
@Entity
@Table(name="TIER")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Tier extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "NAME", length = 4000)
    String name;

    @Column(name = "SORT_ORDER")
    Long sortOrder;

    @Column(name = "TYPE")
    String type;

    @Column(name = "APPROVER")
    String approver; // email

    @Column(name = "APPROVER_GRP")
    Long approverGroup; // group id

    @Column(name = "ORG_MAP")
    String orgMap;

    @Column(name = "ORG_MAP_POINTER")
    String orgMapPointer;

    @Type(value = JsonType.class)
    @Column(name = "ORG_MAP_PARAM",columnDefinition = "json")
    JsonNode orgMapParam;

    @Column(name = "SUBMIT_MAILER")
    String submitMailer;

    @Column(name = "ASSIGN_MAILER")
    String assignMailer;

    @Column(name = "RESUBMIT_MAILER")
    String resubmitMailer;

    @Column(name = "CAN_REMARK")
    boolean canRemark;

    @Column(name = "ALWAYS_APPROVE")
    boolean alwaysApprove;

    @Column(name = "SHOW_APPROVER")
    boolean showApprover;

    @Column(name = "PRE", length = 2000)
    String pre;

    @Column(name = "POST", length = 2000)
    String post;

    @JoinColumn(name = "ASSIGNER", referencedColumnName = "ID")
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    UserGroup assigner;

    @JoinColumn(name = "FORM_SECTION", referencedColumnName = "ID")
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    Section section;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "tier", orphanRemoval = true, fetch = FetchType.LAZY)
    @MapKeyColumn(name = "code")
    @JsonManagedReference("tier-actions")
    @OrderBy("sortOrder ASC")
    private Map<String, TierAction> actions = new HashMap<>();



    @JoinColumn(name = "FORM", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonBackReference("form-tier")
    @OnDelete(action = OnDeleteAction.CASCADE)
    Form form;

    public static final String TYPE_HOD = "HOD",
            TYPE_PERSON = "PERSON",
            TYPE_GROUP = "GROUP";


    public void setSubmitMailer(List<Long> val){
        this.submitMailer = val.stream().map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    public List<Long> getSubmitMailer(){
        if (!Helper.isNullOrEmpty(this.submitMailer)) {
            return Arrays.asList(this.submitMailer.split(",")).stream().map(Long::parseLong).collect(Collectors.toList());
        }else{
            return new ArrayList<>();
        }
    }

    public void setResubmitMailer(List<Long> val){
        this.resubmitMailer = val.stream().map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    public List<Long> getResubmitMailer(){
        if (!Helper.isNullOrEmpty(this.resubmitMailer)) {
            return Arrays.asList(this.resubmitMailer.split(",")).stream().map(Long::parseLong).collect(Collectors.toList());
        }else{
            return new ArrayList<>();
        }
    }

    public void setAssignMailer(List<Long> val){
        this.assignMailer = val.stream().map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    public List<Long> getAssignMailer(){
        if (!Helper.isNullOrEmpty(this.assignMailer)) {
            return Arrays.asList(this.assignMailer.split(",")).stream().map(Long::parseLong).collect(Collectors.toList());
        }else{
            return new ArrayList<>();
        }
    }



}
