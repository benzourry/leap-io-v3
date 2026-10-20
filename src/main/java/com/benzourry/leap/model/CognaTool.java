package com.benzourry.leap.model;

import com.benzourry.leap.utility.audit.Trailable;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

@Setter
@Getter
@Entity
@Table(name="COGNA_TOOL")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CognaTool implements Trailable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "NAME")
    String name;

    @Column(name = "DESCRIPTION", length = 5000, columnDefinition = "text")
    String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    JsonNode params;

    @Column(name = "LAMBDA_ID")
    Long lambdaId; //10025 -- id of the component

    @Column(name = "ENABLED")
    boolean enabled;

    @JoinColumn(name = "COGNA", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonBackReference("cogna-tool")
    @OnDelete(action = OnDeleteAction.CASCADE)
    Cogna cogna;


    @Column(name = "COGNA",insertable=false, updatable=false)
    Long cognaId;


    @Override
    public Long getTrailId() {
        return this.id;
    }

    @Override
    public String getTrailModule() {
        return "cogna_tool";
    }

    @Override
    public String getTrailScope() {

        Long safeCognaId = this.cognaId != null ? this.cognaId : (this.cogna != null ? this.cogna.getId() : null);

        return "cogna_"+ safeCognaId + "_tool_" + this.id;
    }

    @Override
    public Long getTrailAppId() {
        // If form is a corrupted proxy, this safely avoids triggering the EntityNotFoundException
        if (this.cogna == null) return null;
        try {
            return this.cogna.getAppId();
        } catch (Exception e) {
            return null; // Fallback if the parent App is missing
        }
    }


}
