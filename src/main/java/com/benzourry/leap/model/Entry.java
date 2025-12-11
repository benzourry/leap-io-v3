package com.benzourry.leap.model;

import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.audit.AuditableEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Index;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Entity
@Setter
@Getter
@Table(name = "ENTRY", indexes = {
        @Index(name = "idx_entry_form", columnList = "FORM"),
        @Index(name = "idx_entry_current_tier", columnList = "CURRENT_TIER"),
        @Index(name = "idx_entry_current_status", columnList = "CURRENT_STATUS"),
        @Index(name = "idx_entry_current_tier_id", columnList = "CURRENT_TIER_ID"),
        @Index(name = "idx_entry_live", columnList = "LIVE"),
        @Index(name = "idx_entry_email", columnList = "EMAIL"),
        @Index(name = "idx_entry_deleted", columnList = "DELETED"),

})
@JsonInclude(JsonInclude.Include.NON_NULL)
@SQLDelete(sql = "UPDATE entry SET deleted = true WHERE id = ?") //hibernate specific
@Loader(namedQuery = "findEntryById")
@NamedQuery(
        name = "findEntryById",
        query = "select e from Entry e where e.deleted = false and e.id = ?1"
)
@Where(clause = "deleted = false") //hibernate specific
@ToString(exclude = {"prevEntry"})
public class Entry extends AuditableEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

//    @Column(name = "CODE")
//    String code;
//
//    @Column(name = "COUNTER")
//    Long counter;

//    @JdbcTypeCode(SqlTypes.JSON)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private JsonNode data;

    @JoinColumn(name = "PREV_ENTRY", referencedColumnName = "ID")
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Entry prevEntry;

    // TEST: Test if it is OK. If not, revert back to LAZY
    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
//    @MapKeyColumn(name = "approval_keys")
    @MapKey(name="tierId")
    @JsonManagedReference("entry-appr")
    private Map<Long, EntryApproval> approval = new HashMap<Long, EntryApproval>();

    @JoinColumn(name = "FORM", referencedColumnName = "ID")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    Form form;

    @Column(name = "FORM", updatable = false, insertable = false)
    Long formId;

    @Column(name = "CURRENT_TIER")
    private Integer currentTier;

    @Column(name = "CURRENT_STATUS")
    private String currentStatus;

    @Column(name = "CURRENT_TIER_ID")
    private Long currentTierId;

    @Column(name = "FINAL_TIER_ID")
    private Long finalTierId;

    @Column(name = "SUBMISSION_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date submissionDate;

    @Column(name = "RESUBMISSION_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date resubmissionDate;

    @Column(name = "CURRENT_EDIT")
    boolean currentEdit;

    @Column(name = "LIVE")
    boolean live;

    @Column(name = "EMAIL")
    String email;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    Map<String, String> txHash = new HashMap<>();

    @ElementCollection
    @CollectionTable(
            name="ENTRY_APPROVER",
            joinColumns=@JoinColumn(name="ENTRY_ID")
    )
    @Column(name="APPROVER", length = 5000, columnDefinition = "text")
    @MapKeyColumn(name="TIER_ID")
//    @OnDelete(action= OnDeleteAction.CASCADE)
    private Map<Long, String> approver = new HashMap<>();

    public static final String
            STATUS_SUBMITTED = "submitted",
            STATUS_RESUBMITTED = "resubmitted",
            STATUS_DRAFTED = "drafted",
            STATUS_ALWAYS_APPROVE = "always_approve";

    public Entry(){}

    public Entry(Long id, JsonNode data){
        this.id = id;
        this.data = data;
    }

    public Entry(JsonNode data){
        this.data = data;
    }

    @Transient
    @JsonIgnore
    private JsonNode cachedPrev;

    private static final int MAX_PREV_DEPTH = 2; // limit recursion depth to 2

    public JsonNode getPrev() {
        if (cachedPrev == null) {
            cachedPrev = buildPrevNode(this.prevEntry, 0);
        }
        return cachedPrev;
    }

    @JsonIgnore
    private JsonNode buildPrevNode(Entry entry, int depth) {
        if (entry == null || depth >= MAX_PREV_DEPTH) {
            return null;
        }
        // Convert the current entry's data to a map
        Map<String, Object> map = Helper.MAPPER.convertValue(entry.getData(), Map.class);

        // If there is a previous entry and depth < limit, attach minimal info about it
        if (entry.getPrevEntry() != null && depth + 1 < MAX_PREV_DEPTH) {
            map.put("$prev$", buildPrevNode(entry.getPrevEntry(), depth + 1));
        }

        return Helper.MAPPER.valueToTree(map);
    }

    @PrePersist
    public void prePersist(){
        if (!this.live){ // new && live==false/null
            this.live = this.getForm().getApp().isLive();
        }
    }

//    @PreRemove
//    public void preRemove(){
////        System.out.println("prePersist");
////        if (!this.live){ // new && live==false/null
////            this.live = this.getForm().getApp().isLive();
//////            System.out.println("new&&live=false;"+this.live);
////        }
//    }


    // WHAT IS THE USE CASE OF THIS PRE-UPDATE? IS IT TO RETRO $code when formatter change?
    @PreUpdate
    public void preUpdate() {
//        JsonNode node = this.getData();
        ObjectNode newData = data.deepCopy();
        newData.put("$id", this.getId()); // 30/7/2025 to ensure $id is there
        // getId() only attainable on PostPersist
        // create $code only if null
        if (newData.get("$code")==null){
            if (this.getForm().getCodeFormat()!=null && !this.getForm().getCodeFormat().isEmpty()){
                String codeFormat = this.getForm().getCodeFormat();
                if (codeFormat.contains("{{")){
                    Map<String, Object> dataMap = new HashMap<>();
                    dataMap.put("data", Helper.MAPPER.convertValue(newData, Map.class));
                    dataMap.put("prev", Helper.MAPPER.convertValue(this.getPrev(), Map.class));
                    codeFormat = Helper.compileTpl(codeFormat, dataMap);
                }
                newData.put("$code",String.format(codeFormat, newData.get("$counter")!=null?newData.get("$counter").asLong(0):0));
            }else{
                newData.put("$code",String.valueOf(newData.get("$counter")!=null?newData.get("$counter").asLong(0):0));
            }

        }

        this.data = newData;
    }

    // PostPersist not able to update jsonnode
    @PostPersist
    public void postPersist() {
        ObjectNode newData = data.deepCopy();
        newData.put("$id", this.getId());

        // create $code only if null
        if (newData.get("$code")==null){
            if (this.getForm().getCodeFormat()!=null && !this.getForm().getCodeFormat().isEmpty()){
                String codeFormat = this.getForm().getCodeFormat();
                if (codeFormat.contains("{{")){
                    Map<String, Object> dataMap = new HashMap<>();
                    dataMap.put("data", Helper.MAPPER.convertValue(newData, Map.class));
                    dataMap.put("prev", Helper.MAPPER.convertValue(this.getPrev(), Map.class));
                    codeFormat = Helper.compileTpl(codeFormat, dataMap);
                }
                newData.put("$code",String.format(codeFormat, this.getForm().getCounter()));
                newData.put("$counter",this.getForm().getCounter());
            }else{
                newData.put("$code",String.valueOf(this.getForm().getCounter()));
                newData.put("$counter",this.getForm().getCounter());
            }
        }

        this.data = newData;
    }

}
