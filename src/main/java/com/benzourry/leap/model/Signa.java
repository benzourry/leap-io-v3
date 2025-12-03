package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "signa")
@Setter
@Getter
public class Signa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String location;

    private String reason;

    private String imagePath;

    private String email;

    private String keyPath;

    private String hashAlg;
    private String keystoreType;

    private String password;

    private Boolean showStamp;

    private float stampLlx;

    private float stampLly;

    private float stampUrx;

    private float stampUry;

    @JsonIgnore
    @JoinColumn(name = "APP", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    App app;

    @Column(name = "APP",insertable=false, updatable=false)
    Long appId;

}