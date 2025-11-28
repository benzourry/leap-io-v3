package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;

@Setter
@Getter
@Entity
@Table(name="CHART_FILTER")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChartFilter implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "LABEL", length = 2000)
    String label;

    @Column(name = "CODE", length = 2000)
    String code;

    @Column(name = "PRESET", length = 2000)
    String preset;

    @Column(name = "SORT_ORDER")
    Long sortOrder;

    @Column(name = "FORM_ID")
    Long formId;

    @Column(name = "ROOT")
    String root;

    @Column(name = "PREFIX")
    String prefix; //data,prev,1234,1324


    @Column(name = "TYPE") //section,list,approval
    String type;


    @JoinColumn(name = "CHART", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonBackReference("chart-filter")
    @OnDelete(action = OnDeleteAction.CASCADE)
    Chart chart;

}
