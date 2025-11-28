package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Setter
@Getter
@Entity
@Table(name="LAMBDA_BIND")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LambdaBind {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "NAME")
    String name;

    @Column(name = "TYPE", length = 25)
    String type; //[dataset, entry, user]

    @Column(name = "SRC_ID")
    Long srcId; //10025 -- id of the component

    @Column(name = "PARAMS", length = 250)
    String params;

    @JoinColumn(name = "LAMBDA", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    @JsonBackReference("lambda-bind")
    @OnDelete(action = OnDeleteAction.CASCADE)
    Lambda lambda;

}
