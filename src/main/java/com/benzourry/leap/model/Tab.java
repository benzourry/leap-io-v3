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
@Table(name="TAB")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Tab implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "TITLE")
    String title;

    @Column(name = "CODE")
    String code;

    @Column(name = "SORT_ORDER")
    Long sortOrder;

    @Column(name = "PRE", length = 2000)
    String pre;

    @JoinColumn(name = "FORM", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonBackReference("form-tab")
    @OnDelete(action = OnDeleteAction.CASCADE)
    Form form;
}
