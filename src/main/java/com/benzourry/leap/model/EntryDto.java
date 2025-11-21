package com.benzourry.leap.model;

import com.benzourry.leap.utility.audit.AuditableEntity;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
@ToString(exclude = {"prevEntry"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntryDto extends AuditableEntity{

    Long id;

    private JsonNode data;

    private JsonNode prev;

    private Map<Long, EntryApproval> approval = new HashMap<>();

//    Form form;

    Long formId;


    private Integer currentTier;

    private String currentStatus;

    private Long currentTierId;

    private Long finalTierId;

    private Date submissionDate;

    private Date resubmissionDate;

    boolean currentEdit;

    boolean live;

    String email;

    Map<String, String> txHash = new HashMap<>();

    private Map<Long, String> approver = new HashMap<>();


    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode parseJsonOrEmpty(String json) {
        if (json == null || json.isEmpty()) {
            return MAPPER.createObjectNode(); // lightweight empty object
        }
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON: " + json, e);
        }
    }

    private static Map<String, String> parseJsonOrMap(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON: " + json, e);
        }
    }

    public EntryDto(Long id,
                    String dataStr,
                    String prevStr,
//                    String approval,
//                    Form form,
                    Long formId,
                    Integer currentTier,
                    String currentStatus,
                    Long currentTierId,
                    Long finalTierId,
                    Date submissionDate,
                    Date resubmissionDate,
                    boolean currentEdit,
                    Date createdDate,
                    String createdBy,
                    Date modifiedDate,
                    String modifiedBy,
                    boolean live,
                    String email,
                    String txHash
//                    String approver
    ) {
        this.id = id;
        this.data = parseJsonOrEmpty(dataStr);
        this.prev = parseJsonOrEmpty(prevStr);
        this.formId = formId;
        this.currentTier = currentTier;
        this.currentStatus = currentStatus;
        this.currentTierId = currentTierId;
        this.finalTierId = finalTierId;
        this.submissionDate = submissionDate;
        this.resubmissionDate = resubmissionDate;
        this.currentEdit = currentEdit;
        this.setCreatedDate(createdDate);
        this.setCreatedBy(createdBy);
        this.setModifiedDate(modifiedDate);
        this.setModifiedBy(modifiedBy);
        this.live = live;
        this.email = email;
        this.txHash = parseJsonOrMap(txHash);
    }
    public EntryDto(Long id,
                    String dataStr,
                    String prevStr) {
        this.id = id;
        this.data = parseJsonOrEmpty(dataStr);
        this.prev = parseJsonOrEmpty(prevStr);
    }

    public EntryDto(Long id,
                    String dataStr) {
        this.id = id;
        this.data = parseJsonOrEmpty(dataStr);
    }

    public EntryDto() {
    }
}
