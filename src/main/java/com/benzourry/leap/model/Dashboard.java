package com.benzourry.leap.model;

import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

//    @JoinColumn(name = "ACCESS", referencedColumnName = "ID")
//    @ManyToOne
//    @NotFound(action = NotFoundAction.IGNORE)
//    @OnDelete(action = OnDeleteAction.NO_ACTION)
//    UserGroup access;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    private JsonNode x;


    @Column(name = "ACCESS_LIST")
    String accessList;


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

    @Column(name = "APP",insertable=false, updatable=false)
    Long appId;

    public void setAccessList(List<Long> val){
        if (!Helper.isNullOrEmpty(val)) {
            this.accessList = val.stream().map(String::valueOf)
                    .collect(Collectors.joining(","));
        }
    }

    public List<Long> getAccessList(){
        if (!Helper.isNullOrEmpty(this.accessList)) {
            return Arrays.asList(this.accessList.split(",")).stream().map(Long::parseLong).collect(Collectors.toList());
        }else{
            return new ArrayList<>();
        }
    }



}
