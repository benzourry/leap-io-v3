package com.benzourry.leap.model;

import com.benzourry.leap.utility.audit.AuditableMetaEntity;
import com.benzourry.leap.utility.audit.Trailable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "krypta_contract")
@Setter
@Getter
public class KryptaContract extends AuditableMetaEntity implements Trailable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "SOL", length = 5000, columnDefinition = "text")
    private String sol;

    private String abi;

    private String bin;

    private String email;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private JsonNode abiSummary;

    @JsonIgnore
    @JoinColumn(name = "APP", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    App app;


    @Column(name = "APP",insertable=false, updatable=false)
    Long appId;

    @Override
    public Long getTrailId() {
        return this.id;
    }

    @Override
    public String getTrailModule() {
        return "kryptaContract";
    }

    @Override
    public String getTrailScope() {
        return "kryptaContract_" + this.id;
    }


    @Override
    public Long getTrailAppId() {
        return this.app != null ? this.app.getId() : this.appId;
    }

}
