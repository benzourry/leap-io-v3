package com.benzourry.leap.model;

import com.benzourry.leap.utility.audit.Trailable;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Setter
@Getter
@Entity
@Table(name="COGNA_SUB", indexes = {
        @Index(name = "idx_cogna_sub_app_id", columnList = "APP_ID"),
        @Index(name = "idx_cogna_sub_sub_id", columnList = "SUB_ID")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CognaSub implements Trailable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "NAME")
    String name;

    @Column(name = "DESCRIPTION")
    String description;

    @Column(name = "APP_ID")
    Long appId;

    @Column(name = "SUB_ID")
    Long subId;

    @Column(name = "ENABLED")
    boolean enabled;

    @JoinColumn(name = "COGNA", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonBackReference("cogna-sub")
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
        return "cogna_sub";
    }

    @Override
    public String getTrailScope() {

        Long safeCognaId = this.cognaId != null ? this.cognaId : (this.cogna != null ? this.cogna.getId() : null);

        return "cogna_"+ safeCognaId + "_sub_" + this.id;
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
