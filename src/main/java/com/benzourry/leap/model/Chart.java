package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name="CHART")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Chart extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "TITLE")
    String title;

    @Column(name = "SIZE")
    String size;

    @Column(name = "HEIGHT")
    String height;

    @Column(name = "TYPE")
    String type; //[bar, pie, doughnat, line]

    @Column(name = "DESCRIPTION")
    String description;

    @Column(name = "ROOT_CODE")
    String rootCode;

    @Column(name = "FIELD_CODE", length = 5000, columnDefinition = "text")
    String fieldCode;

    @Column(name = "ROOT_VALUE")
    String rootValue;

    @Column(name = "FIELD_VALUE")
    String fieldValue;

    @Column(name = "ROOT_SERIES")
    String rootSeries;

    @Column(name = "FIELD_SERIES")
    String fieldSeries;


    @Column(name = "SERIES")
    boolean series;

    @Column(name = "AGG")
    String agg;

//    @Column(name = "UI_TEMPLATE")
//    String uiTemplate;

    @Column(name = "SORT_ORDER")
    Long sortOrder;

    @Column(name = "CAN_VIEW")
    boolean canView;

    @Column(name = "SHOW_AGG")
    boolean showAgg;

    @Column(name = "STATUS")
    String status;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json", name="STATUS_FILTER")
    JsonNode statusFilter;



    @OneToMany(cascade = CascadeType.ALL, mappedBy = "chart", orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("chart-filter")
    @OrderBy("sortOrder ASC")
    Set<ChartFilter> filters;


    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    JsonNode presetFilters;

    @JoinColumn(name = "DASHBOARD", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonBackReference("dashboard-chart")
    @OnDelete(action = OnDeleteAction.CASCADE)
    Dashboard dashboard;

    @JoinColumn(name = "FORM", referencedColumnName = "ID")
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    Form form;

    @Column(name = "FORM",insertable=false, updatable=false)
    Long formId;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "json")
    private JsonNode x;

    @Column(name = "SOURCE_TYPE")
    String sourceType; //db, rest

    @Column(name = "ENDPOINT")
    String endpoint;

    @Column(name = "F", length = 5000, columnDefinition = "text")
    String f;

    // if rest
    // String


}
