package com.benzourry.leap.model;

import com.benzourry.leap.utility.Helper;
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
import java.util.*;
import java.util.stream.Collectors;

@Setter
@Getter
@Entity
@Table(name="FORM")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Form extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "TITLE", length = 1000)
    String title;

    @Column(name = "DESCRIPTION", length = 2000)
    String description;

    @JoinColumn(name = "ADMIN", referencedColumnName = "ID")
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    UserGroup admin;

    @Column(name = "ACCESS_LIST")
    String accessList;

    @Column(name = "NAV", length = 50)
    String nav; // tabbed, accordian

    @Column(name = "ICON")
    String icon;

    @Column(name = "ALIGN")
    String align;

    @Column(name = "CODE_FORMAT")
    String codeFormat;

    @Column(name = "F", length = 5000, columnDefinition = "text")
    String f;

    @Column(name = "ON_SAVE", length = 5000, columnDefinition = "text")
    String onSave;

    @Column(name = "ON_SUBMIT", length = 5000, columnDefinition = "text")
    String onSubmit;

    @Column(name = "ON_VIEW", length = 5000, columnDefinition = "text")
    String onView;

    @Column(name = "PUBLIC_EP")
    boolean publicEp;

    @Column(name = "HIDE_STATUS")
    boolean hideStatus;

    @Column(name = "SINGLE")
    boolean single;


    @Column(name = "SINGLE_Q")
    String singleQ;

    @JoinColumn(name = "PREV", referencedColumnName = "ID")
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
//    @JsonBackReference("form-prev")
    Form prev;

    @Column(name = "START_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startDate;

    @Column(name = "END_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endDate;


    @Column(name = "INACTIVE")
    boolean inactive;

    @Column(name = "VALIDATE_SAVE")
    boolean validateSave;

    @Column(name = "CAN_SAVE")
    boolean canSave;

    @Column(name = "CAN_EDIT")
    boolean canEdit;

    @Column(name = "CAN_RETRACT")
    boolean canRetract;

    @Column(name = "CAN_SUBMIT")
    boolean canSubmit;

    @Column(name = "SHOW_INDEX")
    boolean showIndex; // untuk index accordion / tabpane


    @OneToMany(cascade = CascadeType.ALL, mappedBy = "form", orphanRemoval = true, fetch = FetchType.LAZY)
    @MapKeyColumn(name = "code")
    @JsonManagedReference("form-items")
    private Map<String, Item> items = new HashMap<>();

//    @OneToMany(cascade = CascadeType.ALL, mappedBy = "form", orphanRemoval = true, fetch = FetchType.LAZY)
//    @JsonManagedReference("form-elements")
//    private Set<Element> elements = new HashSet<>();

//    @JsonIgnore
//    @OneToMany(cascade = CascadeType.ALL, mappedBy = "form", orphanRemoval = true, fetch = FetchType.LAZY)
//    @MapKeyColumn(name = "code")
//    @JsonManagedReference("form-models")
//    private Map<String, Model> models = new HashMap<>();

//    @JoinColumn(name = "ELEMENT", referencedColumnName = "ID")
//    @OneToOne
//    private Element element;

//    @OneToMany(cascade = CascadeType.ALL, mappedBy = "form", orphanRemoval = true, fetch = FetchType.LAZY)
//    @MapKeyColumn(name = "id")
//    @JsonManagedReference("form-tab")
//    @OrderBy("sortOrder ASC")
//    private Map<Long, Tab> tabs = new HashMap<>();

//    @OneToMany(cascade = CascadeType.ALL, mappedBy = "form", orphanRemoval = true, fetch = FetchType.LAZY)
//    @JsonManagedReference("form-screen")
//    @OrderBy("sortOrder ASC")
//    List<Screen> screens = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "form", orphanRemoval = true,  fetch = FetchType.LAZY)
    @JsonManagedReference("form-section")
    @OrderBy("sortOrder ASC")
    List<Section> sections = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "form", orphanRemoval = true)
    @JsonManagedReference("form-tier")
    @OrderBy("sortOrder ASC")
    List<Tier> tiers = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "form", orphanRemoval = true)
    @JsonManagedReference("form-tab")
    @OrderBy("sortOrder ASC")
    List<Tab> tabs = new ArrayList<>();

    @Column(name = "COUNTER")
    long counter;

    @Column(name = "ADD_MAILER")
    String addMailer;

    @Column(name = "UPDATE_MAILER")
    String updateMailer;

    @Column(name = "RETRACT_MAILER")
    String retractMailer;

    @Column(name = "UPDATE_APPR_MAILER")
    Long updateApprovalMailer;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    private JsonNode x;

    @JoinColumn(name = "APP", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    App app;

    @Column(name = "APP",insertable=false, updatable=false)
    Long appId;

    @Column(name = "SORT_ORDER")
    Long sortOrder;

    public Form(){}

    public void setAddMailer(List<Long> val){
        this.addMailer = val.stream().map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    public boolean isLive(){
        return app!=null?app.isLive():false;
    }

    public List<Long> getAddMailer(){
        if (!Helper.isNullOrEmpty(this.addMailer)) {
            return Arrays.asList(this.addMailer.split(",")).stream().map(Long::parseLong).collect(Collectors.toList());
        }else{
            return new ArrayList<>();
        }
    }

    public void setUpdateMailer(List<Long> val){
        this.updateMailer = val.stream().map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    public List<Long> getUpdateMailer(){
        if (!Helper.isNullOrEmpty(this.updateMailer)) {
            return Arrays.asList(this.updateMailer.split(",")).stream().map(Long::parseLong).collect(Collectors.toList());
        }else{
            return new ArrayList<>();
        }
    }


    public void setRetractMailer(List<Long> val){
        this.retractMailer = val.stream().map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    public List<Long> getRetractMailer(){
        if (!Helper.isNullOrEmpty(this.retractMailer)) {
            return Arrays.asList(this.retractMailer.split(",")).stream().map(Long::parseLong).collect(Collectors.toList());
        }else{
            return new ArrayList<>();
        }
    }


    public void setAccessList(List<Long> val){
        this.accessList = val.stream().map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    public List<Long> getAccessList(){
        if (!Helper.isNullOrEmpty(this.accessList)) {
            return Arrays.asList(this.accessList.split(",")).stream().map(Long::parseLong).collect(Collectors.toList());
        }else{
            return new ArrayList<>();
        }
    }

}
