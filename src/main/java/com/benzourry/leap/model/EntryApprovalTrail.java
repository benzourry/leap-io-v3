package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

import java.util.Date;

@Entity
@Setter
@Getter
@Table(name = "ENTRY_APPROVAL_TRAIL", indexes = {
        @Index(name = "idx_entry_trail_entry_id", columnList = "ENTRY_ID"),
        @Index(name = "idx_entry_trail_snap_status", columnList = "STATUS")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntryApprovalTrail extends BaseEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private JsonNode data;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private JsonNode snap;

    @JoinColumn(name = "TIER", referencedColumnName = "ID")
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Tier tier;

    @Column(name="STATUS")
    private String status;

    @Column(name = "REMARK", length = 5000, columnDefinition = "text")
    private String remark;

    @Column(name = "TIMESTAMP")
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    @JoinColumn(name = "EMAIL")
    private String email;

    @Column(name = "ENTRY_ID")
    private Long entryId;

    public final static String DELETE = "delete";
//    public final static String SAVE = "save";


    public EntryApprovalTrail(){}

    public EntryApprovalTrail(EntryApproval ea){
        try{
            this.setEntryId(ea.getEntry().getId());
            this.setEmail(ea.getEmail());
            this.setStatus(ea.getStatus());
            this.setRemark(ea.getRemark());
            this.setTimestamp(new Date());
            this.setTier(ea.getTier());
            this.setData(ea.getData());
        }catch (Exception e){}

    }

    public EntryApprovalTrail(JsonNode data, Tier tier, String status, String remark, String email, Long entryId) {
//        this.id = id;
        this.data = data;
        this.tier = tier;
        this.status = status;
        this.remark = remark;
        this.timestamp = new Date();
        this.email = email;
        this.entryId = entryId;
    }
}
