package com.benzourry.leap.model;

import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Setter
@Getter
@Entity
@Table(name="NAVI_GROUP")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NaviGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String title;

//    @JoinColumn(name = "ACCESS", referencedColumnName = "ID")
//    @ManyToOne
//    @NotFound(action = NotFoundAction.IGNORE)
//    @OnDelete(action = OnDeleteAction.NO_ACTION)
//    UserGroup access;


    @Column(name = "ACCESS_LIST")
    String accessList;


    @OneToMany(cascade = CascadeType.ALL, mappedBy = "group", orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("group-item")
    @OrderBy("sortOrder ASC")
    List<NaviItem> items;

    @Column(name = "SORT_ORDER")
    Long sortOrder;

    @Column(name = "PRE", length = 2000)
    String pre;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    private JsonNode x;

    @JoinColumn(name = "APP", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
//    @JsonBackReference("app-group")
    App app;

    public void setAccessList(List<Long> val){
        this.accessList = val.stream().map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    public List<Long> getAccessList(){
        if (!Helper.isNullOrEmpty(this.accessList)) {
            return Arrays.asList(this.accessList.split(",")).stream().map(Long::parseLong).collect(Collectors.toList());
        }else{
            return new ArrayList<>();
        }
    }
}
