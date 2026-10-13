package com.benzourry.leap.model;

import com.benzourry.leap.utility.audit.AuditableMetaEntity;
import com.benzourry.leap.utility.audit.Trailable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "krypta_wallet")
@Setter
@Getter
public class KryptaWallet extends AuditableMetaEntity implements Trailable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // network info
    private String rpcUrl;
    private Long chainId; //1337L for dev

    // contract info
    private String contractAddress;

    // wallet info
    private String name;
    private String description;
//    private String address;
//    private String password;
    private String privateKey; // store encrypted value
    private Long userId;
    // getters & setters

    private String email;

    @JoinColumn(name = "CONTRACT_ID", referencedColumnName = "ID")
    @NotFound(action = NotFoundAction.IGNORE)
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private KryptaContract contract;

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
        return "kryptaWallet";
    }

    @Override
    public String getTrailScope() {
        return "kryptaWallet_" + this.id;
    }


    @Override
    public Long getTrailAppId() {
        return this.app != null ? this.app.getId() : this.appId;
    }

}
