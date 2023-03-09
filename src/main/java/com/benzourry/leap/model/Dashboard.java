package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name="DASHBOARD")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Dashboard implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "TITLE")
    String title;

    @Column(name = "SIZE")
    String size;

    @Column(name = "CODE", length = 2000)
    String code;

    @Column(name = "TYPE")
    String type;

    @Column(name = "DESCRIPTION")
    String description;

//    @Column(name = "ADMIN")
//    String admin;

    @JoinColumn(name = "ACCESS", referencedColumnName = "ID")
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    UserGroup access;

    @Column(name = "SORT_ORDER")
    Long sortOrder;

    @Column(name = "WIDE")
    boolean wide;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "dashboard", orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("dashboard-chart")
    @OrderBy("sortOrder ASC")
    Set<Chart> charts;

//    @JoinColumn(name = "FORM", referencedColumnName = "ID")
//    @ManyToOne(optional = false)
////    @JsonBackReference("form-dashboard")
//    Form form;

    @JoinColumn(name = "APP", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    App app;

}
