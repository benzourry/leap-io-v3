package com.benzourry.leap.model;

import com.benzourry.leap.service.MailService;
import com.benzourry.leap.utility.audit.AuditableEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.NamedQuery;
import lombok.Getter;
import lombok.Setter;
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
@Table(name = "ENTRY")
@JsonInclude(JsonInclude.Include.NON_NULL)
@SQLDelete(sql = "UPDATE ENTRY SET deleted = true WHERE id = ?") //hibernate specific
@Loader(namedQuery = "findEntryById")
@NamedQuery(
        name = "findEntryById",
        query = "select e from Entry e where e.deleted = false and e.id = ?1"
)
@Where(clause = "deleted = false") //hibernate specific
public class Entry extends AuditableEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

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

    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
//    @MapKeyColumn(name = "approval_keys")
    @MapKey(name="tierId")
    @JsonManagedReference("entry-appr")
    private Map<Long, EntryApproval> approval = new HashMap<Long, EntryApproval>();


    @JoinColumn(name = "FORM", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    Form form;


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

//    @Column(name = "CREATED_DATE", updatable = false)
//    @Temporal(TemporalType.TIMESTAMP)
//    @CreatedDate
//    private Date createdDate;
//
//    @Column(name = "MODIFIED_DATE", updatable = false)
//    @Temporal(TemporalType.TIMESTAMP)
//    @LastModifiedDate
//    private Date modifiedDate;
//
//    @Column(name = "CREATED_BY")
//    @CreatedBy
//    private String createdBy;
//
//    @Column(name = "MODIFIED_BY")
//    @LastModifiedBy
//    private String modifiedBy;

    @Column(name = "EMAIL")
    String email;

    @ElementCollection
    @CollectionTable(
            name="ENTRY_APPROVER",
            joinColumns=@JoinColumn(name="ENTRY_ID")
    )
    @Column(name="APPROVER")
    @MapKeyColumn(name="TIER_ID")
    @JoinColumn(name = "ENTRY_ID")
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
            STATUS_DRAFTED = "drafted";

    public Entry(){}

    public Entry(Long id, JsonNode data){
        this.id = id;
        this.data = data;
    }

    public Entry(JsonNode data){
        this.data = data;
    }


    public JsonNode getPrev(){
        if (this.prevEntry!=null){
            ObjectMapper mapper = new ObjectMapper();
            Map<String,Object> m1 = mapper.convertValue(this.prevEntry.getData(), Map.class);
            if (this.prevEntry.prevEntry!=null){
                m1.put("$prev$",this.prevEntry.prevEntry.getData());
            }
            return mapper.valueToTree(m1);
        }else{
            return null;
        }
    }

    @Override
    public String toString() {
        return "id:"+id+",data:"+ data;
    }

//    @PostLoad
//    private void saveCode(){
//
//        JsonNode node = this.getData();
//        ObjectNode o = (ObjectNode) node;
//        this.savedCode = o.get("$code").asText();
//        System.out.println("saveCode():"+this.savedCode);
//    }
//    @PrePersist
    @PreUpdate
    public void prePersist() {
        JsonNode node = this.getData();
        ObjectNode o = (ObjectNode) node;
//        System.out.println("prePersist "+ o);
//        if (o.get("$counter")==null){
//            if (this.getForm().getCodeFormat()!=null && !this.getForm().getCodeFormat().isEmpty()){
//                o.put("$code",String.format(this.getForm().getCodeFormat(), o.get("$counter")!=null?o.get("$counter").asLong(0):0));
//            }else{
//                o.put("$code",String.valueOf(o.get("$counter")!=null?o.get("$counter").asLong(0):0));
//            }            //get old value
//        }

        if (this.getForm().getCodeFormat()!=null && !this.getForm().getCodeFormat().isEmpty()){
            String codeFormat = this.getForm().getCodeFormat();
            if (codeFormat.contains("{{")){
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> dataMap = new HashMap<>();
                dataMap.put("data", mapper.convertValue(node, HashMap.class));
                dataMap.put("prev", mapper.convertValue(this.getPrev(), HashMap.class));
                codeFormat = MailService.compileTpl(codeFormat, dataMap);
            }
            o.put("$code",String.format(codeFormat, o.get("$counter")!=null?o.get("$counter").asLong(0):0));
        }else{
            o.put("$code",String.valueOf(o.get("$counter")!=null?o.get("$counter").asLong(0):0));
        }            //get old value

        this.setData(o);
    }

    // PostPersist not able to update jsonnode
    @PostPersist
    public void postPersist() {
        JsonNode node = this.getData();
        ObjectNode o = (ObjectNode) node;
//        System.out.println("postPersist "+ o);
        o.put("$id", this.getId());
//        if (this.getForm().getCodeFormat()!=null && !this.getForm().getCodeFormat().isEmpty()){
//            o.put("$code",String.format(this.getForm().getCodeFormat(), this.getForm().getCounter()));
//            o.put("$counter",this.getForm().getCounter());
//        }else{
//            o.put("$code",String.valueOf(this.getForm().getCounter()));
//            o.put("$counter",this.getForm().getCounter());
//        }

        if (this.getForm().getCodeFormat()!=null && !this.getForm().getCodeFormat().isEmpty()){
            String codeFormat = this.getForm().getCodeFormat();
            if (codeFormat.contains("{{")){
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> dataMap = new HashMap<>();
                dataMap.put("data", mapper.convertValue(node, HashMap.class));
                dataMap.put("prev", mapper.convertValue(this.getPrev(), HashMap.class));
                codeFormat = MailService.compileTpl(codeFormat, dataMap);
            }
            o.put("$code",String.format(codeFormat, this.getForm().getCounter()));
            o.put("$counter",this.getForm().getCounter());
        }else{
            o.put("$code",String.valueOf(this.getForm().getCounter()));
            o.put("$counter",this.getForm().getCounter());
        }
        this.setData(o);
    }

}
