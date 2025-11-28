package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name="CLONE_REQUEST")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloneRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "NAME", length = 4000)
    String name;

    @Column(name = "EMAIL")
    String email;

    @Column(name = "CATEGORY")
    String category;

    @Column(name = "TYPE", length = 50)
    String type; // user, creator

    @Column(name = "ACTIVATED")
    boolean activated;

    @Column(name = "STATUS", length = 50)
    String status; // pending, activated, rejected

    @Column(name = "FIRST_LOGIN")
    @Temporal(TemporalType.TIMESTAMP)
    private Date firstLogin;

    @Column(name = "LAST_LOGIN")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastLogin;

    @JoinColumn(name = "APP", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)

//    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    App app;

    public CloneRequest(){}
}
