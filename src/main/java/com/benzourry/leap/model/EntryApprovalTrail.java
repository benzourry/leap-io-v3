package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;

import jakarta.persistence.*;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Date;

@Entity
@Setter
@Getter
@Table(name = "ENTRY_APPROVAL_TRAIL")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntryApprovalTrail extends BaseEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    private JsonNode data;

    @JoinColumn(name = "TIER", referencedColumnName = "ID")
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Tier tier;

    @Column(name="STATUS")
    private String status;

    private String remark;

    @Column(name = "TIMESTAMP")
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    @JoinColumn(name = "EMAIL")
//    @ManyToOne
    private String email;

//    @JoinColumn(name = "ENTRY", referencedColumnName = "ID")
//    @ManyToOne(optional = false)
//    @JsonBackReference("entry-appr")
//    private Entry entry;

    @Column(name = "ENTRY_ID")
    private Long entryId;

    public EntryApprovalTrail(){}

    public EntryApprovalTrail(EntryApproval ea){
        try{
            this.setEntryId(ea.getEntry().getId());
            this.setEmail(ea.getEmail());
            this.setStatus(ea.getStatus());
            this.setRemark(ea.getRemark());
            this.setTimestamp(ea.getTimestamp());
            this.setTier(ea.getTier());
            this.setData(ea.getData());
        }catch (Exception e){}

    }

    public EntryApprovalTrail(JsonNode data, Tier tier, String status, String remark, Date timestamp, String email, Long entryId) {
//        this.id = id;
        this.data = data;
        this.tier = tier;
        this.status = status;
        this.remark = remark;
        this.timestamp = timestamp;
        this.email = email;
        this.entryId = entryId;
    }
}
