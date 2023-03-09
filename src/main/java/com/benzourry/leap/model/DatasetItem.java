package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.*;
import java.io.Serializable;

@Setter
@Getter
@Entity
@Table(name="DATASET_ITEM")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatasetItem implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "LABEL", length = 2000)
    String label;

    @Column(name = "CODE", length = 2000)
    String code;

    @Column(name = "SORT_ORDER")
    Long sortOrder;

    @Column(name = "FORM_ID")
    Long formId;

    @Column(name = "ROOT")
    String root;

    @Column(name = "PREFIX")
    String prefix;

    @Column(name = "TYPE") //section,list,approval
    String type;

    @JoinColumn(name = "DATASET", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonBackReference("dataset-item")
    @OnDelete(action = OnDeleteAction.CASCADE)
    Dataset dataset;

}
