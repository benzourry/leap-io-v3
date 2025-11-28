package com.benzourry.leap.model;

import com.benzourry.leap.utility.audit.AuditableEntity;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;

import java.util.Date;

/**
 * Created by MohdRazif on 1/9/2017.
 */
@Entity
@Setter
@Getter
@Table(name = "ENTRY_APPROVAL")
@JsonInclude(JsonInclude.Include.NON_NULL)
// Mn enabled soft-delete ctok, entry xpat delete - no centry entity with id ??? exist
@SQLDelete(sql = "UPDATE entry_approval SET deleted = true WHERE id = ?") //hibernate specific
@Loader(namedQuery = "findEntryApprovalById")
@NamedQuery(
        name = "findEntryApprovalById",
        query = "select ea from EntryApproval ea where ea.deleted = false and ea.id = ?1"
)
@Where(clause = "deleted = false") //hibernate specific
public class EntryApproval extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    private JsonNode data;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    private JsonNode list;

    @JoinColumn(name = "TIER", referencedColumnName = "ID")
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Tier tier;

    @Column(name="TIER_ID")
    private Long tierId;

    @Column(name="STATUS")
    private String status;

    @Column(name = "REMARK", length = 5000, columnDefinition = "text")
    private String remark;

    @Column(name = "TIMESTAMP")
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    @JoinColumn(name = "APPROVER")
    @ManyToOne(fetch = FetchType.LAZY)
    @NotFound(action = NotFoundAction.IGNORE)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    private User approver;

    @JoinColumn(name = "EMAIL")
//    @ManyToOne
    private String email;

    @JoinColumn(name = "ENTRY", referencedColumnName = "ID")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JsonBackReference("entry-appr")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Entry entry;

    @Column(name = "ENTRY", insertable = false, updatable = false)
    private Long entryId;  // scalar field



}
