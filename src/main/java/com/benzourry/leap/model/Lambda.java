package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;

import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name="LAMBDA")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Lambda extends Schedulable implements Serializable {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "NAME")
    String name;

    @Column(name = "DESCRIPTION", length = 4000)
    String description;

    @Column(name = "LANG")
    String lang; // [js, python]

    @Column(name = "EMAIL")
    String email;

    @JoinColumn(name = "ACCESS", referencedColumnName = "ID")
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    UserGroup access;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    JsonNode data;

//    @Column(name = "SCHEDULED")
//    boolean scheduled;
//
//    @Column(name = "FREQ")
//    String freq; // everyday, everyweek, everymonth;
//
//    @Column(name = "CLOCK")
//    String clock; //everyday:0101

    @Column(name = "CODE")
    String code; //unique

//    @Column(name = "DAY_OF_WEEK")
//    Integer dayOfWeek; //everyweek:1,2,3,4,5,6,7
//
//    @Column(name = "DAY_OF_MONTH")
//    Integer dayOfMonth; // 25
//
//    @Column(name = "MONTH_OF_YEAR")
//    Integer monthOfYear; // 2

    @Column(name = "PUBLIC_ACCESS")
    boolean publicAccess;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "lambda", orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("lambda-bind")
    @OrderBy("id ASC")
    private Set<LambdaBind> binds = new HashSet<>();

    @JoinColumn(name = "APP", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    App app;
}
