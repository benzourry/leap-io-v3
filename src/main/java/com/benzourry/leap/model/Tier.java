package com.benzourry.leap.model;

import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.LongListToStringConverter;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Column(name = "APPROVER", length = 5000, columnDefinition = "text")
    String approver; // email

    @Column(name = "APPROVER_GRP")
    Long approverGroup; // group id

    @Column(name = "ORG_MAP")
    String orgMap;

    @Column(name = "ORG_MAP_POINTER")
    String orgMapPointer;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ORG_MAP_PARAM",columnDefinition = "json")
    JsonNode orgMapParam;

    @Column(name = "SUBMIT_MAILER")
    @Convert(converter = LongListToStringConverter.class)
    List<Long> submitMailer;

    @Column(name = "ASSIGN_MAILER")
    @Convert(converter = LongListToStringConverter.class)
    List<Long> assignMailer;

    @Column(name = "RESUBMIT_MAILER")
    @Convert(converter = LongListToStringConverter.class)
    List<Long> resubmitMailer;

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


    public String get_pre(){
        return Helper.encodeBase64(Helper.optimizeJs(this.pre),'@');
    }

    public String get_post(){
        return Helper.encodeBase64(Helper.optimizeJs(this.post),'@');
    }


}
