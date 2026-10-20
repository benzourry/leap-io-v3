package com.benzourry.leap.model;

import com.benzourry.leap.utility.audit.Trailable;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Date;

@Setter
@Getter
@Entity
@Table(name="COGNA_SOURCE")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CognaSource extends Schedulable implements Trailable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "NAME")
    String name;

    @Column(name = "TYPE", length = 25)
    String type; //[dataset, entry, user]

    @Column(name = "SRC_ID")
    Long srcId; //10025 -- id of the component

    @Column(name = "APP_ID")
    Long appId; //10025 -- id of the component

    @Column(name = "SRC_URL")
    String srcUrl; //https://www.unimas.my/policy.pdf

    @Column(name = "PARAMS", length = 250)
    String params;

    @Column(name = "SENTENCE_TPL", length = 2000)
    String sentenceTpl;

    @Column(name = "CATEGORY_TPL", length = 2000)
    String categoryTpl;

    @Column(name = "LAST_INGEST")
    Date lastIngest;

    @JoinColumn(name = "COGNA", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonBackReference("cogna-source")
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
        return "cogna_source";
    }

    @Override
    public String getTrailScope() {

        Long safeCognaId = this.cognaId != null ? this.cognaId : (this.cogna != null ? this.cogna.getId() : null);

        return "cogna_"+ safeCognaId + "_source_" + this.id;
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
