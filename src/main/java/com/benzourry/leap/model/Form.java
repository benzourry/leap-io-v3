package com.benzourry.leap.model;

import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.LongListToStringConverter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;

import java.util.*;

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
    @Convert(converter = LongListToStringConverter.class)
    List<Long> accessList;

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
    @Convert(converter = LongListToStringConverter.class)
    List<Long> addMailer;

    @Column(name = "UPDATE_MAILER")
    @Convert(converter = LongListToStringConverter.class)
    List<Long> updateMailer;

    @Column(name = "RETRACT_MAILER")
    @Convert(converter = LongListToStringConverter.class)
    List<Long> retractMailer;

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

    public String get_f(){
        return Helper.encodeBase64(Helper.optimizeJs(this.f),'@');
    }
    public String get_onSave(){
        return Helper.encodeBase64(Helper.optimizeJs(this.onSave),'@');
    }
    public String get_onSubmit(){
        return Helper.encodeBase64(Helper.optimizeJs(this.onSubmit),'@');
    }
    public String get_onView(){
        return Helper.encodeBase64(Helper.optimizeJs(this.onView),'@');
    }

    public boolean isLive(){
        return app!=null?app.isLive():false;
    }

}
