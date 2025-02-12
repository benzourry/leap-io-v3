package com.benzourry.leap.model;


import com.benzourry.leap.utility.Helper;
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
import java.util.*;
import java.util.stream.Collectors;

@Setter
@Getter
@Entity
@Table(name="SCREEN")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Screen extends BaseEntity implements Serializable{


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "TITLE")
    String title;

    @Column(name = "DESCRIPTION", length = 4000)
    String description;

    @Column(name = "TYPE")
    String type; // [qr,search,view,form,list,]

//    @JoinColumn(name = "ACCESS", referencedColumnName = "ID")
//    @ManyToOne
//    @NotFound(action = NotFoundAction.IGNORE)
//    @OnDelete(action = OnDeleteAction.NO_ACTION)
//    UserGroup access;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    JsonNode data;


    @Column(name = "NEXT")
    Long next;

//    @OneToMany(cascade = CascadeType.ALL, mappedBy = "screen", orphanRemoval = true, fetch = FetchType.LAZY)
//    @JsonManagedReference("screen-elements")
//    private Set<Element> elements = new HashSet<>();
//
    @Column(name = "SORT_ORDER")
    Long sortOrder;

    @Column(name = "SHOW_ACTION")
    boolean showAction;


    @Column(name = "CAN_PRINT")
    boolean canPrint;

    @Column(name = "WIDE")
    boolean wide;

    //
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "screen", orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("screen-action")
    @OrderBy("id ASC")
    private Set<Action> actions = new HashSet<>();

    @JoinColumn(name = "FORM", referencedColumnName = "ID")
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    Form form;

    @JoinColumn(name = "DATASET", referencedColumnName = "ID")
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    Dataset dataset;

    @JoinColumn(name = "BUCKET", referencedColumnName = "ID")
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    Bucket bucket;

    @JoinColumn(name = "COGNA", referencedColumnName = "ID")
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    Cogna cogna;

    @JoinColumn(name = "APP", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    App app;

    @Column(name = "APP",insertable=false, updatable=false)
    Long appId;


    @Column(name = "ACCESS_LIST")
    String accessList;



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
