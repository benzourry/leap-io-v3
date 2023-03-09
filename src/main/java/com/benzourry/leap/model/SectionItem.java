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
@Table(name="SECTION_ITEM")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SectionItem implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "CODE", length = 2000)
    String code;

    @Column(name = "SORT_ORDER")
    Long sortOrder;

    @JoinColumn(name = "SECTION", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonBackReference("section-item")
    @OnDelete(action = OnDeleteAction.CASCADE)
    Section section;

}
