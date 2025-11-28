package com.benzourry.leap.model;

import com.benzourry.leap.utility.LongListToStringConverter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;

import java.io.Serializable;
import java.util.List;
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

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    private JsonNode x;

    @Column(name = "ACCESS_LIST")
    @Convert(converter = LongListToStringConverter.class)
    List<Long> accessList;

    @Column(name = "SORT_ORDER")
    Long sortOrder;

    @Column(name = "WIDE")
    boolean wide;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "dashboard", orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("dashboard-chart")
    @OrderBy("sortOrder ASC")
    Set<Chart> charts;


    @JoinColumn(name = "APP", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    App app;

    @Column(name = "APP",insertable=false, updatable=false)
    Long appId;

}
