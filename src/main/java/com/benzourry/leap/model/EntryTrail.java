package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Date;

@Entity
@Setter
@Getter
@Table(name = "ENTRY_TRAIL")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntryTrail extends BaseEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    @JsonIgnore
    private JsonNode snap;

    @Column(name = "SNAP_TIER")
    private Integer snapTier;

    @Column(name = "SNAP_TIER_ID")
    private Long snapTierId;

    @Column(name = "SNAP_STATUS")
    private String snapStatus;

    @Column(name = "SNAP_EDIT")
    Boolean snapEdit; // must be nullable

    @Column(name="ACTION")
    private String action; //[save, update, delete]

    @Column(name = "REMARK", length = 2000)
    private String remark;

    @Column(name = "TIMESTAMP")
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    @JoinColumn(name = "EMAIL")
    private String email;


    @Column(name = "FORM_ID")
    private Long formId;

    @Column(name = "ENTRY_ID")
    private Long entryId;

    final public static String CREATED = "created";
    final public static String SAVED = "saved";
    final public static String UPDATED = "updated";
    final public static String REVERTED = "reverted";
    final public static String XUPDATED = "xupdated"; // X field status is to mark this action has been undone
    final public static String REMOVED = "removed";
    final public static String RESTORED = "restored";
    final public static String XREMOVED = "xremoved"; // X field status is to mark this action has been restore
    final public static String RETRACTED = "retracted";
    final public static String APPROVAL = "approval";

    public EntryTrail(){}

    public EntryTrail(Long entryId, JsonNode snap, String email, Long formId, String action, String remark){
        try{
            this.setEntryId(entryId);
            this.setEmail(email);
            this.setAction(action);
            this.setFormId(formId);
            this.setRemark(remark);
            this.setTimestamp(new Date());
            this.setSnap(snap);
        }catch (Exception e){}

    }

    public EntryTrail(Long entryId, JsonNode snap, String email, Long formId, String action, String remark,
                      Integer snapTier, Long snapTierId, String snapStatus, boolean snapEdit){
        try{
            this.setEntryId(entryId);
            this.setEmail(email);
            this.setAction(action);
            this.setFormId(formId);
            this.setRemark(remark);
            this.setTimestamp(new Date());
            this.setSnap(snap);
            this.setSnapTier(snapTier);
            this.setSnapStatus(snapStatus);
            this.setSnapTierId(snapTierId);
            this.setSnapEdit(snapEdit);
        }catch (Exception e){}

    }

}
