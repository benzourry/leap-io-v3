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
@Table(name="DATASET_FILTER")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatasetFilter implements Serializable {

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
    String root; //data,prev,1234,1324
    //data,prev,approval

    @Column(name = "PREFIX")
    String prefix; //data,prev,1234,1324
    //data,prev,approval

    @Column(name = "TYPE") //section,list,approval
    String type;

    @JoinColumn(name = "DATASET", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonBackReference("dataset-filter")
    @OnDelete(action = OnDeleteAction.CASCADE)
    Dataset dataset;

}
