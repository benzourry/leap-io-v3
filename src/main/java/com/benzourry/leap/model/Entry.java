package com.benzourry.leap.model;

import com.benzourry.leap.service.MailService;
import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.audit.AuditableEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.Index;
import jakarta.persistence.NamedQuery;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.*;

import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

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
@ToString
public class Entry extends AuditableEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

//    @Column(name = "CODE")
//    String code;
//
//    @Column(name = "COUNTER")
//    Long counter;

    /**
     * Problem using join utk prevEntry
     * - Perlu update frontend
     * - Utk search perlu update predicate
     * -
     */
//    @Type(type = "json")
//    @Column(columnDefinition = "json")
//    private JsonNode prev;

    @Type(value = JsonType.class)
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
    @ManyToOne(optional = false)
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

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    Map<String, String> txHash = new HashMap<>();

    @ElementCollection
    @CollectionTable(
            name="ENTRY_APPROVER",
            joinColumns=@JoinColumn(name="ENTRY_ID")
    )
    @Column(name="APPROVER", length = 5000, columnDefinition = "text")
    @MapKeyColumn(name="TIER_ID")
//    @JoinColumn(name = "ENTRY_ID") // why this is here in the first place???
//    @OnDelete(action= OnDeleteAction.CASCADE)
    private Map<Long, String> approver = new HashMap<>();

//    @Transient
//    private transient String savedCode;

    public static final String
//            STATUS_APPROVED = "approved",
//            STATUS_REJECTED = "rejected",
//            STATUS_CANCELLED = "cancelled",
//            STATUS_RETURNED = "returned",
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

    private static final ObjectMapper MAPPER = new ObjectMapper();
    public JsonNode getPrev(){
        if (this.prevEntry!=null){
//            ObjectMapper mapper = new ObjectMapper();
            Map<String,Object> m1 = MAPPER.convertValue(this.prevEntry.getData(), Map.class);
            if (this.prevEntry.prevEntry!=null){
                m1.put("$prev$",this.prevEntry.prevEntry.getData());
            }
            return MAPPER.valueToTree(m1);
        }else{
            return null;
        }
    }

//    @Override
//    public String toString() {
//        return "id:"+id+",data:"+ data;
//    }

//    @PostLoad
//    private void saveCode(){
//
//        JsonNode node = this.getData();
//        ObjectNode o = (ObjectNode) node;
//        this.savedCode = o.get("$code").asText();
//        System.out.println("saveCode():"+this.savedCode);
//    }
//    @PrePersist

    @PrePersist
    public void prePersist(){
//        System.out.println("prePersist:" + this.getId());
        if (!this.live){ // new && live==false/null
            this.live = this.getForm().getApp().isLive();
//            System.out.println("new&&live=false;"+this.live);
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
        JsonNode node = this.getData();
        ObjectNode o = (ObjectNode) node;
        o.put("$id", this.getId()); // 30/7/2025 to ensure $id is there
        // getId() only attainable on PostPersist
        // create $code only if null
        if (o.get("$code")==null){
            if (this.getForm().getCodeFormat()!=null && !this.getForm().getCodeFormat().isEmpty()){
                String codeFormat = this.getForm().getCodeFormat();
                if (codeFormat.contains("{{")){
//                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> dataMap = new HashMap<>();
                    dataMap.put("data", MAPPER.convertValue(node, HashMap.class));
                    dataMap.put("prev", MAPPER.convertValue(this.getPrev(), HashMap.class));
                    codeFormat = Helper.compileTpl(codeFormat, dataMap);
                }
                o.put("$code",String.format(codeFormat, o.get("$counter")!=null?o.get("$counter").asLong(0):0));
            }else{
                o.put("$code",String.valueOf(o.get("$counter")!=null?o.get("$counter").asLong(0):0));
            }            //get old value

        }

        this.setData(o);
    }

    // PostPersist not able to update jsonnode
    @PostPersist
    public void postPersist() {
        JsonNode node = this.getData();
        ObjectNode o = (ObjectNode) node;
        o.put("$id", this.getId());

        // create $code only if null
        if (o.get("$code")==null){
            if (this.getForm().getCodeFormat()!=null && !this.getForm().getCodeFormat().isEmpty()){
                String codeFormat = this.getForm().getCodeFormat();
                if (codeFormat.contains("{{")){
//                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> dataMap = new HashMap<>();
                    dataMap.put("data", MAPPER.convertValue(node, HashMap.class));
                    dataMap.put("prev", MAPPER.convertValue(this.getPrev(), HashMap.class));
                    codeFormat = Helper.compileTpl(codeFormat, dataMap);
                }
                o.put("$code",String.format(codeFormat, this.getForm().getCounter()));
                o.put("$counter",this.getForm().getCounter());
            }else{
                o.put("$code",String.valueOf(this.getForm().getCounter()));
                o.put("$counter",this.getForm().getCounter());
            }
        }

        this.setData(o);
    }

}
