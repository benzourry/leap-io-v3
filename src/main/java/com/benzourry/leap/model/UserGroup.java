package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.*;

@Setter
@Getter
@Entity
@Table(name="USER_GROUP")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "NAME", length = 250)
    String name;

    @Column(name = "DESCRIPTION",length = 2000)
    String description;

//    @Column(name = "USERS", length = 8000)
//    String users;


    @Column(name = "ALLOW_REG")
    boolean allowReg;

    @Column(name = "NEED_APPROVAL")
    boolean needApproval;


    @JoinColumn(name = "APP", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
    App app;

}
