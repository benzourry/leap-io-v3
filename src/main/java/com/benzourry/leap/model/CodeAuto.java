package com.benzourry.leap.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name="CODE_AUTO")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CodeAuto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "LABEL")
    String label;

    @Column(name = "TYPE")
    String type;

    @Column(name = "SNIPPET",columnDefinition = "text")
    String snippet;

    @Column(name = "DETAIL", length = 2000)
    String detail;
}
